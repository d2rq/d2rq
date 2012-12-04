package org.d2rq.r2rml;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ConstantIRITest.class,
	LanguageTagTest.class,
	StringTemplateTest.class,
	TableOrViewNameTest.class,
	ValidatorTest.class,
	ProcessorTest.class
})

public class AllTests {}