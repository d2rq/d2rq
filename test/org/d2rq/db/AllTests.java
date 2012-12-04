package org.d2rq.db;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses({
	ResultRowTest.class,
	SelectStatementBuilderTest.class,
})

public class AllTests {}