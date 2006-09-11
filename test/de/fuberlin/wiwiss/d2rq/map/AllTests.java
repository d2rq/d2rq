package de.fuberlin.wiwiss.d2rq.map;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for the map package
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: AllTests.java,v 1.9 2006/09/11 23:02:50 cyganiak Exp $
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq.map");
		//$JUnit-BEGIN$
		suite.addTestSuite(TranslationTableTest.class);
		suite.addTestSuite(AliasMapTest.class);
		suite.addTestSuite(ColumnRenamerTest.class);
		suite.addTestSuite(MappingTest.class);
		//$JUnit-END$
		return suite;
	}

}
