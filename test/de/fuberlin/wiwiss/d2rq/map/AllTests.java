package de.fuberlin.wiwiss.d2rq.map;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for the map package
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: AllTests.java,v 1.4 2006/09/03 17:22:50 cyganiak Exp $
 */
public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq.map");
		//$JUnit-BEGIN$
		suite.addTestSuite(PatternTest.class);
		suite.addTestSuite(TranslationTableTest.class);
		suite.addTestSuite(ColumnTest.class);
		suite.addTestSuite(AliasMapTest.class);
		suite.addTestSuite(NodeMakerTest.class);
		suite.addTestSuite(ValueRestrictionTest.class);
		suite.addTestSuite(ExpressionTest.class);
		//$JUnit-END$
		return suite;
	}

}
