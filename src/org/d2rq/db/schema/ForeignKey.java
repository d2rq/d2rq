package org.d2rq.db.schema;

/**
 * A SQL foreign key. States that the values of some list of local columns,
 * if non-null, must exist in some list of referenced columns, which may
 * be in the same or a different table.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ForeignKey {	
	private final IdentifierList localKey;
	private final IdentifierList referencedKey;
	private final TableName referencedTable;
	
	public ForeignKey(IdentifierList localColumns, IdentifierList referencedColumns, TableName referencedTable) {
		this.localKey = localColumns;
		this.referencedKey = referencedColumns;
		this.referencedTable = referencedTable;
	}
	
	public IdentifierList getLocalColumns() {
		return localKey;
	}
	
	public IdentifierList getReferencedColumns() {
		return referencedKey;
	}
	
	public TableName getReferencedTable() {
		return referencedTable;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("FK(");
		result.append(localKey.getColumns());
		result.append("=>");
		result.append(referencedTable);
		result.append(referencedKey.getColumns());
		return result.toString();
	}

	public int hashCode() {
		return localKey.hashCode() ^ referencedKey.hashCode() ^ referencedTable.hashCode() ^ 31;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof ForeignKey)) return false;
		ForeignKey other = (ForeignKey) o;
		return localKey.equals(other.localKey) && 
				referencedKey.equals(other.referencedKey) && 
				referencedTable.equals(other.referencedTable);
	}
}
