package org.mdpnp.dbus_java;

import java.util.List;
import java.util.Vector;

import org.freedesktop.dbus.DBusInterface;

import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.topic.Topic;

public interface ReceiveInterface extends DBusInterface {
	
	//public void receiveOnMyThreadByConditionVar();
	
	//public void receiveOnMiddlewareThread();
	public void getData(List unit, List symbol, List value, List date, List time, String systemID, String vendor, String model, String specializationCode ) throws InterruptedException ;

	//public void sendOnThisThread(List unit, List value, List date, List time) throws InterruptedException ;
	
	public int echoMessage(String str);

	public int echoAndAdd(String str, int a, int b);
	
	public List getMeasurementList();
	
	public List getUnitList();
	
	public List getTimeList();
	
	public List getDateList();
	
	public List getSymbolList();
	
	public String getVendorMetricId();
	
    public String getSpecializationCode();
    
    public String getSystemID();
    
    public String getModel();
    
	
}
