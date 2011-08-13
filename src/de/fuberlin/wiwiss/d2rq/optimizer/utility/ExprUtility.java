package de.fuberlin.wiwiss.d2rq.optimizer.utility;

import com.hp.hpl.jena.sparql.expr.Expr;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.expr.Expression;

/**
 * Converts a SPARQL filter expression to an SQL expression.
 * 
 * @author Herwig Leimer
 */
public class ExprUtility
{

	/**
	 * Converts a SPARQL filter expression to an SQL expression
	 * 
	 * @param expr The root node of an {@link Expr Expr} tree, contains the SPARQL filter.
	 * @param nodeRelation The relation supplying the values to apply the filter on.
	 * @return The root node of an {@link Expression Expression} tree, if conversion was successful, <code>null</code> otherwise.
	 */
	public static Expression convertExprToSQL(final Expr expr, final NodeRelation nodeRelation)
	{
		TransformExprToSQLApplyer transformer = new TransformExprToSQLApplyer(nodeRelation);

		expr.visit(transformer);

		return transformer.result(); 
	}
}
