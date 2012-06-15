package de.fuberlin.wiwiss.d2rq.sql.vendor;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.expr.BooleanToIntegerCaseExpression;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLApproximateNumeric;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBinary;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLCharacterString;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLTimestamp;
import de.fuberlin.wiwiss.d2rq.sql.types.UnsupportedDataType;

/**
 * This syntax class implements MySQL-compatible SQL syntax.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Oracle extends SQL92 {

	public Oracle() {
		super(false);
	}
	
	@Override
	public Expression getRowNumLimitAsExpression(int limit) {
		if (limit == Database.NO_LIMIT) return Expression.TRUE;
		return SQLExpression.create("ROWNUM <= " + limit);
	}

	@Override
	public String getRowNumLimitAsQueryAppendage(int limit) {
		return "";
	}
	
	@Override
	public String quoteBinaryLiteral(String hexString) {
		return quoteStringLiteral(hexString);
	}

	@Override
	public DataType getDataType(int jdbcType, String name, int size) {
		
		// Doesn't support DISTINCT over LOB types
		if (jdbcType == Types.CLOB || "NCLOB".equals(name)) {
			return new SQLCharacterString(this, name, false);
		}
		if (jdbcType == Types.BLOB) {
			return new SQLBinary(this, name, false);
		}
		
		DataType standard = super.getDataType(jdbcType, name, size);
		if (standard != null) return standard;

		// Special handling for TIMESTAMP(x) WITH LOCAL TIME ZONE
		if (name.contains("WITH LOCAL TIME ZONE") || "TIMESTAMPLTZ".equals(name)) {
			return new OracleCompatibilityTimeZoneLocalDataType(this, name);
		}
		
		// Special handling for TIMESTAMP(x) WITH TIME ZONE
		if(name.contains("WITH TIME ZONE") || "TIMESTAMPTZ".equals(name)) {
			return new OracleCompatibilityTimeZoneDataType(this, name);
		}
		
		// Oracle-specific character string types
		if ("VARCHAR2".equals(name) || "NVARCHAR2".equals(name)) {
			return new SQLCharacterString(this, name, true);
		}

		// Oracle-specific floating point types
    	if ("BINARY_FLOAT".equals(name) || "BINARY_DOUBLE".equals(name)) {
    		return new SQLApproximateNumeric(this, name);
    	}
    	
		// Oracle binary file pointer
		// TODO: We could at least support reading from BFILE, although querying for them seems hard
    	if ("BFILE".equals(name)) {
    		return new UnsupportedDataType(jdbcType, name);
    	}

    	return null;
	}

	@Override
	public void initializeConnection(Connection connection) throws SQLException {
		// Set Oracle date formats 
		Statement stmt = connection.createStatement();
		try {
			stmt.execute("ALTER SESSION SET NLS_DATE_FORMAT = 'SYYYY-MM-DD'");
			stmt.execute("ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'SYYYY-MM-DD HH24:MI:SS'");
			setSessionTimeZone(connection, getTimeZoneForSession().getID());
		} catch (Exception ex) {
			throw new D2RQException(ex);
		} finally {
			stmt.close();
		}
	}

	/**
	 * Sets the session time zone on the connection. We need to call an
	 * Oracle-specific method. We use reflection for this so we can compile
	 * the code even without the Oracle driver on the classpath. This method
	 * does:
	 * 
	 * ((OracleConnection) connection).setSessionTimeZone(timeZoneID);
	 */
	private void setSessionTimeZone(Connection connection, String timeZoneID)
			throws Exception {
		Class<?> c = Class.forName("oracle.jdbc.driver.OracleConnection");
		Method setSessionTimeZone = c.getMethod("setSessionTimeZone", String.class);
		setSessionTimeZone.invoke(connection, timeZoneID);
	}
	
	/**
	 * A separate method for this just to highlight that it needs to
	 * be used in multiple places.
	 */
	private static TimeZone getTimeZoneForSession() {
		return TimeZone.getDefault();
	}
	
	/**
	* Oracle doesn't actually support booolean expressions, except in
	* a few places. Turn the boolean into an int using a CASE statement
	*/
	public Expression booleanExpressionToSimpleExpression(Expression expression) {
	    return new BooleanToIntegerCaseExpression(expression);
	}
	
	@Override
	public boolean isIgnoredTable(String schema, String table) {
		if (Arrays.binarySearch(IGNORED_SCHEMAS, schema) >= 0) return true; 
		if (IGNORED_SCHEMAS_PATTERN.matcher(schema).matches()) return true;
		// Skip Oracle system schemas as well as deleted tables in Oracle's Recycling Bin.
		// The latter have names like MYSCHEMA.BIN$FoHqtx6aQ4mBaMQmlTCPTQ==$0
		if (table.startsWith("BIN$")) return true;
		// Skip nested tables with names like SYS_NTr8Sify7K5pLgQBmQXa5h2g==
		// There are issues where the table cannot be found later
		if (table.startsWith("SYS_NT")) return true;
		return false;
	}
	private static final String[] IGNORED_SCHEMAS = {
		"APPQOSSYS", "CACHEADM", "CTXSYS", "DBSNMP", "EXFSYS", "FLOWS_FILES",
		"MDSYS", "OLAPSYS", "ORDDATA", "ORDSYS", "OUTLN", "OWBSYS",
		"SYS", "SYSMAN", "SYSTEM", "TIMESTEN", "WKSYS", "WK_TEST", "WMSYS",
		"XDB", "XDBEXT", "XDBPM"};
	private final static Pattern IGNORED_SCHEMAS_PATTERN = Pattern.compile("(APEX|FLOWS)_\\d{6}");
	
	/**
	 * getString() doesn't really work for TIMESTAMP WITH LOCAL TIME ZONE,
	 * we have to use getTimestamp() and format the resulting Timestamp object
	 * according to the session's time zone.
	 * 
	 * @author Aftab Iqbal
	 */
	public static class OracleCompatibilityTimeZoneLocalDataType extends SQLTimestamp {
		public OracleCompatibilityTimeZoneLocalDataType(Vendor syntax, String name) {
			super(syntax, name);
		}
		
		@Override
		public String value(ResultSet resultSet, int column) throws SQLException {
			// Hack for TIMESTAMP WITH LOCAL TIME ZONE data type
			Timestamp timestampValue = resultSet.getTimestamp(column);
			return formatForSessionTimeZone(timestampValue);
		}
		
		/**
		 * @param timestamp A timestamp (does not contain time zone information)
		 * @return Formatted as xsd:dateTime in the session's time zone
		 */
		private static String formatForSessionTimeZone(Timestamp timestamp) {
	        Calendar cal = new GregorianCalendar();
	        cal.setTime(timestamp);
	        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	        df.setTimeZone(getTimeZoneForSession());
	        String dateTime = df.format(cal.getTime());
        
	        // Adding colon into time zone value (e.g. +03:00)
	        // as required by xsd:dateTime. SimpleDateFormat can't do it on its own
	        return dateTime.substring(0, dateTime.length()-2) + 
	        		":" + dateTime.substring(dateTime.length()-2);
	    }
	}
	
	public static class OracleCompatibilityTimeZoneDataType extends SQLTimestamp {
		public OracleCompatibilityTimeZoneDataType(Vendor syntax, String name) {
			super(syntax, name);
		}
		@Override
		public String value(ResultSet resultSet, int column) throws SQLException {
			// Hack for Oracle TIMESTAMP WITH TIME ZONE data type
			try {
				return super.value(resultSet, column);
			} catch (SQLException ex) {
				return null;
			}
		}
	}
}
