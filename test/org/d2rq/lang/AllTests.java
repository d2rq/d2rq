package org.d2rq.lang;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	AliasDeclarationTest.class,
	D2RQCompilerTest.class,
	D2RQReaderTest.class,
	ConstantValueClassMapTest.class,
	D2RQValidatorTest.class,
	JoinSetParserTest.class,
	MappingTest.class,
	MicrosyntaxTest.class,
	PatternTest.class,
	ProcessorTest.class,
	TranslationTableTest.class
})

public class AllTests {}