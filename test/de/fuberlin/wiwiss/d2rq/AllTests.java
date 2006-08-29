/*
 * $Id: AllTests.java,v 1.10 2006/08/29 15:13:13 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import de.fuberlin.wiwiss.d2rq.find.ApplyTripleMakersIteratorTest;
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
		suite.addTestSuite(ApplyTripleMakersIteratorTest.class);
		suite.addTestSuite(CSVParserTest.class);
		suite.addTestSuite(TranslationTableTest.class);
		//$JUnit-END$
		suite.addTest(de.fuberlin.wiwiss.d2rq.parser.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.map.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.find.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.sql.AllTests.suite());
		suite.addTest(de.fuberlin.wiwiss.d2rq.functional_tests.AllTests.suite());
		return suite;
	}
}