package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * This syntax class implements MySQL-compatible SQL syntax.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
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
