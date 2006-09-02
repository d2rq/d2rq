package de.fuberlin.wiwiss.d2rs;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rs");
		//$JUnit-BEGIN$
		suite.addTestSuite(ContentNegotiatorTest.class);
		//$JUnit-END$
		return suite;
	}

}
