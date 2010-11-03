package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * This syntax class implements MySQL-compatible SQL syntax.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: OracleSyntax.java,v 1.2 2010/11/03 18:48:17 cyganiak Exp $
 */
public class OracleSyntax extends SQL92Syntax {

	public OracleSyntax() {
		super(false);
	}
	
	public Expression getRowNumLimitAsExpression(int limit) {
		if (limit == Database.NO_LIMIT) return Expression.TRUE;
		return SQLExpression.create("ROWNUM <= " + limit);
	}

	public String getRowNumLimitAsQueryAppendage(int limit) {
		return "";
	}
}
