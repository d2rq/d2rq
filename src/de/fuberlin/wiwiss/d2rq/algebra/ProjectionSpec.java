package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * Something to be used in the SELECT clause of a SQL query, e.g.
 * a column name or an expression.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ProjectionSpec.java,v 1.1 2008/04/24 17:48:52 cyganiak Exp $
 */
public interface ProjectionSpec {

	public Set requiredAttributes();
	
	public Expression toExpression();
	
	public String toSQL(ConnectedDB database, AliasMap aliases);
}
