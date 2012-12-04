package org.d2rq.functional_tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Functional test suite for D2RQ. These are functional tests (as opposed to
 * unit tests). The suite runs different find queries against the
 * ISWC database, using the example map provided with the D2RQ manual.
 * To run the test, you must have the MySQL version accessible.
 * You may have to adapt the JDBC connection information at the beginning
 * of the map file to fit your database server.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	FindTest.class,
	ModelAPITest.class
})

public class AllTests {}