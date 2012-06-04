package de.fuberlin.wiwiss.d2rq.sql.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

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

		if ("UUID".equals(name)) {
			return new SQLCharacterString(this, name, true);
		}
		
		// As postGis jdbc is only a wrapper of the org.postgresql.Driver,
		// the JDBC database product type is the one of Postgresql : PostgreSQL
		// Thus Postgis field as geometry are handled here
		if ((jdbcType == Types.OTHER) && ("GEOMETRY".equals(name))) {
			// let try the simpliest version
			return new SQLCharacterString(this, name, true);
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
