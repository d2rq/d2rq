package de.fuberlin.wiwiss.d2rq.parser;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.parser");
		//$JUnit-BEGIN$
		suite.addTestSuite(ParserTest.class);
		//$JUnit-END$
		return suite;
	}

}
