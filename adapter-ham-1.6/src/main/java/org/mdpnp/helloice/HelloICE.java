package org.mdpnp.helloice;

import com.rti.dds.domain.DomainParticipantQos;
import com.rti.dds.publication.Publisher;
import com.rti.dds.publication.PublisherQos;
import com.rti.dds.subscription.*;

import ice.ConnectionState;
//import ice.DeviceDataReader;
import ice.HAM_DeviceDataReader;
import ice.NumericDataReader;
import ice.SampleArrayDataReader;
import ice.Time_t;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.mdpnp.dbus_java.ReceiveInterface;
import org.mdpnp.rtiapi.data.QosProfiles;
import org.mdpnp.rtiapi.qos.IceQos;

import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.domain.DomainParticipantFactory;
import com.rti.dds.infrastructure.ConditionSeq;
import com.rti.dds.infrastructure.Duration_t;
import com.rti.dds.infrastructure.InstanceHandle_t;
import com.rti.dds.infrastructure.RETCODE_NO_DATA;
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.infrastructure.StringSeq;
import com.rti.dds.infrastructure.WaitSet;
import com.rti.dds.topic.Topic;

public class HelloICE implements ReceiveInterface{
	
	private static DomainParticipant participant;
	private static SubscriberQos subscriberQos;
	private static PublisherQos publisherQos;
	private static Topic sampleArrayTopic;
	private static Topic numericTopic;
	private static Topic deviceTopic;
	private static ReceiveInterface recvInterfaceMeasure;
	private static ReceiveInterface recvInterfaceAttributes;
	private static boolean flag;
	
	
    public static void receiveOnMiddlewareThread() {
    	
    	// A listener to receive callback events from the SampleArrayDataReader
        final DataReaderListener saListener = new DataReaderListener() {

            @Override
            public void on_data_available(DataReader reader) {
                // Will contain the data samples we read from the reader
                ice.SampleArraySeq sa_data_seq = new ice.SampleArraySeq();

                // Will contain the SampleInfo information about those data
                SampleInfoSeq info_seq = new SampleInfoSeq();

                SampleArrayDataReader saReader = (SampleArrayDataReader) reader;

                // Read samples from the reader
                try {
                    saReader.read(sa_data_seq,info_seq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED, SampleStateKind.NOT_READ_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ALIVE_INSTANCE_STATE);

                    // Iterator over the samples
                    for(int i = 0; i < info_seq.size(); i++) {
                        SampleInfo si = (SampleInfo) info_seq.get(i);
                        ice.SampleArray data = (ice.SampleArray) sa_data_seq.get(i);
                        // If the updated sample status contains fresh data that we can evaluate
                        if(si.valid_data) {
                            System.out.println(data);
                        }

                    }
                } catch (RETCODE_NO_DATA noData) {
                    // No Data was available to the read call
                } finally {
                    // the objects provided by "read" are owned by the reader and we must return them
                    // so the reader can control their lifecycle
                    saReader.return_loan(sa_data_seq, info_seq);
                }
            }

            @Override
            public void on_liveliness_changed(DataReader arg0, LivelinessChangedStatus arg1) {
                System.out.println("liveliness_changed "+arg1);
            }

            @Override
            public void on_requested_deadline_missed(DataReader arg0, RequestedDeadlineMissedStatus arg1) {
                System.out.println("requested_deadline_missed "+arg1);
            }

            @Override
            public void on_requested_incompatible_qos(DataReader arg0, RequestedIncompatibleQosStatus arg1) {
                System.out.println("requested_incompatible_qos "+arg1);
            }

            @Override
            public void on_sample_lost(DataReader arg0, SampleLostStatus arg1) {
                System.out.println("sample_lost "+arg1);
            }

            @Override
            public void on_sample_rejected(DataReader arg0, SampleRejectedStatus arg1) {
                System.out.println("sample_rejected "+arg1);
            }

            @Override
            public void on_subscription_matched(DataReader arg0, SubscriptionMatchedStatus arg1) {
                System.out.println("subscription_matched "+arg1);
            }
            
        };
        
        // A listener to receive callback events from the NumericDataReader
        final DataReaderListener nListener = new DataReaderListener() {
            @Override
            public void on_data_available(DataReader reader) {
                ice.NumericSeq n_data_seq = new ice.NumericSeq();

                // Will contain the SampleInfo information about those data
                SampleInfoSeq info_seq = new SampleInfoSeq();
                
                NumericDataReader nReader = (NumericDataReader) reader;
                
                try {
                    // Read samples from the reader
                    nReader.read(n_data_seq,info_seq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED, SampleStateKind.NOT_READ_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ALIVE_INSTANCE_STATE);

                    // Iterator over the samples
                    for(int i = 0; i < info_seq.size(); i++) {
                        SampleInfo si = (SampleInfo) info_seq.get(i);
                        ice.Numeric data = (ice.Numeric) n_data_seq.get(i);
                        // If the updated sample status contains fresh data that we can evaluate
                        if(si.valid_data) {
                            if(data.metric_id.equals(rosetta.MDC_PULS_OXIM_SAT_O2.VALUE)) {
                                // This is an O2 saturation from pulse oximetry
//                                System.out.println("SpO2="+data.value);
                            } else if(data.metric_id.equals(rosetta.MDC_PULS_OXIM_PULS_RATE.VALUE)) {
                                // This is a pulse rate from pulse oximetry
//                              System.out.println("Pulse Rate="+data.value);
                            }
                            System.out.println(data);
                        }

                    }
                } catch (RETCODE_NO_DATA noData) {
                    // No Data was available to the read call
                } finally {
                    // the objects provided by "read" are owned by the reader and we must return them
                    // so the reader can control their lifecycle
                    nReader.return_loan(n_data_seq, info_seq);
                }

            }
            
            @Override
            public void on_liveliness_changed(DataReader arg0, LivelinessChangedStatus arg1) {
                System.out.println("liveliness_changed "+arg1);
            }

            @Override
            public void on_requested_deadline_missed(DataReader arg0, RequestedDeadlineMissedStatus arg1) {
                System.out.println("requested_deadline_missed "+arg1);
            }

            @Override
            public void on_requested_incompatible_qos(DataReader arg0, RequestedIncompatibleQosStatus arg1) {
                System.out.println("requested_incompatible_qos "+arg1);
            }

            @Override
            public void on_sample_lost(DataReader arg0, SampleLostStatus arg1) {
                System.out.println("sample_lost "+arg1);
            }

            @Override
            public void on_sample_rejected(DataReader arg0, SampleRejectedStatus arg1) {
                System.out.println("sample_rejected "+arg1);
            }

            @Override
            public void on_subscription_matched(DataReader arg0, SubscriptionMatchedStatus arg1) {
                System.out.println("subscription_matched "+arg1);
            }
        };
        
        

        
        
        // A listener to receive callback events from the NumericDataReader
        final DataReaderListener dataListener = new DataReaderListener() {
            @Override
            public void on_data_available(DataReader reader) {
            	
                //ice.NumericSeq n_data_seq = new ice.NumericSeq();
            	ice.HAM_DeviceSeq n_data_seq = new ice.HAM_DeviceSeq();

                // Will contain the SampleInfo information about those data
                SampleInfoSeq info_seq = new SampleInfoSeq();
                
                //NumericDataReader nReader = (NumericDataReader) reader;
                HAM_DeviceDataReader dataReader = (HAM_DeviceDataReader) reader;
                
                try {
                    // Read samples from the reader
                    //nReader.read(n_data_seq,info_seq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED, SampleStateKind.NOT_READ_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ALIVE_INSTANCE_STATE);
                	dataReader.read(n_data_seq,info_seq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED, SampleStateKind.NOT_READ_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ALIVE_INSTANCE_STATE);
                    // Iterator over the samples
                    for(int i = 0; i < info_seq.size(); i++) {
                        SampleInfo si = (SampleInfo) info_seq.get(i);
                        ice.HAM_Device data = (ice.HAM_Device) n_data_seq.get(i);
                        // If the updated sample status contains fresh data that we can evaluate
                        if(si.valid_data) {
                            //if(data.metric_id.equals(rosetta.MDC_PULS_OXIM_SAT_O2.VALUE)) {
                                // This is an O2 saturation from pulse oximetry
//                                System.out.println("SpO2="+data.value);
                            //} else if(data.metric_id.equals(rosetta.MDC_PULS_OXIM_PULS_RATE.VALUE)) {
                                // This is a pulse rate from pulse oximetry
//                              System.out.println("Pulse Rate="+data.value);
                           // }
                            System.out.println(data);
                        }

                    }
                } catch (RETCODE_NO_DATA noData) {
                    // No Data was available to the read call
                } finally {
                    // the objects provided by "read" are owned by the reader and we must return them
                    // so the reader can control their lifecycle
                	dataReader.return_loan(n_data_seq, info_seq);
                }

            }
            
            @Override
            public void on_liveliness_changed(DataReader arg0, LivelinessChangedStatus arg1) {
                System.out.println("liveliness_changed "+arg1);
            }

            @Override
            public void on_requested_deadline_missed(DataReader arg0, RequestedDeadlineMissedStatus arg1) {
                System.out.println("requested_deadline_missed "+arg1);
            }

            @Override
            public void on_requested_incompatible_qos(DataReader arg0, RequestedIncompatibleQosStatus arg1) {
                System.out.println("requested_incompatible_qos "+arg1);
            }

            @Override
            public void on_sample_lost(DataReader arg0, SampleLostStatus arg1) {
                System.out.println("sample_lost "+arg1);
            }

            @Override
            public void on_sample_rejected(DataReader arg0, SampleRejectedStatus arg1) {
                System.out.println("sample_rejected "+arg1);
            }

            @Override
            public void on_subscription_matched(DataReader arg0, SubscriptionMatchedStatus arg1) {
                System.out.println("subscription_matched "+arg1);
            }
        };
        
        // Create a reader endpoint for samplearray data
        @SuppressWarnings("unused")
        ice.HAM_DeviceDataReader dataReader = (HAM_DeviceDataReader) participant.create_datareader_with_profile(deviceTopic, QosProfiles.ice_library, QosProfiles.default_profile, dataListener, StatusKind.STATUS_MASK_ALL);
       // @SuppressWarnings("unused")
       // ice.NumericDataReader nReader = (ice.NumericDataReader) participant.create_datareader_with_profile(numericTopic, QosProfiles.ice_library, QosProfiles.numeric_data, nListener, StatusKind.STATUS_MASK_ALL);
     // Create a reader endpoint for samplearray data
        @SuppressWarnings("unused")
        ice.SampleArrayDataReader saReader = (ice.SampleArrayDataReader) participant.create_datareader_with_profile(sampleArrayTopic, QosProfiles.ice_library, QosProfiles.waveform_data, saListener, StatusKind.STATUS_MASK_ALL);

        @SuppressWarnings("unused")
        ice.NumericDataReader nReader = (ice.NumericDataReader) participant.create_datareader_with_profile(numericTopic, QosProfiles.ice_library, QosProfiles.numeric_data, nListener, StatusKind.STATUS_MASK_ALL);
    }
    
