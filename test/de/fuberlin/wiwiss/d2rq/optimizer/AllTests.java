package de.fuberlin.wiwiss.d2rq.optimizer;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(ExprTransformTest.class);
		//$JUnit-END$
		return suite;
	}

}
