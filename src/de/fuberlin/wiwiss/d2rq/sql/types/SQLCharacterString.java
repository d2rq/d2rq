package de.fuberlin.wiwiss.d2rq.sql.types;

import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class SQLCharacterString extends DataType {
	public SQLCharacterString(Vendor syntax) {
		super(syntax, "CHARACTER");
	}
}
