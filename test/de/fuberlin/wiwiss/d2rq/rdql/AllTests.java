package de.fuberlin.wiwiss.d2rq.rdql;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq.rdql");
		//$JUnit-BEGIN$
		suite.addTestSuite(ExpressionTest.class);
		suite.addTestSuite(SPARQLExpressionTest.class);
		//$JUnit-END$
		return suite;
	}

}
