package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * Something to be used in the SELECT clause of a SQL query, e.g.
 * a column name or an expression.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface ProjectionSpec extends Comparable<ProjectionSpec> {

	public Set<Attribute> requiredAttributes();
	
	public ProjectionSpec renameAttributes(ColumnRenamer renamer);
	
	public Expression toExpression();
	
	public String toSQL(ConnectedDB database, AliasMap aliases);
	
	public Expression notNullExpression(ConnectedDB database, AliasMap aliases);
}
