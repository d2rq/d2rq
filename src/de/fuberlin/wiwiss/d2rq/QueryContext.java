/*
 * $Id: QueryContext.java,v 1.3 2005/04/13 16:55:08 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq;

/**
 * Encapsulates state for the execution of a find query.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class QueryContext {
	private boolean uriPatternMatched = false;

	public void setURIPatternMatched(boolean matched) {
		this.uriPatternMatched = matched;
	}

	public boolean isURIPatternMatched() {
		return this.uriPatternMatched;
	}
}
