package de.fuberlin.wiwiss.d2rq.map;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for the map package
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq.map");
		//$JUnit-BEGIN$
		suite.addTestSuite(CompileTest.class);
		suite.addTestSuite(ConstantValueClassMapTest.class);
		suite.addTestSuite(MappingTest.class);
		suite.addTestSuite(TranslationTableTest.class);
		//$JUnit-END$
		return suite;
	}

}
