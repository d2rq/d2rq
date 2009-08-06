package de.fuberlin.wiwiss.d2rq.optimizer.utility;

import com.hp.hpl.jena.sparql.expr.Expr;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.expr.Expression;

/**
 * Utility-class for expr. 
 * Can convert expr to someone else - current to sql-string. 
 * 
 * @author Herwig Leimer
 *
 */
public class ExprUtility
{

    /**
     * Converts an expression to sql  
     * @param expr - root-node of an expression-tree
     * @return String - the corresponding sql-string
     */
    public static Expression convertExprToSQL(final Expr expr, final NodeRelation nodeRelation)
    {
        TransformExprToSQLApplyer transformExprToSQLApplyer;        
        transformExprToSQLApplyer = new TransformExprToSQLApplyer(nodeRelation);
        
        // start converting expr to sql
        expr.visit(transformExprToSQLApplyer);

        return transformExprToSQLApplyer.result(); 
    }
}
