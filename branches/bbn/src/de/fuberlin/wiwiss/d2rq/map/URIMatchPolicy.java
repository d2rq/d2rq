/*
 * $Id: URIMatchPolicy.java,v 1.1 2006/04/12 09:53:04 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.map;

import de.fuberlin.wiwiss.d2rq.find.QueryContext;



/**
 * Encapsulates code for this optimizing rule: If a query URI matches
 * some NodeMaker based on a URI pattern, then don't check any NodeMakers
 * that are based on an URI column.
 *
 * TODO: Isn't really Policy pattern; use other name or refactor?
 * 
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class URIMatchPolicy {
	private boolean isSubjectBasedOnURIPattern = false;
	private boolean isSubjectBasedOnURIColumn = false;
	private boolean isObjectBasedOnURIPattern = false;
	private boolean isObjectBasedOnURIColumn = false;

	protected void setObjectBasedOnURIColumn(boolean isObjectBasedOnURIColumn) {
		this.isObjectBasedOnURIColumn = isObjectBasedOnURIColumn;
	}

	protected void setObjectBasedOnURIPattern(boolean isObjectBasedOnURIPattern) {
		this.isObjectBasedOnURIPattern = isObjectBasedOnURIPattern;
	}

	protected void setSubjectBasedOnURIColumn(boolean isSubjectBasedOnURIColumn) {
		this.isSubjectBasedOnURIColumn = isSubjectBasedOnURIColumn;
	}

	protected void setSubjectBasedOnURIPattern(boolean isSubjectBasedOnURIPattern) {
		this.isSubjectBasedOnURIPattern = isSubjectBasedOnURIPattern;
	}
	
	protected boolean couldFitSubjectInContext(QueryContext context) {
		return !this.isSubjectBasedOnURIColumn || !context.isURIPatternMatched();
	}
	
	protected void updateContextAfterSubjectMatch(QueryContext context) {
		if (this.isSubjectBasedOnURIPattern) {
			context.setURIPatternMatched(true);
		}
	}
	
	protected boolean couldFitObjectInContext(QueryContext context) {
		return !this.isObjectBasedOnURIColumn || !context.isURIPatternMatched();
	}
	
	protected void updateContextAfterObjectMatch(QueryContext context) {
		if (this.isObjectBasedOnURIPattern) {
			context.setURIPatternMatched(true);
		}
	}

	protected int getEvaluationPriority() {
		int result = 0;
		if (this.isObjectBasedOnURIColumn) {
			result -= 1;
		}
		if (this.isSubjectBasedOnURIColumn) {
			result -= 1;
		}
		if (this.isObjectBasedOnURIPattern) {
			result += 2;
		}
		if (this.isSubjectBasedOnURIPattern) {
			result += 2;
		}
		return result;
	}
}
