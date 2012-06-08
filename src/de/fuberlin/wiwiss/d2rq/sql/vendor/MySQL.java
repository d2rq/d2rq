package de.fuberlin.wiwiss.d2rq.sql.vendor;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.sql.Quoter;
import de.fuberlin.wiwiss.d2rq.sql.Quoter.PatternDoublingQuoter;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBit;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBoolean;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLDate;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLExactNumeric;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLTime;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLTimestamp;

/**
 * This syntax class implements MySQL-compatible SQL syntax.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MySQL extends SQL92 {

	public MySQL() {
		super(true);
	}
	
	@Override
	public String getConcatenationExpression(String[] sqlFragments) {
		StringBuffer result = new StringBuffer("CONCAT(");
		for (int i = 0; i < sqlFragments.length; i++) {
			if (i > 0) {
				result.append(", ");
			}
			result.append(sqlFragments[i]);
		}
		result.append(")");
		return result.toString();
	}

	@Override
	public String quoteIdentifier(String identifier) {
		return backtickEscaper.quote(identifier);
	}
	private final static Quoter backtickEscaper = 
		new PatternDoublingQuoter(Pattern.compile("([\\\\`])"), "`");
	
	@Override
	public String quoteStringLiteral(String s) {
		return singleQuoteEscaperWithBackslash.quote(s);
	}
	private final static Quoter singleQuoteEscaperWithBackslash = 
		new PatternDoublingQuoter(Pattern.compile("([\\\\'])"), "'");
	
	@Override
	public Properties getDefaultConnectionProperties() {
		Properties result = new Properties();
		result.setProperty("autoReconnect", "true");
		result.setProperty("zeroDateTimeBehavior", "convertToNull");
		return result;
	}

	@Override
	// TODO: The MySQL JDBC driver reports TINYINT(1) as BIT, should be handled as xsd:boolean?
	public DataType getDataType(int jdbcType, String name, int size) {

		// MySQL reports BIT columns in result sets as VARBINARY,
		// and formats the value as a number. This makes no sense.
		if (jdbcType == Types.VARBINARY && "BIT".equals(name)) {
			return new MySQLCompatibilityBitDataType(this);
		}
		
		// TINYINT(1) is conventionally treated as BOOLEAN in MySQL.
		// MySQL reports TINYINT(1) either as Types.BIT with size 0,
		// or as Types.BIT with type name still TINYINT. All real BIT
		// types are reported with a size > 0.
		if (jdbcType == Types.BIT && ("TINYINT".equals(name) || size == 0)) {
			return new SQLBoolean(this, name);
		}

		// MySQL supports UNSIGNED varieties of the integer types
		if (name.contains("UNSIGNED")) {
			return new SQLExactNumeric(this, name, jdbcType, true);
		}
		
		// The MySQL driver chokes on some values that are supported by the
		// MySQL database but not by the corresponding Java objects.
		if (jdbcType == Types.DATE) {
			return new MySQLCompatibilityDateDataType(this);
		}
		if (jdbcType == Types.TIME) {
			return new MySQLCompatibilityTimeDataType(this);
		}
		if (jdbcType == Types.TIMESTAMP) {
			return new MySQLCompatibilityTimestampDataType(this);
		}
		
		return super.getDataType(jdbcType, name, size);
	}

	public static class MySQLCompatibilityBitDataType extends SQLBit {
		public MySQLCompatibilityBitDataType(Vendor syntax) {
			super(syntax, "BIT");
		}
		@Override
		public String value(ResultSet resultSet, int column) throws SQLException {
			String value = resultSet.getString(column);
			if (resultSet.wasNull()) return null;
			try {
				// TODO: We would like to request the actual length of the bit type
				// from the type descriptor, and only return the appropriate number
				// of binary digits, but metaData.getPrecision() doesn't
				// work on streaming result sets (because it requires opening
				// a new result set internal to the JDBC driver). So any leading
				// zeroes will be dropped.
				return new BigInteger(value).toString(2);
			} catch (NumberFormatException ex) {
				log.warn("Expected numeric, got '" + value + "'; treating as null");
				return null;
			}
		}
	}
	
	public static class MySQLCompatibilityDateDataType extends SQLDate {
		public MySQLCompatibilityDateDataType(Vendor syntax) {
			super(syntax, "DATE");
		}
		@Override
		public String value(ResultSet resultSet, int column) throws SQLException {
			// MySQL JDBC connector 5.1.18 chokes on the zero/error
			// value 0000-00-00 with a SQLException
			try {
				return super.value(resultSet, column);
			} catch (SQLException ex) {
				return null;
			}
		}
	}
	
	public static class MySQLCompatibilityTimeDataType extends SQLTime {
		public MySQLCompatibilityTimeDataType(Vendor syntax) {
			super(syntax, "TIME");
		}
		@Override
		public String value(ResultSet resultSet, int column) throws SQLException {
			// MySQL JDBC connector 5.1.18 chokes on negative or too
			// large TIME values with a SQLException
			try {
				return super.value(resultSet, column);
			} catch (SQLException ex) {
				log.warn(ex);
				return null;
			}
		}
	}
	
	public static class MySQLCompatibilityTimestampDataType extends SQLTimestamp {
		public MySQLCompatibilityTimestampDataType(Vendor syntax) {
			super(syntax, "TIMESTAMP");
		}
		@Override
		public String value(ResultSet resultSet, int column) throws SQLException {
			// MySQL JDBC connector 5.1.18 chokes on the zero/error
			// value 0000-00-00 00:00:00 with a SQLException
			try {
				return super.value(resultSet, column);
			} catch (SQLException ex) {
				return null;
			}
		}
	}
}
