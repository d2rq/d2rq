package de.fuberlin.wiwiss.d2rq.find;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq.find");
		//$JUnit-BEGIN$
		suite.addTestSuite(URIMakerRuleTest.class);
		//$JUnit-END$
		return suite;
	}

}
