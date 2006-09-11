package de.fuberlin.wiwiss.d2rq.algebra;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.algebra");
		//$JUnit-BEGIN$
		suite.addTestSuite(ExpressionTest.class);
		suite.addTestSuite(JoinTest.class);
		suite.addTestSuite(AttributeTest.class);
		//$JUnit-END$
		return suite;
	}

}
