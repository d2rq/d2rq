package org.d2rq.db.types;



public class SQLCharacterString extends DataType {
	private final boolean supportsDistinct;
	
	public SQLCharacterString(String name, boolean supportsDistinct) {
		super(name);
		this.supportsDistinct = supportsDistinct;
	}
	
	@Override
	public boolean supportsDistinct() {
		return supportsDistinct;
	}
}
