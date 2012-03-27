package de.fuberlin.wiwiss.d2rq.sql.vendor;

import java.sql.Connection;
import java.sql.SQLException;

import de.fuberlin.wiwiss.d2rq.sql.SQL;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLCharacterString;

public class PostgreSQL extends SQL92 {

	public PostgreSQL() {
		super(true);
	}

	@Override
	public String quoteBinaryLiteral(String hexString) {
		if (!SQL.isHexString(hexString)) {
			throw new IllegalArgumentException("Not a hex string: '" + hexString + "'");
		}
		return "E'\\\\x" + hexString + "'";
	}

	@Override
	public DataType getDataType(int jdbcType, String name, int size) {
		DataType standard = super.getDataType(jdbcType, name, size);
		if (standard != null) return standard;

		if ("uuid".equals(name)) {
			return new SQLCharacterString(this);
		}

		return null;
	}

	@Override
	public boolean isIgnoredTable(String schema, String table) {
		// PostgreSQL has schemas "information_schema" and "pg_catalog" in every DB
		return "information_schema".equals(schema) || "pg_catalog".equals(schema);				
	}
	
	@Override
	public void initializeConnection(Connection connection) throws SQLException {
		// Disable auto-commit in PostgreSQL to support cursors
		// @see http://jdbc.postgresql.org/documentation/83/query.html
		connection.setAutoCommit(false);
	}
}
