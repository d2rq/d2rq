package de.fuberlin.wiwiss.d2rq.sql.types;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class SQLApproximateNumeric extends DataType {
	public SQLApproximateNumeric(Vendor syntax, String name) {
		super(syntax, name);
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String rdfType() {
		return "xsd:double";
	}
	@Override
	public String value(ResultSet resultSet, int column) throws SQLException {
		double d = resultSet.getDouble(column);
		if (resultSet.wasNull()) return null;
		if (Double.isNaN(d)) {
			return "NaN";
		} else if (Double.isInfinite(d)) {
			return d > 0 ? "INF" : "-INF";
		} else if (d == Double.NEGATIVE_INFINITY) {
			return "-INF";
		} else {
			String dd = Double.toString(d);
			if (!dd.contains("E")) {
				// Canonical XSD form requires 0.0E0 form
				dd += "E0";
			}
			return dd;
		}
	}
	@Override
	public String toSQLLiteral(String value) {
		try {
			return new BigDecimal(value).toString();
		} catch (NumberFormatException ex) {
			try {
				double d = Double.parseDouble(value);
				if (Double.isNaN(d) || Double.isInfinite(d)) {
					// Valid in xsd:double, but not supported by vanilla DBs
					return "NULL";
				}
				return Double.toString(d);
			} catch (NumberFormatException ex2) {
				// Not a number AFAICT
				log.warn("Unsupported DOUBLE format: '" + value + "'; treating as NULL");
				return "NULL";
			}
		}
	}
}