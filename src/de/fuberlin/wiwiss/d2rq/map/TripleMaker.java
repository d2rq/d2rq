package de.fuberlin.wiwiss.d2rq.map;

import java.util.Collection;

/**
 * Knows how to create triples from String arrays produced by a database query.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TripleMaker.java,v 1.2 2006/09/09 20:51:48 cyganiak Exp $
 */
public interface TripleMaker {

	/**
	 * Creates triples from a database result row. May return the empty list.
	 * @param row a database result row
	 * @return Triples extracted from the row
	 */
	public Collection makeTriples(String[] row);
}
