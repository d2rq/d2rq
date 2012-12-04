package org.d2rq.db.expr;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ColumnListEqualityTest.class,
	ConcatenationTest.class,
	ConjunctionTest.class,
	ExpressionTest.class,
	SQLExpressionTest.class,
})

public class AllTests {}
