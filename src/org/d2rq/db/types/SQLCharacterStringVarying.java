package org.d2rq.db.types;



public class SQLCharacterStringVarying extends DataType {
	private final boolean supportsDistinct;
	
	public SQLCharacterStringVarying(String name, boolean supportsDistinct) {
		super(name);
		this.supportsDistinct = supportsDistinct;
	}
	
	@Override
	public boolean supportsDistinct() {
		return supportsDistinct;
	}
}
