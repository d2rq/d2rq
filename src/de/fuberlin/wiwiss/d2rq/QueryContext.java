/*
 * $Id: QueryContext.java,v 1.2 2004/08/09 20:16:52 cyganiak Exp $
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
class QueryContext {
	private boolean uriPatternMatched = false;

	protected void setURIPatternMatched(boolean matched) {
		this.uriPatternMatched = matched;
	}

	protected boolean isURIPatternMatched() {
		return this.uriPatternMatched;
	}
}
