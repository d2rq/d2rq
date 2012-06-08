package de.fuberlin.wiwiss.d2rq.sql.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLApproximateNumeric;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBinary;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLCharacterString;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLInterval;
import de.fuberlin.wiwiss.d2rq.sql.types.UnsupportedDataType;

public class HSQLDB extends SQL92 {

	public HSQLDB() {
		super(true);
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
		
		// HSQLDB 2.2.8 reports INTERVAL types as VARCHAR 
		if (jdbcType == Types.VARCHAR && name.startsWith("INTERVAL")) {
			return new SQLInterval(this, name);
		}
		
		// HSQLDB supports NaN and +/-INF in DOUBLE
		if (jdbcType == Types.DOUBLE || jdbcType == Types.FLOAT || jdbcType == Types.REAL) {
			return new HSQLDBCompatibilityDoubleDataType(this);
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

	public static class HSQLDBCompatibilityDoubleDataType extends SQLApproximateNumeric {
		public HSQLDBCompatibilityDoubleDataType(Vendor syntax) {
			super(syntax, "DOUBLE");
		}
		public String toSQLLiteral(String value) {
			if ("NaN".equals(value)) {
				return "(0E0/0E0)";
			} else if ("INF".equals(value)) {
				return "(1E0/0)";
			} else if ("-INF".equals(value)) {
				return "(-1E0/0)";
			}
			return super.toSQLLiteral(value);
		}
	}
}
