package org.d2rq.db.op;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	AliasOpTest.class,
	InnerJoinOpTest.class,
	ProjectOpTest.class,
	SelectOpTest.class
})

public class AllTests {}