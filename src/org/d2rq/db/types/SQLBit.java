package org.d2rq.db.types;

import org.d2rq.db.vendor.Vendor;


public class SQLBit extends DataType {
	public SQLBit(String name) {
		super(name);
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String toSQLLiteral(String value, Vendor vendor) {
		// In SQL-92, BIT is a bit string with a special literal form
		if (!value.matches("^[01]*$")) {
			log.warn("Unsupported BIT format: '" + value + "'; treating as NULL");
			return "NULL";
		}
		return "B" + vendor.quoteStringLiteral(value);
	}
	@Override
	public String valueRegex() {
		return "^[01]*$";
	}
}