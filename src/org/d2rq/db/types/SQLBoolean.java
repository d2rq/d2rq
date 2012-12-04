package org.d2rq.db.types;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.d2rq.db.vendor.Vendor;

import com.hp.hpl.jena.vocabulary.XSD;


public class SQLBoolean extends DataType {
	public SQLBoolean(String name) {
		super(name);
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String rdfType() {
		return XSD.xboolean.getURI();
	}
	@Override
	public String value(ResultSet resultSet, int column) throws SQLException {
		boolean b = resultSet.getBoolean(column);
		if (resultSet.wasNull()) return null;
		return b ? "true" : "false";
	}
	@Override
	public String toSQLLiteral(String value, Vendor vendor) {
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