    public static void receiveOnMyThreadByConditionVar() {
        // Create a reader endpoint for samplearray data
        //ice.SampleArrayDataReader saReader = (ice.SampleArrayDataReader) participant.create_datareader_with_profile(sampleArrayTopic, QosProfiles.ice_library, QosProfiles.waveform_data, null, StatusKind.STATUS_MASK_NONE);

        ice.HAM_DeviceDataReader dataReader = (ice.HAM_DeviceDataReader) participant.create_datareader_with_profile(numericTopic, QosProfiles.ice_library, QosProfiles.default_profile, null, StatusKind.STATUS_MASK_NONE);

        // A waitset allows us to wait for various status changes in various entities
        WaitSet ws = new WaitSet();

        // Here we configure the status condition to trigger when new data becomes available to the reader
        //saReader.get_statuscondition().set_enabled_statuses(StatusKind.DATA_AVAILABLE_STATUS);

        dataReader.get_statuscondition().set_enabled_statuses(StatusKind.DATA_AVAILABLE_STATUS);

        // And register that status condition with the waitset so we can monitor its triggering
        //ws.attach_condition(saReader.get_statuscondition());

        ws.attach_condition(dataReader.get_statuscondition());

        // will contain triggered conditions
        ConditionSeq cond_seq = new ConditionSeq();

        // we'll wait as long as necessary for data to become available
        Duration_t timeout = new Duration_t(Duration_t.DURATION_INFINITE_SEC, Duration_t.DURATION_INFINITE_NSEC);

        // Will contain the data samples we read from the reader
        //ice.SampleArraySeq sa_data_seq = new ice.SampleArraySeq();

        ice.HAM_DeviceSeq n_data_seq = new ice.HAM_DeviceSeq();

        // Will contain the SampleInfo information about those data
        SampleInfoSeq info_seq = new SampleInfoSeq();

        // This loop will repeat until the process is terminated
        for(;;) {
            // Wait for a condition to be triggered
            ws.wait(cond_seq, timeout);
            // Check that our status condition was indeed triggered
//            if(cond_seq.contains(saReader.get_statuscondition())) {
//                // read the actual status changes
//                int status_changes = saReader.get_status_changes();
//
//                // Ensure that DATA_AVAILABLE is one of the statuses that changed in the DataReader.
//                // Since this is the only enabled status (see above) this is here mainly for completeness
//                if(0 != (status_changes & StatusKind.DATA_AVAILABLE_STATUS)) {
//                    try {
//                        // Read samples from the reader
//                        saReader.read(sa_data_seq,info_seq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED, SampleStateKind.NOT_READ_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ALIVE_INSTANCE_STATE);
//
//                        // Iterator over the samples
//                        for(int i = 0; i < info_seq.size(); i++) {
//                            SampleInfo si = (SampleInfo) info_seq.get(i);
//                            ice.SampleArray data = (ice.SampleArray) sa_data_seq.get(i);
//                            // If the updated sample status contains fresh data that we can evaluate
//                            if(si.valid_data) {
//                                System.out.println(data);
//                            }
//
//                        }
//                    } catch (RETCODE_NO_DATA noData) {
//                        // No Data was available to the read call
//                    } finally {
//                        // the objects provided by "read" are owned by the reader and we must return them
//                        // so the reader can control their lifecycle
//                        saReader.return_loan(sa_data_seq, info_seq);
//                    }
//                }
//            }
            if(cond_seq.contains(dataReader.get_statuscondition())) {
                // read the actual status changes
                int status_changes = dataReader.get_status_changes();

                // Ensure that DATA_AVAILABLE is one of the statuses that changed in the DataReader.
                // Since this is the only enabled status (see above) this is here mainly for completeness
                if(0 != (status_changes & StatusKind.DATA_AVAILABLE_STATUS)) {
                    try {
                        // Read samples from the reader
                    	dataReader.read(n_data_seq,info_seq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED, SampleStateKind.NOT_READ_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ALIVE_INSTANCE_STATE);

                        // Iterator over the samples
                        for(int i = 0; i < info_seq.size(); i++) {
                            SampleInfo si = (SampleInfo) info_seq.get(i);
                            ice.HAM_Device data = (ice.HAM_Device) n_data_seq.get(i);
                            // If the updated sample status contains fresh data that we can evaluate
                            if(si.valid_data) {
//                                if(data.metric_id.equals(rosetta.MDC_PULS_OXIM_SAT_O2.VALUE)) {
//                                    // This is an O2 saturation from pulse oximetry
////                                    System.out.println("SpO2="+data.value);
//                                } else if(data.metric_id.equals(rosetta.MDC_PULS_OXIM_PULS_RATE.VALUE)) {
//                                    // This is a pulse rate from pulse oximetry
////                                  System.out.println("Pulse Rate="+data.value);
//                                }
                                System.out.println(data);
                            }

                        }
                    } catch (RETCODE_NO_DATA noData) {
                        // No Data was available to the read call
                    } finally {
                        // the objects provided by "read" are owned by the reader and we must return them
                        // so the reader can control their lifecycle
                        dataReader.return_loan(n_data_seq, info_seq);
                    }
                }
            }
        }
    }
    
    
    public void getData(List unit, List symbol, List value, List date, List time, String systemID, String vendor, String model, String specializationCode) throws InterruptedException {
        // Creates a data writer; uses the default implicit publisher for this participant
        //ice.SampleArrayDataWriter saWriter = (ice.SampleArrayDataWriter) participant.create_datawriter_with_profile(sampleArrayTopic, QosProfiles.ice_library, QosProfiles.waveform_data, null, StatusKind.STATUS_MASK_NONE);

        //ice.NumericDataWriter nWriter = (ice.NumericDataWriter) participant.create_datawriter_with_profile(numericTopic, QosProfiles.ice_library, QosProfiles.numeric_data, null, StatusKind.STATUS_MASK_NONE);
        
        ice.HAM_DeviceDataWriter dataWriter = (ice.HAM_DeviceDataWriter) participant.create_datawriter_with_profile(deviceTopic, QosProfiles.ice_library, QosProfiles.default_profile, null, StatusKind.STATUS_MASK_NONE);
        // Populate the values of a sample to be written later
        ice.HAM_Device deviceData = new ice.HAM_Device();
        deviceData.unique_system_id = systemID;
        deviceData.vendor_metric_id = vendor;
        deviceData.device_model = model;
        deviceData.specialization_code = specializationCode;
        deviceData.unit_symbol.userData.addAll(symbol);
          deviceData.unit_id.userData.addAll(unit);
          //deviceData.unit_symbol.userData.addAll(arg0);
          deviceData.values.userData.addAll(value);
          deviceData.measurement_date.userData.addAll(date);
          deviceData.measurement_time.userData.addAll(time);
     
//          deviceData.unit_id.userData.copy_from(unit);
//          deviceData.value.userData.copy_from(measurement);
//          deviceData.measurement_date.userData.copy_from(date);
//          deviceData.measurement_time.userData.copy_from(time);
        
        
        // Preregistering instances speeds up subsequent writes 
       // InstanceHandle_t saHandle = saWriter.register_instance(sampleArray);
       // InstanceHandle_t nHandle = nWriter.register_instance(numeric);
        InstanceHandle_t deviceHandle = dataWriter.register_instance(deviceData);
        
        
        	Thread.sleep(1000L);
        	dataWriter.write(deviceData, deviceHandle);
		
        
        // Write 
//        for(int i = 0; i < 10; i++) {
//            long time = System.currentTimeMillis();
//            
//            numeric.device_time.sec = (int) (time / 1000L);
//            numeric.device_time.nanosec = (int) (time % 1000L * 1000000L);
//            numeric.presentation_time.copy_from(numeric.device_time);
//            nWriter.write(numeric, nHandle);
//            
//            sampleArray.values.clear();
//            
//            sampleArray.device_time.copy_from(numeric.device_time);
//            sampleArray.presentation_time.copy_from(numeric.presentation_time);
//            
//            // Square wave
//            if(0 == (i%2)) {
//                for(int j = 0; j < 10; j++) {
//                    sampleArray.values.userData.add(0.0f);
//                }
//            } else {
//                for(int j = 0; j < 10; j++) {
//                    sampleArray.values.userData.add(1.0f);
//                }
//                
//            }
//            saWriter.write(sampleArray, saHandle);
//            
//            System.out.println("Wrote " + numeric);
//            System.out.println(sampleArray);
//            
//            Thread.sleep(1000L);
//        }
        System.out.println("Wrote " + deviceData);
        dataWriter.unregister_instance(deviceData, deviceHandle);
        //saWriter.unregister_instance(sampleArray, saHandle);
        //nWriter.unregister_instance(numeric, nHandle);
        
    }
    public static String listToString(List list){
    	
    	String listString = "";

    	for (int i = 0; i < list.size(); i++) {
    		listString += list.get(i) + "\t";
		}
    
    	
    	return listString;
    }
    
//    public static void sendTeste(){
//    	
//    	 ice.NumericDataWriter nWriter = (ice.NumericDataWriter) participant.create_datawriter_with_profile(numericTopic, QosProfiles.ice_library, QosProfiles.numeric_data, null, StatusKind.STATUS_MASK_NONE);
//    	 ice.Numeric numeric = new ice.Numeric();
//    	 
//    	 numeric.unique_device_identifier = listToString(recvInterface.getMeasurement());
//         numeric.metric_id = rosetta.MDC_ECG_HEART_RATE.VALUE;
//         numeric.unit_id = rosetta.MDC_DIM_BEAT_PER_MIN.VALUE;
//         numeric.vendor_metric_id = "";
//         numeric.instance_id = 0;
//         numeric.device_time = new Time_t();
//         numeric.presentation_time = new Time_t();
//         
//         InstanceHandle_t nHandle = nWriter.register_instance(numeric);
//         
//         nWriter.write(numeric, nHandle);
//         
//         System.out.println("Wrote " + numeric);
//         
//         nWriter.unregister_instance(numeric, nHandle);
//    }
    
