package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;

/**
 * A result row returned by a database query, presented as a
 * map from columns to string values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ResultRow.java,v 1.3 2006/09/14 13:12:45 cyganiak Exp $
 */
public interface ResultRow {
	public static final ResultRow NO_ATTRIBUTES = new ResultRow() {
		public String get(Attribute attribute) { return null; }
	};
	
	public String get(Attribute column);
}
