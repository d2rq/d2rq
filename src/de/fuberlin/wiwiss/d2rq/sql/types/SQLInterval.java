package de.fuberlin.wiwiss.d2rq.sql.types;

import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class SQLInterval extends DataType {
	public SQLInterval(Vendor syntax) {
		super(syntax, "INTERVAL");
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String toSQLLiteral(String value) {
		// TODO: Generate appropriate INTERVAL literal 
		return "NULL";
	}
}