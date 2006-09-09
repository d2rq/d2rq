package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Collection;

/**
 * Something that can create RDF triples from a query result row.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TripleMaker.java,v 1.1 2006/09/09 23:25:16 cyganiak Exp $
 */
public interface TripleMaker {
	Collection makeTriples(ResultRow row);
}
