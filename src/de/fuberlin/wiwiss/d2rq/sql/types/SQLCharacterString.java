package de.fuberlin.wiwiss.d2rq.sql.types;

import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class SQLCharacterString extends DataType {
	private final boolean supportsDistinct;
	
	public SQLCharacterString(Vendor syntax, boolean supportsDistinct) {
		super(syntax, "CHARACTER");
		this.supportsDistinct = supportsDistinct;
	}
	
	@Override
	public boolean supportsDistinct() {
		return supportsDistinct;
	}
}
