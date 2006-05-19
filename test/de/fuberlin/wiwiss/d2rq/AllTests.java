/*
 * $Id: AllTests.java,v 1.7 2006/05/19 19:13:02 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import de.fuberlin.wiwiss.d2rq.rdql.TablePrefixerTest;
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
        suite.addTestSuite(ColumnTest.class);
        suite.addTestSuite(ValueRestrictionTest.class);
        suite.addTestSuite(PatternTest.class);
        suite.addTestSuite(TranslationTableTest.class);
        suite.addTestSuite(SQLStatementMakerTest.class);
        suite.addTestSuite(CSVParserTest.class);
        suite.addTestSuite(ResultIteratorTest.class);
        suite.addTestSuite(TablePrefixerTest.class);
        //$JUnit-END$
        suite.addTest(de.fuberlin.wiwiss.d2rq.parser.AllTests.suite());
        suite.addTest(de.fuberlin.wiwiss.d2rq.functional_tests.AllTests.suite());
        return suite;
    }
}