package de.fuberlin.wiwiss.d2rq.sql.vendor;

import java.sql.Types;

import de.fuberlin.wiwiss.d2rq.expr.BooleanToIntegerCaseExpression;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.SQL;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBinary;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBit;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLCharacterString;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLDate;

/**
 * This syntax class implements SQL syntax for MS SQL Server
 * and MS Access.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SQLServer extends SQL92 {

	public SQLServer() {
		super(true);
	}
	
	@Override
	public String getRowNumLimitAsSelectModifier(int limit) {
		if (limit == Database.NO_LIMIT) return "";
		return "TOP " + limit;
	}

	@Override
	public String getRowNumLimitAsQueryAppendage(int limit) {
		return "";
	}
	
	@Override
	public String quoteBinaryLiteral(String hexString) {
		if (!SQL.isHexString(hexString)) {
			throw new IllegalArgumentException("Not a hex string: '" + hexString + "'");
		}
		return "0x" + hexString;		
	}
	
	@Override
	public String quoteDateLiteral(String date) {
		// TODO Reportedly, MS Access requires #YYYY-MM-DD#
		return quoteStringLiteral(date);
	}
	
	@Override
	public String quoteTimeLiteral(String time) {
		// TODO Reportedly, MS Access requires #HH:mm:ss#
		return quoteStringLiteral(time);
	}
	
	@Override
	public String quoteTimestampLiteral(String timestamp) {
		// TODO Reportedly, MS Access requires #YYYY-MM-DD HH:mm:ss#
		return quoteStringLiteral(timestamp);
	}

	@Override
	public DataType getDataType(int jdbcType, String name, int size) {
		// MS SQLServer 2008 driver returns DATE as VARCHAR type
		if (name.equals("DATE")) {
			return new SQLDate(this, name);
		}
		
		// On SQL Server, BIT is a single-bit numeric type
		if (jdbcType == Types.BIT) {
			return new SQLServerCompatibilityBitDataType(this);
		}

		// Doesn't support DISTINCT over LOB types
		if (jdbcType == Types.CLOB || "NCLOB".equals(name)) {
			return new SQLCharacterString(this, name, false);
		}
		if (jdbcType == Types.BLOB) {
			return new SQLBinary(this, name, false);
		}

		return super.getDataType(jdbcType, name, size);
	}
	
	/**
	* Expressions can not return true or false in Microsoft SQL 
	* Server. (No boolean type) But, the entire expression can be 
	* moved inside a CASE WHEN statement to return an int for the 
	* boolean result. 
	*/
	public Expression booleanExpressionToSimpleExpression(Expression expression) {
	    return new BooleanToIntegerCaseExpression(expression);
	}
	
	@Override
	public boolean isIgnoredTable(String schema, String table) {
		// MS SQL Server has schemas "sys" and "information_schema" in every DB
        // along with tables which need to be ignored
		return "sys".equals(schema) || "INFORMATION_SCHEMA".equals(schema) || "sysdiagrams".equals(table);
	}

	private static class SQLServerCompatibilityBitDataType extends SQLBit {
		public SQLServerCompatibilityBitDataType(Vendor syntax) {
			super(syntax, "BIT");
		}
		public String toSQLLiteral(String value) {
			// On SQL Server, BIT is a single-bit numeric type
			try {
				return Integer.parseInt(value) == 0 ? "0" : "1";
			} catch (NumberFormatException nfex) {
				// Not 0 or 1
				DataType.log.warn("Unsupported BIT format: '" + value + "'; treating as NULL");
				return "NULL";
			}
		}
		public String valueRegex() {
			return "^[01]$";
		}
	}
}
