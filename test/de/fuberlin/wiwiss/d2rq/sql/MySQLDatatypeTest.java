package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;

/**
 * TODO: Put MySQL connection details into a properties file
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MySQLDatatypeTest extends DatatypeTestBase {
	
	public void setUp() throws Exception {
		initDB("jdbc:mysql:///d2rq_test", "com.mysql.jdbc.Driver", "root", null, 
				D2RQTestSuite.DIRECTORY + "sql/mysql_datatypes.sql", null);
	}

	public void testSerial() {
		createMapping("SERIAL");
		assertMappedType("xsd:unsignedLong");
		assertValues(new String[]{"1", "2", "18446744073709551615"});
	}
	
	public void testBit_4() {
		createMapping("BIT_4");
		assertMappedType("xsd:string");
		assertValues(new String[] {"0", "1", "1000", "1111"});
	}

	public void testBit() {
		createMapping("BIT");
		assertMappedType("xsd:string");
		assertValues(new String[] {"0", "1"});
	}

	public void testTinyInt() {
		createMapping("TINYINT");
		assertMappedType("xsd:byte");
		assertValues(new String[]{"0", "1", "-128", "127"});
	}
	
	public void testTinyInt1() {
		createMapping("TINYINT_1");
		assertMappedType("xsd:boolean");
		assertValues(new String[]{"false", "true", "true"});
	}
	
	public void testTinyIntUnsigned() {
		createMapping("TINYINT_UNSIGNED");
		assertMappedType("xsd:unsignedByte");
		assertValues(new String[]{"0", "1", "255"});
	}
	
	public void testSmallInt() {
		createMapping("SMALLINT");
		assertMappedType("xsd:short");
		assertValues(new String[]{"0", "1", "-32768", "32767"});
	}
	
	public void testSmallIntUnsigned() {
		createMapping("SMALLINT_UNSIGNED");
		assertMappedType("xsd:unsignedShort");
		assertValues(new String[]{"0", "1", "65535"});
	}
	
	public void testMediumInt() {
		createMapping("MEDIUMINT");
		assertMappedType("xsd:int");
		assertValues(new String[]{"0", "1", "-8388608", "8388607"});
	}
	
	public void testMediumIntUnsigned() {
		createMapping("MEDIUMINT_UNSIGNED");
		assertMappedType("xsd:unsignedInt");
		assertValues(new String[]{"0", "1", "16777215"});
	}
	
	public void testInteger() {
		createMapping("INTEGER");
		assertMappedType("xsd:int");
		assertValues(new String[]{"0", "1", "-2147483648", "2147483647"});
	}
	
	public void testIntegerUnsigned() {
		createMapping("INTEGER_UNSIGNED");
		assertMappedType("xsd:unsignedInt");
		assertValues(new String[]{"0", "1", "4294967295"});
	}
	
	public void testInt() {
		createMapping("INT");
		assertMappedType("xsd:int");
		assertValues(new String[]{"0", "1", "-2147483648", "2147483647"});
	}
	
	public void testIntUnsigned() {
		createMapping("INT_UNSIGNED");
		assertMappedType("xsd:unsignedInt");
		assertValues(new String[]{"0", "1", "4294967295"});
	}
	
	public void testBigInt() {
		createMapping("BIGINT");
		assertMappedType("xsd:long");
		assertValues(new String[]{"0", "1", "-9223372036854775808", "9223372036854775807"});
	}

	public void testBigIntUnsigned() {
		createMapping("BIGINT_UNSIGNED");
		assertMappedType("xsd:unsignedLong");
		assertValues(new String[]{"0", "1", "18446744073709551615"});
	}

	private final static String[] DECIMAL_VALUE = {"0", "1", "100000000", "-100000000"};
	public void testDecimal() {
		createMapping("DECIMAL");
		assertMappedType("xsd:decimal");
		assertValues(DECIMAL_VALUE);
	}
	
	public void testDecimal_4_2() {
		createMapping("DECIMAL_4_2");
		assertMappedType("xsd:decimal");
		assertValues(new String[]{"0", "1", "4.95", "99.99", "-99.99"});
	}

	public void testDec() {
		createMapping("DEC");
		assertMappedType("xsd:decimal");
		assertValues(DECIMAL_VALUE);
	}
	
	public void testDec_4_2() {
		createMapping("DEC_4_2");
		assertMappedType("xsd:decimal");
		assertValues(new String[]{"0", "1", "4.95", "99.99", "-99.99"});
	}

	private final static String[] FLOAT_VALUES = 
			{"0.0E0", "1.0E0", "-1.0E0", 
			"-3.0E38", "-1.0E-38", 
			"1.0E-38", "3.0E38"}; 
	public void testFloat() {
		createMapping("FLOAT");
		assertMappedType("xsd:double");
		// TODO: Fuzzy match to search for floating-point values
		assertValues(FLOAT_VALUES, false);
	}
	
	private final static String[] DOUBLE_VALUES = 
			{"0.0E0", "1.0E0", "-1.0E0", 
			"-1.0E308", "-2.0E-308", 
			"2.0E-308", "1.0E308"}; 
	public void testDouble() {
		createMapping("DOUBLE");
		assertMappedType("xsd:double");
		// TODO: Fuzzy match to search for floating-point values
		assertValues(DOUBLE_VALUES, false);
	}
	
	public void testReal() {
		createMapping("REAL");
		assertMappedType("xsd:double");
		// TODO: Fuzzy match to search for floating-point values
		assertValues(DOUBLE_VALUES, false);
	}
	
	public void testDoublePrecision() {
		createMapping("DOUBLE_PRECISION");
		assertMappedType("xsd:double");
		// TODO: Fuzzy match to search for floating-point values
		assertValues(DOUBLE_VALUES, false);
	}
	
	public void testChar_3() {
		createMapping("CHAR_3");
		assertMappedType("xsd:string");
		assertValues(new String[]{"", "AOU", "\u00C4\u00D6\u00DC"});
	}
	
	private final static String[] CHAR_VALUES = {"", "A", "\u00C4"};
	public void testChar() {
		createMapping("CHAR");
		assertMappedType("xsd:string");
		assertValues(CHAR_VALUES);
	}
	
	public void testCharacter() {
		createMapping("CHARACTER");
		assertMappedType("xsd:string");
		assertValues(CHAR_VALUES);
	}
	
	public void testNationalCharacter() {
		createMapping("NATIONAL_CHARACTER");
		assertMappedType("xsd:string");
		assertValues(CHAR_VALUES);
	}
	
	public void testNChar() {
		createMapping("NCHAR");
		assertMappedType("xsd:string");
		assertValues(CHAR_VALUES);
	}
	
	private final static String[] VARCHAR_VALUES = 
			{"", "   ", "AOU", "\u00C4\u00D6\u00DC"};
	public void testVarchar() {
		createMapping("VARCHAR");
		assertMappedType("xsd:string");
		assertValues(VARCHAR_VALUES);
	}
	
	public void testNVarchar() {
		createMapping("NVARCHAR");
		assertMappedType("xsd:string");
		assertValues(VARCHAR_VALUES);
	}
	
	public void testNationalVarchar() {
		createMapping("NATIONAL_VARCHAR");
		assertMappedType("xsd:string");
		assertValues(VARCHAR_VALUES);
	}
	
	public void testTinyText() {
		createMapping("TINYTEXT");
		assertMappedType("xsd:string");
		assertValues(VARCHAR_VALUES);
	}
	
	public void testMediumText() {
		createMapping("MEDIUMTEXT");
		assertMappedType("xsd:string");
		assertValues(VARCHAR_VALUES);
	}
	
	public void testText() {
		createMapping("TEXT");
		assertMappedType("xsd:string");
		assertValues(VARCHAR_VALUES);
	}
	
	public void testLongText() {
		createMapping("LONGTEXT");
		assertMappedType("xsd:string");
		assertValues(VARCHAR_VALUES);
	}
	
	public void testBinary_4() {
		createMapping("BINARY_4");
		assertMappedType("xsd:hexBinary");
		assertValues(new String[] {"00000000", "FFFFFFFF", "F001F001"});
	}
	
	public void testBinary() {
		createMapping("BINARY");
		assertMappedType("xsd:hexBinary");
		assertValues(new String[] {"00", "01", "FF"});
	}
	
	private final static String[] VARBINARY_VALUES = 
			{"", "00", "01", "F001F001F001F001"};
	public void testVarBinary() {
		createMapping("VARBINARY");
		assertMappedType("xsd:hexBinary");
		assertValues(VARBINARY_VALUES);
	}
	
	public void testTinyBLOB() {
		createMapping("TINYBLOB");
		assertMappedType("xsd:hexBinary");
		assertValues(VARBINARY_VALUES);
	}
	
	public void testMediumBLOB() {
		createMapping("MEDIUMBLOB");
		assertMappedType("xsd:hexBinary");
		assertValues(VARBINARY_VALUES);
	}
	
	public void testBLOB() {
		createMapping("BLOB");
		assertMappedType("xsd:hexBinary");
		assertValues(VARBINARY_VALUES);
	}
	
	public void testLongBLOB() {
		createMapping("LONGBLOB");
		assertMappedType("xsd:hexBinary");
		assertValues(VARBINARY_VALUES);
	}
	
	public void testDate() {
		createMapping("DATE");
		assertMappedType("xsd:date");
		assertValues(new String[] {"1000-01-01", "2012-03-07", 
				"9999-12-31", "1978-11-30", "1978-11-30"});
	}

	public void testDateTime() {
		createMapping("DATETIME");
		assertMappedType("xsd:dateTime");
		assertValues(new String[]{
			"1000-01-01T00:00:00", 
			"2012-03-07T20:39:21", 
			"9999-12-31T23:59:59",
			"1978-11-30T00:00:00",
			"1978-11-30T00:00:00"});
	}

	public void testTimestamp() {
		createMapping("TIMESTAMP");
		assertMappedType("xsd:dateTime");
		assertValues(new String[]{
			"1970-01-01T00:00:01", 
			"2012-03-07T20:39:21", 
			"2038-01-19T03:14:07"});
	}

	public void testTime() {
		createMapping("TIME");
		assertMappedType("xsd:time");
		assertValues(new String[]{"00:00:00", "20:39:21", "23:59:59"});
	}

	public void testYear() {
		createMapping("YEAR");
		assertMappedType("xsd:date");
		assertValues(new String[] {"1901-01-01", "2012-01-01", "2155-01-01"}); 
	}

	public void testYear4() {
		createMapping("YEAR_4");
		assertMappedType("xsd:date");
		assertValues(new String[] {"1901-01-01", "2012-01-01", "2155-01-01"}); 
	}

	public void testYear2() {
		createMapping("YEAR_2");
		assertMappedType("xsd:date");
		assertValues(new String[] {"1970-01-01", "2012-01-01", "2069-01-01"}); 
	}
	
	public void testEnum() {
		createMapping("ENUM");
		assertMappedType("xsd:string");
		assertValues(new String[] {"foo", "bar"}); 
	}
	
	public void testSet() {
		createMapping("SET");
		assertMappedType("xsd:string");
		assertValues(new String[] {"", "foo", "bar", "foo,bar", "foo,bar"}); 
	}
}