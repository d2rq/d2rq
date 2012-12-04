package org.d2rq.db.types;

import org.d2rq.db.vendor.Vendor;


public class SQLInterval extends DataType {
	public SQLInterval(String name) {
		super(name);
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String toSQLLiteral(String value, Vendor vendor) {
		// TODO: Generate appropriate INTERVAL literal 
		return "NULL";
	}
}