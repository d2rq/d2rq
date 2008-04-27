package de.fuberlin.wiwiss.d2rq.expr;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq.expr");
		//$JUnit-BEGIN$
		suite.addTestSuite(ConjunctionTest.class);
		suite.addTestSuite(SQLExpressionTest.class);
		suite.addTestSuite(ConcatenationTest.class);
		suite.addTestSuite(ExpressionTest.class);
		//$JUnit-END$
		return suite;
	}

}
