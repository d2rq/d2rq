package de.fuberlin.wiwiss.d2rq.csv;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq.csv");
		//$JUnit-BEGIN$
		suite.addTestSuite(TranslationTableParserTest.class);
		//$JUnit-END$
		return suite;
	}

}
