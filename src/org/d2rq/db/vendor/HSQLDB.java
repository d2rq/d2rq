package org.d2rq.db.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.SQLApproximateNumeric;
import org.d2rq.db.types.SQLBinary;
import org.d2rq.db.types.SQLCharacterString;
import org.d2rq.db.types.SQLInterval;
import org.d2rq.db.types.UnsupportedDataType;


public class HSQLDB extends SQL92 {

	public HSQLDB() {
		super(true);
	}
	
	@Override
	public DataType getDataType(int jdbcType, String name, int size) {

		// Doesn't support DISTINCT over LOB types
		if (jdbcType == Types.CLOB || "NCLOB".equals(name)) {
			return new SQLCharacterString(name, false);
		}
		if (jdbcType == Types.BLOB) {
			return new SQLBinary(name, false);
		}
		
		// HSQLDB 2.2.8 reports INTERVAL types as VARCHAR 
		if (jdbcType == Types.VARCHAR && name.startsWith("INTERVAL")) {
			return new SQLInterval(name);
		}
		
		// HSQLDB supports NaN and +/-INF in DOUBLE
		if (jdbcType == Types.DOUBLE || jdbcType == Types.FLOAT || jdbcType == Types.REAL) {
			return new HSQLDBCompatibilityDoubleDataType();
		}
		
    	// OTHER in HSQLDB 2.2.8 is really JAVA_OBJECT
		if (jdbcType == Types.OTHER) {
			return new UnsupportedDataType(jdbcType, name);
		}

		return super.getDataType(jdbcType, name, size);
	}

	@Override
	public void initializeConnection(Connection connection) throws SQLException {
		// Enable storage of special Double values: NaN, INF, -INF
		Statement stmt = connection.createStatement();
		try {
			stmt.execute("SET DATABASE SQL DOUBLE NAN FALSE");
		} finally {
			stmt.close();
		}
	}

	@Override
	public TableName toQualifiedTableName(String catalog,
			String schema, String table) {
		// Call the tables in default schema "FOO", not "PUBLIC.FOO"
		if (schema != null && "public".equals(schema.toLowerCase())) { 
			return super.toQualifiedTableName(null, null, table);
		}
		return super.toQualifiedTableName(catalog, schema, table);
	}

	public static class HSQLDBCompatibilityDoubleDataType extends SQLApproximateNumeric {
		public HSQLDBCompatibilityDoubleDataType() {
			super("DOUBLE");
		}
		public String toSQLLiteral(String value, Vendor vendor) {
			if ("NaN".equals(value)) {
				return "(0E0/0E0)";
			} else if ("INF".equals(value)) {
				return "(1E0/0)";
			} else if ("-INF".equals(value)) {
				return "(-1E0/0)";
			}
			return super.toSQLLiteral(value, vendor);
		}
	}
}