    enum ReceiveStrategy {
        OnMyThreadByConditionVar,
        OnMiddlewareThread,
        PublishExample,
    }
    //public HelloICE() {
	
    public static void dbusConnection(){
    	
    	try {
			DBusConnection conn = DBusConnection.getConnection(DBusConnection.SESSION);
			//conn.requestBusName("service.measurementInterface");
			//conn.exportObject("/measurement/nutes", new HelloICE());
			recvInterfaceMeasure = (ReceiveInterface)conn.getRemoteObject("org.mdpnp.dbus_measurements", "/measurement/nutes", ReceiveInterface.class);//ReceiveInterface
			recvInterfaceAttributes = (ReceiveInterface)conn.getRemoteObject("org.mdpnp.dbus_attributes", "/attributes/nutes", ReceiveInterface.class);
			// getRemoteObject(String busname, String objectpath) Return a reference to a remote object.
			// To call the methods, you can do something like this: recvInterface.method(anything);
		} catch (DBusException e) {
			e.printStackTrace();
		}
    }
    
    public static void main(String[] args) throws InterruptedException, IOException {
		
	
        int domainId = 0;

        // domainId is the one command line argument
//        if(args.length > 0) {
//            System.out.println("Using Domain " + args[0]);
//            domainId = Integer.parseInt(args[0]);
//        }

        // Here we use 'default' Quality of Service settings supplied by x73-idl-rti-dds
        IceQos.loadAndSetIceQos();

        // A domain participant is the main access point into the DDS domain.  Endpoints are created within the domain participant
        participant = DomainParticipantFactory.get_instance().create_participant(domainId, DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);



        // OpenICE divides the global DDS data-space into individual patient contexts using the DDS partitioning mechanism.
        // Partitions are a list of strings (and wildcards) that pubs and subs are required to match (at least one once) in their respective lists to pair.
        // Partitions are assigned at the publisher/subscriber level in the quality of service (QoS) settings.

        // Declare and instantiate a new SubscriberQos policy
        subscriberQos = new SubscriberQos();

        // Populate the SubscriberQos policy
        participant.get_default_subscriber_qos(subscriberQos);

        subscriberQos.partition.name.clear();

        // Add an entry to the partition list. 'name' is actually a DDS StringSeq that you can simply .add() to.
        // To receive OpenICE data for a specific patient, add the patient's MRN to the QoS partition policy
        // e.g. "MRN=12345". MRNs are alphanumeric prefixed with "MRN="
        // subscriberQos.partition.name.add("MRN=10101");   // This is fake patient Randall Jones in the MDPnP lab.

        // Partitioning supports some wildcards like "*" to access every partition (including the default or null partition)
        subscriberQos.partition.name.add("*");

        // A note about partition names:
        // The Supervisor uses an MRN for the Partition name and a First / Last name as a display name. For example, MRN=10101
        // will show up as Randall Jones in the Supervisor. To match display names and MRNs, the Supervisor will either
        // use defaults or look them up in an HL7 FHIR database. If you provide an HL7 FHIR server address, the Supervisor
        // will treat that server as a "Master Patient Index". The Supervisor will attempt to download the Patient Resource
        // (https://www.hl7.org/fhir/patient.html) from that address and use resource.identifier.value as the Partition name and
        // resource.name.family / resource.name.given as the display name. If no HL7 FHIR address is provided, the Supervisor will
        // provide a small SQL server of default names and MRNs.

        // Set the subscriber qos with our newly created SubscriberQos
        participant.set_default_subscriber_qos(subscriberQos);

        // There are a couple ways to do this (as far as I can tell). I don't know which one is correct or standard or proper or whatever. For example:
        // Subscriber subscriber = participant.get_implicit_subscriber();
        // subscriber.get_qos(subscriberQos);
        // subscriberQos.partition.name.add("MRN=10101");
        // subscriber.set_qos(subscriberQos);


        // Same concept but this time for the publisher
        publisherQos = new PublisherQos();

        participant.get_default_publisher_qos(publisherQos);

        publisherQos.partition.name.clear();

        // Change this line to the patient MRN for which you want to emit data.
        //publisherQos.partition.name.add("MRN=10101");

        participant.set_default_publisher_qos(publisherQos);



        // Inform the participant about the sample array data type we would like to use in our endpoints
        ice.SampleArrayTypeSupport.register_type(participant, ice.SampleArrayTypeSupport.get_type_name());
        
        // Inform the participant about the numeric data type we would like to use in our endpoints
        ice.NumericTypeSupport.register_type(participant, ice.NumericTypeSupport.get_type_name());
        ice.HAM_DeviceTypeSupport.register_type(participant, ice.HAM_DeviceTypeSupport.get_type_name());
        // A topic the mechanism by which reader and writer endpoints are matched.
        sampleArrayTopic = participant.create_topic(ice.SampleArrayTopic.VALUE, ice.SampleArrayTypeSupport.get_type_name(), DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);
        
        // A second topic if for Numeric data
        numericTopic = participant.create_topic(ice.NumericTopic.VALUE, ice.NumericTypeSupport.get_type_name(), DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);
        
        deviceTopic = participant.create_topic(ice.HAM_DeviceTopic.VALUE, ice.HAM_DeviceTypeSupport.get_type_name(), DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);
        
        
        ice.DeviceIdentityTypeSupport.register_type(participant, ice.DeviceIdentityTypeSupport.get_type_name());
        
        Topic identityTopic = participant.create_topic(ice.DeviceIdentityTopic.VALUE, ice.DeviceIdentityTypeSupport.get_type_name(), DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);
        
		 ice.DeviceConnectivityTypeSupport.register_type(participant, ice.DeviceConnectivityTypeSupport.get_type_name());
		        
        Topic connectivityTopic = participant.create_topic(ice.DeviceConnectivityTopic.VALUE, ice.DeviceConnectivityTypeSupport.get_type_name(), DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);
        ReceiveStrategy strategy = ReceiveStrategy.OnMiddlewareThread;
        
//        if(args.length > 1) {
//            strategy = ReceiveStrategy.valueOf(args[1]);
//        }
       
        System.out.println("strategy: " + strategy);
        dbusConnection();
        HelloICE i = new HelloICE();
        try {
        	  Thread.sleep(1000);
        	} catch (InterruptedException ie) {
        	    //Handle exception
        	}
        //i.sendOnThisThread(i.recvInterfaceMeasure.getUnitList(), i.recvInterfaceMeasure.getMeasurementList(), i.recvInterfaceMeasure.getDateList(), i.recvInterfaceMeasure.getTimeList());
        sendIdentity(participant, identityTopic,i.recvInterfaceAttributes.getSystemID());
        sendConnectivity(participant, connectivityTopic,i.recvInterfaceAttributes.getSystemID());
        i.getData(i.recvInterfaceMeasure.getUnitList(),i.recvInterfaceMeasure.getSymbolList(), i.recvInterfaceMeasure.getMeasurementList(), i.recvInterfaceMeasure.getDateList(), i.recvInterfaceMeasure.getTimeList(), i.recvInterfaceAttributes.getSystemID(), i.recvInterfaceAttributes.getVendorMetricId(),i.recvInterfaceAttributes.getModel(),i.recvInterfaceAttributes.getSpecializationCode());

        
        
        //sendTeste();
        
       // System.out.println(recvInterface.getMeasurement());
        
		
		//Note that while the main thread will exit here, the D-Bus will keep the program 
		//waiting.  You do not have to make an infinite loop.

        switch(strategy) {
        // receiveOnMyThreadByConditionVar demonstrates receiving data on *this* thread via notification by condition variable.
        // Alternatively in unique cases readers can be polled at intervals with no signalling.
        case OnMyThreadByConditionVar:
        	receiveOnMyThreadByConditionVar();
            break;
        // receiveOnMiddlewareThread demonstrates receiving data via a callback on a middleware thread.
        case OnMiddlewareThread:
            receiveOnMiddlewareThread();
            break;
        case PublishExample:
           // sendOnThisThread();
            break;
        }
        
    }


