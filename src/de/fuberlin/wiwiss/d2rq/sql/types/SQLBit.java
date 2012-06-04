package de.fuberlin.wiwiss.d2rq.sql.types;

import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class SQLBit extends DataType {
	public SQLBit(Vendor syntax, String name) {
		super(syntax, name);
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String toSQLLiteral(String value) {
		// In SQL-92, BIT is a bit string with a special literal form
		if (!value.matches("^[01]*$")) {
			log.warn("Unsupported BIT format: '" + value + "'; treating as NULL");
			return "NULL";
		}
		return "B" + syntax().quoteStringLiteral(value);
	}
	@Override
	public String valueRegex() {
		return "^[01]*$";
	}
}