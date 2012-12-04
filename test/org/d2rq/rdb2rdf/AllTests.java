package org.d2rq.rdb2rdf;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	DirectMappingTest.class,
	R2RMLTest.class
})

public class AllTests {}