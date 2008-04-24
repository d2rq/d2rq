package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;

/**
 * A result row returned by a database query, presented as a
 * map from columns to string values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ResultRow.java,v 1.4 2008/04/24 17:48:53 cyganiak Exp $
 */
public interface ResultRow {
	public static final ResultRow NO_ATTRIBUTES = new ResultRow() {
		public String get(ProjectionSpec attribute) { return null; }
	};
	
	public String get(ProjectionSpec column);
}
