/*
 * $Id: AllTests.java,v 1.2 2004/08/12 09:11:10 bizer Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import junit.framework.Test;
import junit.framework.TestSuite;
import de.fuberlin.wiwiss.d2rq.functional_tests.FindTest;
import de.fuberlin.wiwiss.d2rq.functional_tests.ModelAPITest;
import de.fuberlin.wiwiss.d2rq.functional_tests.RDQLTest;

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
		suite.addTestSuite(ParserTest.class);
		suite.addTestSuite(CSVParserTest.class);
		suite.addTestSuite(ValueRestrictionTest.class);
		suite.addTestSuite(TranslationTableTest.class);
		suite.addTestSuite(ColumnTest.class);
		suite.addTestSuite(SQLStatementMakerTest.class);
		suite.addTestSuite(FindTest.class);
		suite.addTestSuite(ModelAPITest.class);
		suite.addTestSuite(RDQLTest.class);
		//$JUnit-END$
		return suite;
	}
}