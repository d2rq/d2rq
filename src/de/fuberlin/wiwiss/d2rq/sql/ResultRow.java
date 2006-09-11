package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;

/**
 * A result row returned by a database query, presented as a
 * map from columns to string values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ResultRow.java,v 1.2 2006/09/11 23:02:50 cyganiak Exp $
 */
public interface ResultRow {
	public String get(Attribute column);
}
