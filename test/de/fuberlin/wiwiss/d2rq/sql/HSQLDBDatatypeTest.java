package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;

/*
 * TODO: check that querying for some other valid literals doesn't lead to errors
 * (e.g., for an xsd:dateTime with time zone on a non-timezoned TIMESTAMP column, 
 * or 'abcd' on BIT, or five-digit or negative year on xsd:date, or too long VARCHAR,
 * or generally anything that's a valid XSD literal but not a valid SQL literal of
 * the equivalent type)
 */
public class HSQLDBDatatypeTest extends DatatypeTestBase {
	
	public void setUp() throws Exception {
		initDB("jdbc:hsqldb:mem:d2rq_test", "org.hsqldb.jdbcDriver", null, null, 
				D2RQTestSuite.DIRECTORY + "sql/hsqldb_datatypes.sql", null);
	}

	public void testTinyInt() {
		createMapping("TINYINT");
		assertMappedType("xsd:integer");
		assertValues(new String[]{"0", "1", "-128", "127"});
	}
	
	public void testSmallInt() {
		createMapping("SMALLINT");
		assertMappedType("xsd:integer");
		assertValues(new String[]{"0", "1", "-32768", "32767"});
	}
	
	public void testInteger() {
		createMapping("INTEGER");
		assertMappedType("xsd:integer");
		assertValues(new String[]{"0", "1", "-2147483648", "2147483647"});
	}
	
	public void testBigInt() {
		createMapping("BIGINT");
		assertMappedType("xsd:integer");
		assertValues(new String[]{"0", "1", "-9223372036854775808", "9223372036854775807"});
	}

	private final static String[] NUMERIC_VALUES = 
			{"0", "1", "1000000000000000000", "-1000000000000000000"};
	public void testNumeric() {
		createMapping("NUMERIC");
		assertMappedType("xsd:decimal");
		assertValues(NUMERIC_VALUES);
	}
	
	public void testDecimal() {
		createMapping("DECIMAL");
		assertMappedType("xsd:decimal");
		assertValues(NUMERIC_VALUES);
	}
	
	public void testDecimal_4_2() {
		createMapping("DECIMAL_4_2");
		assertMappedType("xsd:decimal");
		assertValues(new String[]{"0", "1", "4.95", "99.99", "-99.99"});
	}

	private final static String[] DOUBLE_VALUES = 
			{"0.0E0", "1.0E0", "1.0E8", "1.0E-7", "-0.0E0", "INF", "-INF"}; 
	public void testDouble() {
		createMapping("DOUBLE");
		assertMappedType("xsd:double");
		assertValues(DOUBLE_VALUES);
	}
	
	public void testReal() {
		createMapping("REAL");
		assertMappedType("xsd:double");
		assertValues(DOUBLE_VALUES);
	}
	
	public void testFloat() {
		createMapping("FLOAT");
		assertMappedType("xsd:double");
		assertValues(DOUBLE_VALUES);
	}
	
	public void testBoolean() {
		createMapping("BOOLEAN");
		assertMappedType("xsd:boolean");
		assertValues(new String[]{"false", "true"});
	}

	public void testChar_3() {
		createMapping("CHAR_3");
		assertMappedType("xsd:string");
		assertValues(new String[]{"   ", "AOU", "\u00C4\u00D6\u00DC"});
	}
	
	public void testChar() {
		createMapping("CHAR");
		assertMappedType("xsd:string");
		assertValues(new String[]{" ", "A", "\u00C4"});
	}
	
	private final static String[] VARCHAR_VALUES = 
			{"", "   ", "AOU", "\u00C4\u00D6\u00DC"};
	public void testVarchar() {
		createMapping("VARCHAR");
		assertMappedType("xsd:string");
		assertValues(VARCHAR_VALUES);
	}
	
	public void testLongvarchar() {
		createMapping("LONGVARCHAR");
		assertMappedType("xsd:string");
		assertValues(VARCHAR_VALUES);
	}
	
