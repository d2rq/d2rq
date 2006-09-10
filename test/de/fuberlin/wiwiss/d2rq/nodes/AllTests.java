package de.fuberlin.wiwiss.d2rq.nodes;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.nodes");
		//$JUnit-BEGIN$
		suite.addTestSuite(NodeMakerTest.class);
		//$JUnit-END$
		return suite;
	}

}
