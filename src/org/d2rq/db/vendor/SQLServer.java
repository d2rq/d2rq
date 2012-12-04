package org.d2rq.db.vendor;

import java.sql.Types;

import org.d2rq.db.types.DataType;
import org.d2rq.db.types.SQLBinary;
import org.d2rq.db.types.SQLBit;
import org.d2rq.db.types.SQLCharacterString;
import org.d2rq.db.types.SQLDate;
import org.d2rq.lang.Database;


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
	public String getTrueTable() {
		return null;
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
		if (!SQLBinary.isHexString(hexString)) {
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
			return new SQLDate(name);
		}
		
		// On SQL Server, BIT is a single-bit numeric type
		if (jdbcType == Types.BIT) {
			return new SQLServerCompatibilityBitDataType();
		}

		// Doesn't support DISTINCT over LOB types
		if (jdbcType == Types.CLOB || "NCLOB".equals(name)) {
			return new SQLCharacterString(name, false);
		}
		if (jdbcType == Types.BLOB) {
			return new SQLBinary(name, false);
		}

		return super.getDataType(jdbcType, name, size);
	}

	@Override
	public boolean isIgnoredTable(String catalog, String schema, String table) {
		// MS SQL Server has schemas "sys" and "information_schema" in every DB
        // along with tables which need to be ignored
		return "sys".equals(schema) || "INFORMATION_SCHEMA".equals(schema) || "sysdiagrams".equals(table);
	}

	private static class SQLServerCompatibilityBitDataType extends SQLBit {
		public SQLServerCompatibilityBitDataType() {
			super("BIT");
		}
		public String toSQLLiteral(String value, Vendor vendor) {
			// On SQL Server, BIT is a single-bit numeric type
			try {
				return Integer.parseInt(value) == 0 ? "0" : "1";
			} catch (NumberFormatException nfex) {
				// Not 0 or 1
				log.warn("Unsupported BIT format: '" + value + "'; treating as NULL");
				return "NULL";
			}
		}
		public String valueRegex() {
			return "^[01]$";
		}
	}
}
