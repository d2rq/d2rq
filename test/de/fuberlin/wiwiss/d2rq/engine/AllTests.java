package de.fuberlin.wiwiss.d2rq.engine;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.engine");
		//$JUnit-BEGIN$
		suite.addTestSuite(GraphPatternTranslatorTest.class);
		//$JUnit-END$
		return suite;
	}

}
