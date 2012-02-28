package de.fuberlin.wiwiss.d2rq.map;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for the map package
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: AllTests.java,v 1.11 2006/09/15 12:25:26 cyganiak Exp $
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
