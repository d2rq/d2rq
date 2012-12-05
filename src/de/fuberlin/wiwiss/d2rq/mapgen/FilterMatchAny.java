package de.fuberlin.wiwiss.d2rq.mapgen;

import java.util.Collection;

public class FilterMatchAny extends Filter {

	public static Filter create(Collection<Filter> elements) {
		if (elements.isEmpty()) {
			return Filter.NOTHING;
		}
		if (elements.size() == 1) {
			return elements.iterator().next();
		}
		return new FilterMatchAny(elements);
	}
	
	private final Collection<Filter> elements;
	
	public FilterMatchAny(Collection<Filter> elements) {
		this.elements = elements;
	}
	
	public boolean matchesSchema(String schema) {
		for (Filter filter: elements) {
			if (filter.matchesSchema(schema)) return true;
		}
		return false;
	}

	public boolean matchesTable(String schema, String table) {
		for (Filter filter: elements) {
			if (filter.matchesTable(schema, table)) return true;
		}
		return false;
	}

	public boolean matchesColumn(String schema, String table, String column) {
		for (Filter filter: elements) {
			if (filter.matchesColumn(schema, table, column)) return true;
		}
		return false;
	}

	public String getSingleSchema() {
		String result = null;
		for (Filter filter: elements) {
			if (filter.getSingleSchema() == null) continue;
			if (result == null) {
				result = filter.getSingleSchema();
			} else if (!result.equals(filter.getSingleSchema())) {
				return null;
			}
		}
		return null;
	}
	
	public String toString() {
		if (elements.size() == 0) {
			return "any(-)";
		}
		StringBuilder builder = new StringBuilder("any(");
		for (Filter filter: elements) {
			builder.append(filter);
			builder.append(",");
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append(")");
		return builder.toString();
	}
}
