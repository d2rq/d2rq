package de.fuberlin.wiwiss.d2rq.mapgen;


public class FilterMatchTable extends Filter {
	private final IdentifierMatcher schema;
	private final IdentifierMatcher table;
	private final boolean matchParents;
	
	public FilterMatchTable(IdentifierMatcher schema, IdentifierMatcher table, boolean matchParents) {
		this.schema = schema;
		this.table = table;
		this.matchParents = matchParents;
	}
	
	public boolean matchesSchema(String schema) {
		if (!matchParents) return false;
		return this.schema.matches(schema);
	}

	public boolean matchesTable(String schema, String table) {
		return this.schema.matches(schema) && this.table.matches(table);
	}

	public boolean matchesColumn(String schema, String table, String column) {
		return matchesTable(schema, table);
	}

	public String getSingleSchema() { return schema.getSingleString(); }
}
