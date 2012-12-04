package org.d2rq.db.types;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses({
	DataTypeTest.class,
	HSQLDBDatatypeTest.class,
//TODO: MySQL tests are just too bloody slow
//	MySQLDatatypeTest.class,
})

public class AllTests {}