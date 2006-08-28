/*
 * $Id: AllTests.java,v 1.9 2006/08/28 21:33:40 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for D2RQ
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class AllTests {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(AllTests.suite());
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for de.fuberlin.wiwiss.d2rq");
		//$JUnit-BEGIN$
		suite.addTestSuite(PatternTest.class);
		suite.addTestSuite(ValueRestrictionTest.class);
		suite.addTestSuite(DBConnectionTest.class);
		suite.addTestSuite(ColumnTest.class);
		suite.addTestSuite(ResultIteratorTest.class);
		suite.addTestSuite(CSVParserTest.class);
		suite.addTestSuite(TranslationTableTest.class);
		//$JUnit-END$
		suite.addTest(de.fuberlin.wiwiss.d2rq.parser.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.map.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.sql.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.functional_tests.AllTests.suite());
		return suite;
	}
}