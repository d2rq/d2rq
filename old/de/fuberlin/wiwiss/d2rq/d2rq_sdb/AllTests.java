package de.fuberlin.wiwiss.d2rq.d2rq_sdb;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Compares the results of some sparql-queries (located in file queries.txt) from
 * the sdb and from d2rq.
 * A sparql-query will executed to both and the query-results must be the same.
 *
 * @author Herwig Leimer
 * 
 */
public class AllTests {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(AllTests.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.dr2q_sdb");
		//$JUnit-BEGIN$
// TODO SDB tests are currently broken
//		suite.addTestSuite(SdbSqlEqualityTest.class);
		//$JUnit-END$
		return suite;
	}
}
