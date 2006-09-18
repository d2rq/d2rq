package de.fuberlin.wiwiss.d2rq.fastpath;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.fastpath");
		//$JUnit-BEGIN$
		suite.addTestSuite(ConjunctionIteratorTest.class);
		//$JUnit-END$
		return suite;
	}

}
