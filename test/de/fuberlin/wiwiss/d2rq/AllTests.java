package de.fuberlin.wiwiss.d2rq;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for d2rq package
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class AllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq");
		//$JUnit-BEGIN$
		suite.addTestSuite(JenaAPITest.class);
		suite.addTestSuite(DBConnectionTest.class);
		//$JUnit-END$
		return suite;
	}
}