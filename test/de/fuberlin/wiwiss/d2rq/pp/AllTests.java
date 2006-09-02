package de.fuberlin.wiwiss.d2rq.pp;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq.pp");
		//$JUnit-BEGIN$
		suite.addTestSuite(PrettyPrinterTest.class);
		//$JUnit-END$
		return suite;
	}

}