	public void testCLOB() {
		createMapping("CLOB");
		assertMappedType("xsd:string");
		assertValues(new String[]{"   ", "AOU", "\u00C4\u00D6\u00DC"});
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
	
	public void testLongVarBinary() {
		createMapping("LONGVARBINARY");
		assertMappedType("xsd:hexBinary");
		assertValues(VARBINARY_VALUES);
	}

	public void testBLOB() {
		createMapping("BLOB");
		assertMappedType("xsd:hexBinary");
		assertValues(new String[] {"00", "01", "F001F001F001F001"});
	}
	
	public void testBit_4() {
		createMapping("BIT_4");
		assertMappedType("xsd:string");
		assertValues(new String[] {"0000", "0001", "1000", "1111"});
	}

	public void testBit() {
		createMapping("BIT");
		assertMappedType("xsd:string");
		assertValues(new String[] {"0", "1"});
	}

	public void testBitVarying() {
		createMapping("BIT_VARYING");
		assertMappedType("xsd:string");
		assertValues(new String[] {"", "0", "1", "100000000000000"});
	}

	public void testDate() {
		createMapping("DATE");
		assertMappedType("xsd:date");
		assertValues(new String[] {"0001-01-01", "2012-03-07", "9999-12-31"});
	}

	public void testTime() {
		createMapping("TIME");
		assertMappedType("xsd:time");
		assertValues(new String[]{"00:00:00", "20:39:21", "23:59:59"});
	}

	public void testTime_4() {
		createMapping("TIME_4");
		assertMappedType("xsd:time");
		assertValues(new String[]{"00:00:00", "20:39:21.5831", "23:59:59.9999"});
	}

	public void testTimeTZ() {
		createMapping("TIME_TZ");
		assertMappedType("xsd:time");
		assertValues(new String[]{
				"00:00:00Z", "20:39:21Z", "23:59:59Z", 
				"20:39:21Z", "20:39:21-01:00", "20:39:21+11:30"});
	}

	public void testTimeTZ_4() {
		createMapping("TIME_TZ_4");
		assertMappedType("xsd:time");
		assertValues(new String[]{
				"00:00:00Z", "20:39:21.5831Z", "23:59:59.9999Z", 
				"20:39:21.5831Z", "20:39:21.5831-01:00", "20:39:21.5831+11:30"});
	}
	
	public void testTimestamp() {
		createMapping("TIMESTAMP");
		assertMappedType("xsd:dateTime");
		assertValues(new String[]{
				"0001-01-01T00:00:00", 
				"2012-03-07T20:39:21.583122", 
				"9999-12-31T23:59:59.999999"});
	}

	public void testTimestamp_4() {
		createMapping("TIMESTAMP_4");
		assertMappedType("xsd:dateTime");
		assertValues(new String[]{
				"0001-01-01T00:00:00", 
				"2012-03-07T20:39:21.5831", 
				"9999-12-31T23:59:59.9999"});
	}

	public void testTimestampTZ() {
		createMapping("TIMESTAMP_TZ");
		assertMappedType("xsd:dateTime");
		assertValues(new String[]{
				"0001-01-01T00:00:00Z", 
				"2012-03-07T20:39:21.583122Z", 
				"9999-12-31T23:59:59.999999Z", 
				"2012-03-07T20:39:21.583122Z", 
				"2012-03-07T20:39:21.583122-01:00", 
				"2012-03-07T20:39:21.583122+11:30"});
	}

	public void testTimestampTZ_4() {
		createMapping("TIMESTAMP_TZ_4");
		assertMappedType("xsd:dateTime");
		assertValues(new String[]{
				"0001-01-01T00:00:00Z", 
				"2012-03-07T20:39:21.5831Z", 
				"9999-12-31T23:59:59.9999Z", 
				"2012-03-07T20:39:21.5831Z", 
				"2012-03-07T20:39:21.5831-01:00", 
				"2012-03-07T20:39:21.5831+11:30"});
	}

	public void testIntervalDay() {
		createMapping("INTERVAL_DAY");
		// TODO Should be mapped to xsd:duration?
		assertMappedType("xsd:string");
		assertValues(new String[]{"0", "1", "99", "-99"}, false);
		assertValuesNotFindable(new String[]{"0", "1", "99", "-99"});
	}

	public void testIntervalHourMinute() {
		createMapping("INTERVAL_HOUR_MINUTE");
		// TODO Should be mapped to xsd:duration?
		assertMappedType("xsd:string");
		assertValues(new String[]{
				"0:00", "0:01", "1:00", "99:00", "-99:00"}, false);
		assertValuesNotFindable(new String[]{
				"0:00", "0:01", "1:00", "99:00", "-99:00"});
	}

	public void testOther() {
		try {
			createMapping("OTHER");
			fail("Should fail due to DATATYPE_UNMAPPABLE");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.DATATYPE_UNMAPPABLE, ex.errorCode());
		}
	}
	
	public void testArrayInteger() {
		try {
			createMapping("ARRAY_INTEGER");
			fail("Should fail due to DATATYPE_UNMAPPABLE");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.DATATYPE_UNMAPPABLE, ex.errorCode());
		}
	}
}