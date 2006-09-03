package de.fuberlin.wiwiss.d2rq.find;

/**
 * Encapsulates state for the execution of a find query.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: QueryContext.java,v 1.2 2006/09/03 00:08:13 cyganiak Exp $
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
