package de.fuberlin.wiwiss.d2rq.mapgen;

public class FilterIncludeExclude extends Filter {
	private final Filter include;
	private final Filter exclude;
	
	public FilterIncludeExclude(Filter include, Filter exclude) {
		this.include = include;
		this.exclude = exclude;
	}
	
	public boolean matchesSchema(String schema) {
		return include.matchesSchema(schema) && !exclude.matchesSchema(schema);
	}

	public boolean matchesTable(String schema, String table) {
		return include.matchesTable(schema, table) && !exclude.matchesTable(schema, table);
	}

	public boolean matchesColumn(String schema, String table, String column) {
		return include.matchesColumn(schema, table, column) && !exclude.matchesColumn(schema, table, column);
	}

	public String getSingleSchema() {
		return include.getSingleSchema();
	}

}
