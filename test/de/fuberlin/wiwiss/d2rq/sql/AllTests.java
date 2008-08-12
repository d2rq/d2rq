package de.fuberlin.wiwiss.d2rq.sql;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq.sql");
		//$JUnit-BEGIN$
		suite.addTestSuite(SQLBuildingTest.class);
		suite.addTestSuite(ResultRowTest.class);
		suite.addTestSuite(SQLSyntaxTest.class);
		//$JUnit-END$
		return suite;
	}

}
