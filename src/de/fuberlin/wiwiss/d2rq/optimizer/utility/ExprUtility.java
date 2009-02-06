package de.fuberlin.wiwiss.d2rq.optimizer.utility;

import com.hp.hpl.jena.sparql.expr.Expr;

import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;

/**
 * Utility-class for expr. 
 * Can convert expr to someone else. 
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
    public static String convertExprToSQL(final Expr expr, final NodeRelation nodeRelation)
    {
        TransformExprToSQLApplyer transformExprToSQLApplyer;
        String sqlString;
        
        transformExprToSQLApplyer = new TransformExprToSQLApplyer(nodeRelation);
        sqlString = null;
        
        // start converting expr to sql
        expr.visit(transformExprToSQLApplyer);

        sqlString = transformExprToSQLApplyer.result(); 
        
        return sqlString;
    }
}
