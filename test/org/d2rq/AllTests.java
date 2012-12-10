package org.d2rq;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	D2RQExceptionTest.class, 
	HSQLDatabaseTest.class
})

public class AllTests {}
