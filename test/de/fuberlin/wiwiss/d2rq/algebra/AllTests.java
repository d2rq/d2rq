package de.fuberlin.wiwiss.d2rq.algebra;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.algebra");
		//$JUnit-BEGIN$
		suite.addTestSuite(RelationTest.class);
		suite.addTestSuite(AliasMapTest.class);
		suite.addTestSuite(TripleRelationTest.class);
		suite.addTestSuite(AttributeTest.class);
		suite.addTestSuite(ColumnRenamerTest.class);
		suite.addTestSuite(CompatibleRelationGroupTest.class);
		suite.addTestSuite(JoinTest.class);
		//$JUnit-END$
		return suite;
	}

}
