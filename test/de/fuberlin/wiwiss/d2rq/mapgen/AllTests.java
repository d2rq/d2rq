package de.fuberlin.wiwiss.d2rq.mapgen;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for the mapgen package
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.fuberlin.wiwiss.d2rq.mapgen");
		//$JUnit-BEGIN$
		suite.addTestSuite(FilterParserTest.class);
		suite.addTestSuite(IRIEncoderTest.class);
		//$JUnit-END$
		return suite;
	}

}
