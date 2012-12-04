package org.d2rq.db.schema;

import org.d2rq.db.types.DataType;



public class ColumnDef {
	private final Identifier name;
	private final DataType dataType;
	private final boolean isNullable;

	public ColumnDef(Identifier name, DataType dataType, boolean isNullable) {
		this.name = name;
		this.dataType = dataType;
		this.isNullable = isNullable;
	}

	public Identifier getName() {
		return name;
	}

	public DataType getDataType() {
		return dataType;
	}

	public boolean isNullable() {
		return isNullable;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof ColumnDef)) return false;
		ColumnDef other = (ColumnDef) o;
		return name.equals(other.name) && dataType.equals(other.dataType) && isNullable == other.isNullable;
	}
	
	public int hashCode() {
		return name.hashCode() ^ dataType.hashCode() ^ (isNullable ? 555 : 556);
	}
}
