package org.d2rq;

import static org.junit.Assert.*;

import org.junit.Test;

public class D2RQExceptionTest {

	@Test
	public void testGetErrorCodeName() {
		assertEquals("QUERY_TIMEOUT", 
				D2RQException.getErrorCodeName(D2RQException.QUERY_TIMEOUT));
	}
	
	@Test
	public void testNonExistingErrorCodeName() {
		assertNull(D2RQException.getErrorCodeName(-343343));
	}
}