	@Override
	public boolean isRemote() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public int echoMessage(String str) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int echoAndAdd(String str, int a, int b) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public List getMeasurementList() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List getUnitList() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List getTimeList() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List getDateList() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List getSymbolList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVendorMetricId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSpecializationCode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSystemID() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getModel(){
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void sendIdentity(DomainParticipant participant, Topic deviceIdentityTopic, String uid) throws InterruptedException, IOException {
    	
    	ice.DeviceIdentityDataWriter iWriter = (ice.DeviceIdentityDataWriter) participant.create_datawriter_with_profile(deviceIdentityTopic, QosProfiles.ice_library, QosProfiles.device_identity, null, StatusKind.STATUS_MASK_NONE);
    	
    	 ice.DeviceIdentity identity = new ice.DeviceIdentity();
         
         identity.model = "HAM";
         identity.operating_system = "Windows 8.1";
         identity.unique_device_identifier = uid;
         identity.manufacturer = "NUTES";
         identity.icon.content_type = "jpg/png";//"/hello-openice-master/src/main/resources/ham.png"
         
         InputStream is = convertBytes();
         if (null != is) {
             try {
 		        byte[] xfer = new byte[1024];
 		        int len = is.read(xfer);
 		        identity.icon.image.userData.clear();
 		        
 		        while (len >= 0) {
 		        	identity.icon.image.userData.addAllByte(xfer, 0, len);
                     len = is.read(xfer);
                 }
 		        is.close();
             } catch (Exception e) {
                
             }
         }
         
         InstanceHandle_t iHandle = iWriter.register_instance(identity);
         
         for (int i = 0; i < 3; i++) {
         	iWriter.write(identity, iHandle);
         	System.out.println("Wrote " + identity);
         	Thread.sleep(1000L);
 		}
         iWriter.unregister_instance(identity, iHandle);
         
    }
    
    public static void sendConnectivity(DomainParticipant participant, Topic deviceConnectivityTopic, String uid) throws InterruptedException{
    	
    	ice.DeviceConnectivityDataWriter cWriter = (ice.DeviceConnectivityDataWriter) participant.create_datawriter_with_profile(deviceConnectivityTopic, QosProfiles.ice_library, QosProfiles.default_profile, null, StatusKind.STATUS_MASK_NONE);
   
    	ice.DeviceConnectivity connectivity = new ice.DeviceConnectivity();
   	 	
    	connectivity.unique_device_identifier = uid;
    	connectivity.state = ConnectionState.Connected;
    	
   	 
   	 	InstanceHandle_t cHandle = cWriter.register_instance(connectivity);
    	//send
   	 	for (int i = 0; i < 3; i++) {
   	 		cWriter.write(connectivity, cHandle);
   	 		System.out.println("Wrote " + connectivity);
   	 		Thread.sleep(1000L);
   	 	}
   	 	cWriter.unregister_instance(connectivity, cHandle);
    }
    
    
    
    public static ByteArrayInputStream convertBytes() throws IOException {
    	File f = new File("/home/alisson/workspace/adapter-ham-1.6/src/main/java/org/mdpnp/helloice/NutesLogo.png");
        InputStream in = new FileInputStream(f);

        byte[] buff = new byte[8000];

        int bytesRead = 0;

        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        while((bytesRead = in.read(buff)) != -1) {
           bao.write(buff, 0, bytesRead);
        }

        byte[] data = bao.toByteArray();

        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        return bin;
	}

}
