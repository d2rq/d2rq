package de.fuberlin.wiwiss.d2rq.sql.types;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class SQLBoolean extends DataType {
	public SQLBoolean(Vendor syntax, String name) {
		super(syntax, name);
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String rdfType() {
		return "xsd:boolean";
	}
	@Override
	public String value(ResultSet resultSet, int column) throws SQLException {
		boolean b = resultSet.getBoolean(column);
		if (resultSet.wasNull()) return null;
		return b ? "true" : "false";
	}
	@Override
	public String toSQLLiteral(String value) {
		if ("true".equals(value) || "1".equals(value)) {
			return "TRUE";
		}
		if ("false".equals(value) || "0".equals(value)) {
			return "FALSE";
		}
		log.warn("Unsupported BOOLEAN format: '" + value + "'; treating as NULL");
		return "NULL";
	}
}