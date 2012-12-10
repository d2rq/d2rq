package org.d2rq.mapgen;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	FilterParserTest.class,
	IRIEncoderTest.class,
	MappingGeneratorTest.class
})

public class AllTests {}