package junit.reports.core;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rts.ui.Threshold;

public class TestThreshold {

	@Test
	public void testEqualityRules() {
		Threshold t1 = new Threshold("m1", "l1");
		Threshold t2 = new Threshold("m1", "l1");
		
		MockObjects.LOGD("Test equality t1=" + t1 + " t2=" + t2);
		
		// objs compared with equals must have equal hasCodes
		assertTrue(t1.equals(t2));
		assertTrue("Equal objs must have equal hashCodes", t1.hashCode() == t2.hashCode());
	}

}
