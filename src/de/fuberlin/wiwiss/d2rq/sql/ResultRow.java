package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.map.Column;

/**
 * A result row returned by a database query, presented as a
 * map from columns to string values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ResultRow.java,v 1.1 2006/09/09 23:25:16 cyganiak Exp $
 */
public interface ResultRow {
	public String get(Column column);
}
