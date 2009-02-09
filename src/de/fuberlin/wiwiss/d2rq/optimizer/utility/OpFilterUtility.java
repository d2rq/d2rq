package de.fuberlin.wiwiss.d2rq.optimizer.utility;

import java.util.Iterator;

import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.ExprNode;


/**
 * Utility-class for filters. Does some shaping of the filter-expressions. 
 * 
 * @author Herwig Leimer
 *
 */
public class OpFilterUtility 
{
	/**
	 * Translates all expressions of a filter to the conjunctive normalform.
	 * To get the conjunctive normalform of a term, first the law of DeMorgan and then
	 * the Distributive-law will be applied  
	 * @param opFilter - a Filter with some expressions
	 * @return ExprList - every entry of the exprlist is in conjunctive normalform
	 */
	public static ExprList translateFilterExpressionsToCNF(final OpFilter opFilter)
	{
		ExprList exprList, newExprList;
		Expr expr;
		OpFilter copiedOpFilter;
		
		newExprList = new ExprList();	// contains the translated expressions
		exprList = opFilter.getExprs();
		
		// workaround to avoid deep copy from the expressions
		// because deep copy is not available in Q_UnaryNot.java
		copiedOpFilter = (OpFilter) OpFilter.filter(exprList, opFilter.getSubOp());
		exprList = copiedOpFilter.getExprs();
		
		// for every expression of the filter, apply first DeMorgan-law and then Distributive-law
		for (Iterator iterator = exprList.iterator(); iterator.hasNext();)
        {
			expr = (Expr) iterator.next();
			if (expr instanceof ExprNode) { // only SPARQL expressions are handled (e.g. no RDQL SimpleNode)
	            expr = applyDeMorganLaw(expr); 			// !(a || b) will become !a && !b
	            expr = applyDistributivLaw(expr);		// a || (b && c) will become (a || b) && (a || c)
			}
	        newExprList.add(expr);
        }
		
		// split ever expression that contains conjunctions
		// a term like (a > 3) && (b > 4) will become to the two terms: (a > 3), (b > 4)
		newExprList = applySplitConjunctions(newExprList);
		
		return newExprList;
	}
	
	
	/**
	 * Method for appling the law of DeMorgan.
	 * @param expr - root-node of the current expression-tree
	 * @return Expr - the new expression
	 */
	private static Expr applyDeMorganLaw(Expr expr)
	{
		DeMorganLawApplyer deMorganLawApplyer = new DeMorganLawApplyer();
		
		// copy expression to maintain the original one
		//		newExpr = expr.deepCopy();		
		
		// apply the rule
		expr.visit(deMorganLawApplyer);
		expr = deMorganLawApplyer.result();
		
		return expr;
	}
	
	/**
	 * Method for appling the rules of the distributiv-law
	 * @param expr - root-node of the current expression-tree
	 * @return Expr - the new expression
	 */
	private static Expr applyDistributivLaw(Expr expr)
	{
		DistributiveLawApplyer distributiveLawApplyer = new DistributiveLawApplyer();
		
		// copy expression to maintain the original one
		//		newExpr = expr.deepCopy();		
		
		// apply the rule
		expr.visit(distributiveLawApplyer);
		expr = distributiveLawApplyer.result();
		
		return expr;
	}
	
	/**
	 * Method for spliting up all conjunctions where it is possible
	 * @param exprList - list with the expression
	 * @return - splitted up expressions
	 */
	private static ExprList applySplitConjunctions(ExprList exprList)
	{	
		return ExprList.splitConjunction(exprList);
	}
}
