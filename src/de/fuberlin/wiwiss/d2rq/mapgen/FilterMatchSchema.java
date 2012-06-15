package de.fuberlin.wiwiss.d2rq.mapgen;


public class FilterMatchSchema extends Filter {
	private final IdentifierMatcher schema;
	
	public FilterMatchSchema(IdentifierMatcher schema) {
		this.schema = schema;
	}
	
	public boolean matchesSchema(String schema) {
		return this.schema.matches(schema);
	}

	public boolean matchesTable(String schema, String table) {
		return matchesSchema(schema);
	}

	public boolean matchesColumn(String schema, String table, String column) {
		return matchesSchema(schema);
	}

	public String getSingleSchema() { return schema.getSingleString(); }
	
	public String toString() {
		StringBuffer result = new StringBuffer("schema(");
		result.append(schema);
		result.append(")");
		return result.toString();
	}
}
