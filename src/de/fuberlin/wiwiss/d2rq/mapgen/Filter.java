package de.fuberlin.wiwiss.d2rq.mapgen;

import java.util.Collection;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;

public abstract class Filter {
	
	public static final Filter ALL = new Filter() {
		public boolean matchesSchema(String schema) { return true; }
		public boolean matchesTable(String schema, String table) { return true; }
		public boolean matchesColumn(String schema, String table, String column) { return true; }
		public String getSingleSchema() { return null; }
		public String toString() { return "all"; }
	};
	
	public static final Filter NOTHING = new Filter() {
		public boolean matchesSchema(String schema) { return false; }
		public boolean matchesTable(String schema, String table) { return false; }
		public boolean matchesColumn(String schema, String table, String column) { return false; }
		public String getSingleSchema() { return null; }
		public String toString() { return "none"; }
	};

	public abstract boolean matchesSchema(String schema);
	
	public abstract boolean matchesTable(String schema, String table);
	
	public abstract boolean matchesColumn(String schema, String table, String column);
	
	/**
	 * @return If the filter matches only a single schema, then its name; otherwise, <code>null</code>
	 */
	public abstract String getSingleSchema();
	
	public boolean matches(RelationName table) {
		return matchesTable(table.schemaName(), table.tableName());
	}
	
	public boolean matches(Attribute column) {
		return matchesColumn(column.schemaName(), column.tableName(), column.attributeName());
	}
	
	public boolean matchesAll(Collection<Attribute> columns) {
		for (Attribute column: columns) {
			if (!matches(column)) return false;
		}
		return true;
	}
	
	protected boolean sameSchema(String schema1, String schema2) {
		return schema1 == schema2 || (schema1 != null && schema1.equals(schema2));
	}

	public static interface IdentifierMatcher {
		public abstract boolean matches(String identifier);
		public abstract String getSingleString();
	}
	public static final IdentifierMatcher NULL_MATCHER = new IdentifierMatcher() {
		public boolean matches(String identifier) { return identifier == null; }
		public String getSingleString() { return null; }
		public String toString() { return "null"; }
	};
	public static IdentifierMatcher createStringMatcher(final String s) {
		return new IdentifierMatcher() {
			public boolean matches(String identifier) {
				return s.equals(identifier);
			}
			public String getSingleString() {
				return s;
			}
			public String toString() {
				return "'" + s + "'";
			}
		};
	}
	public static IdentifierMatcher createPatternMatcher(final Pattern pattern) {
		return new IdentifierMatcher() {
			public boolean matches(String identifier) {
				if (identifier == null) return false;
				return pattern.matcher(identifier).matches();
			}
			public String toString() {
				return "/" + pattern.pattern() + "/" + pattern.flags();
			}
			public String getSingleString() {
				return null;
			}
		};
	}
}