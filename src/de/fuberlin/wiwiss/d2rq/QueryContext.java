/*
 * $Id: QueryContext.java,v 1.1 2004/08/02 22:48:44 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

/**
 * Encapsulates state for the execution of a find query.
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
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
