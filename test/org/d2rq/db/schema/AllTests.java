package org.d2rq.db.schema;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	ColumnNameTest.class,
	InspectorTest.class,
	KeyTest.class,
	TableDefTest.class,
	TableNameTest.class 
})

public class AllTests {}
