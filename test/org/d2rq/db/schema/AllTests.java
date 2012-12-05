package org.d2rq.db.schema;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	ColumnDefTest.class,
	ColumnNameTest.class,
	InspectorTest.class,
	KeyTest.class,
	TableDefTest.class,
	TableNameTest.class 
})

public class AllTests {}
