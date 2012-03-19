package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;

/**
 * A result row returned by a database query, presented as a
 * map from columns to string values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface ResultRow {
	public static final ResultRow NO_ATTRIBUTES = new ResultRow() {
		public String get(ProjectionSpec attribute) { return null; }
	};
	
	public String get(ProjectionSpec column);
}
