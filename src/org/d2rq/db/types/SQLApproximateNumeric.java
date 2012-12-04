package org.d2rq.db.types;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.d2rq.db.vendor.Vendor;

import com.hp.hpl.jena.vocabulary.XSD;


public class SQLApproximateNumeric extends DataType {
	public SQLApproximateNumeric(String name) {
		super(name);
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String rdfType() {
		return XSD.xdouble.getURI();
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
			return fmtFloatingPoint.format(d);
//			if (!dd.contains("E")) {
//				// Canonical XSD form requires 0.0E0 form
//				dd += "E0";
//			}
//			return dd;
		}
	}
    static private DecimalFormatSymbols decimalNumberSymbols = new DecimalFormatSymbols(Locale.ROOT) ;
    static private NumberFormat fmtFloatingPoint = new DecimalFormat("0.0#################E0", decimalNumberSymbols) ;
	
	@Override
	public String toSQLLiteral(String value, Vendor vendor) {
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