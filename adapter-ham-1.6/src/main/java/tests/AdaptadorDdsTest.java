package tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mdpnp.helloice.HelloICE;

import org.junit.Assert;

public class AdaptadorDdsTest {
	HelloICE hello;
	@Before
	public void setUp() throws Exception {
		hello = new HelloICE();
		hello.main(null);
	}

	@Test
	public void testGetMeasurementList() {
		Assert.assertEquals( new ArrayList<String>().addAll(Arrays.asList("96.500000","63.500000")), hello.getMeasurementList());
		
	}

//	@Test
//	public void testGetUnitList() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetTimeList() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetDateList() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSymbolList() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetVendorMetricId() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSpecializationCode() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetSystemID() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetModel() {
//		fail("Not yet implemented");
//	}

}
