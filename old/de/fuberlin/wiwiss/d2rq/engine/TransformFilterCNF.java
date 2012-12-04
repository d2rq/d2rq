package de.fuberlin.wiwiss.d2rq.engine;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.expr.E_LogicalAnd;
import com.hp.hpl.jena.sparql.expr.E_LogicalNot;
import com.hp.hpl.jena.sparql.expr.E_LogicalOr;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprAggregator;
import com.hp.hpl.jena.sparql.expr.ExprFunction0;
import com.hp.hpl.jena.sparql.expr.ExprFunction1;
import com.hp.hpl.jena.sparql.expr.ExprFunction2;
import com.hp.hpl.jena.sparql.expr.ExprFunction3;
import com.hp.hpl.jena.sparql.expr.ExprFunctionN;
import com.hp.hpl.jena.sparql.expr.ExprFunctionOp;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.ExprNode;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.ExprVisitor;
import com.hp.hpl.jena.sparql.expr.NodeValue;

/**
 * Checks if any {@link OpFilter} can be split into more
 * parts by translating it to Conjunctive Normal Form (CNF).
 * 
 * @author Herwig Leimer
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TransformFilterCNF extends TransformCopy {
	
	public Op transform(OpFilter opFilter, Op subOp) { 
		ExprList exprList = ExprList.splitConjunction(opFilter.getExprs());
		ExprList cnfExprList = ExprList.splitConjunction(
				TransformFilterCNF.translateFilterExpressionsToCNF(opFilter));
		if (cnfExprList.size() > exprList.size()) {
			return OpFilter.filter(cnfExprList, subOp);
		}
		return OpFilter.filter(exprList, subOp);
	}

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
		OpFilter copiedOpFilter;
		
		newExprList = new ExprList();	// contains the translated expressions
		exprList = opFilter.getExprs();
		
		// workaround to avoid deep copy from the expressions
		// because deep copy is not available in Q_UnaryNot.java
		copiedOpFilter = (OpFilter) OpFilter.filter(exprList, opFilter.getSubOp());
		exprList = copiedOpFilter.getExprs();
		
		// for every expression of the filter, apply first DeMorgan-law and then Distributive-law
		for (Expr expr: exprList) {
			if (expr instanceof ExprNode) { // only SPARQL expressions are handled (e.g. no RDQL SimpleNode)
	            expr = applyDeMorganLaw(expr); 			// !(a || b) will become !a && !b
	            expr = applyDistributiveLaw(expr);		// a || (b && c) will become (a || b) && (a || c)
			}
	        newExprList.add(expr);
        }
		
		// split ever expression that contains conjunctions
		// a term like (a > 3) && (b > 4) will become to the two terms: (a > 3), (b > 4)
		newExprList = ExprList.splitConjunction(newExprList);
		
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
	private static Expr applyDistributiveLaw(Expr expr)
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
	 * Visitor for a filter-expression. Visits every expression-node of the expression-tree
	 * and applies the DeMorgan-law: !(a || b) will become !a && !b
	 * 
	 * @author Herwig Leimer
	 *
	 */
	public static class DeMorganLawApplyer implements ExprVisitor 
	{
		private Expr resultExpr;
		
		/**
		 * Constructor
		 */
		public DeMorganLawApplyer()
		{		
		}

		public void finishVisit() 
		{
		}

		public void startVisit() 
		{
		}

		public void visit(NodeValue nv) 
		{
			this.resultExpr = nv;
		}

		public void visit(ExprVar nv) 
		{
			this.resultExpr = nv;
		}

		public void visit(ExprFunction0 func) {
			this.resultExpr = func;
		}

		public void visit(ExprFunction1 curExpr) {
			Expr subExpr, leftExpr, rightExpr;
			Expr newAndExpr;

			subExpr = (curExpr).getArg();
			
			if (curExpr instanceof E_LogicalNot)
			{
				// !!a ==> a
				if (subExpr instanceof E_LogicalNot) {
					this.resultExpr = ((ExprFunction1) subExpr).getArg();
				}
				
				// apply DeMorgan
				// !(a || b) ==> !a && !b
				    // AndyL: don't change !(a && b) to !a || !b (no benefit!) 
				else if (subExpr instanceof E_LogicalOr )
				{
					// step down
					leftExpr = ((ExprFunction2)subExpr).getArg1();
					leftExpr.visit(this);
					leftExpr = this.resultExpr;

					// step down
					rightExpr = ((ExprFunction2)subExpr).getArg2();
					rightExpr.visit(this);
					rightExpr = this.resultExpr;
					
					
					if (!(leftExpr instanceof E_LogicalNot))
					{
						// add not
						leftExpr = new E_LogicalNot(leftExpr);	
					}else
					{
						// remove not
						leftExpr = ((E_LogicalNot)leftExpr).getArg();
					}
					
					if (!(rightExpr instanceof E_LogicalNot))
					{
						// add not
						rightExpr = new E_LogicalNot(rightExpr);
					}else
					{
						// remove not
						rightExpr = ((E_LogicalNot)rightExpr).getArg();
					}
					
//					// change operator
//					if (subExpr instanceof E_LogicalAnd)
//					{
//						newOrExpr = new E_LogicalOr(leftExpr, rightExpr);
//						this.resultExpr = newOrExpr;
//						
//					}else if (subExpr instanceof E_LogicalOr)
//					{
						newAndExpr = new E_LogicalAnd(leftExpr, rightExpr);
						this.resultExpr = newAndExpr;
//					}
				
				}else
				{
					this.resultExpr = curExpr;
				}
			}else
			{
				// step down
				subExpr.visit(this);	
				this.resultExpr = curExpr;
			}
		}

		public void visit(ExprFunction2 curExpr) {
			Expr leftExpr, rightExpr;

			if (curExpr instanceof E_LogicalOr || curExpr instanceof E_LogicalAnd)
			{
				// step down
				leftExpr = ((ExprFunction2)curExpr).getArg1();
				leftExpr.visit(this);
				leftExpr = this.resultExpr;

				// step down
				rightExpr = ((ExprFunction2)curExpr).getArg2();
				rightExpr.visit(this);
				rightExpr = this.resultExpr;
				
				// create new And/Or with resultExpr
				if (curExpr instanceof E_LogicalOr)
				{
					this.resultExpr = new E_LogicalOr(leftExpr, rightExpr);
				}else if (curExpr instanceof E_LogicalAnd)
				{
					this.resultExpr = new E_LogicalAnd(leftExpr, rightExpr);
				}
							
			}else
			{
				this.resultExpr = curExpr;
			}
		}

		public void visit(ExprFunction3 func) {
			this.resultExpr = func;
		}

		public void visit(ExprFunctionN func) {
			this.resultExpr = func;
		}

		public void visit(ExprFunctionOp funcOp) {
			this.resultExpr = funcOp;
		}

		public void visit(ExprAggregator eAgg) {
			this.resultExpr = eAgg;
		}
		
		public Expr result()
	    { 
	        return resultExpr; 
	    }
	}
	/**
	 * Visitor for a filter-expression. Visits every expression-node of the expression-tree
	 * and applies the Distributive-law: a || (b && c) will become (a || b) && (a || c)
	 * 
	 * @author Herwig Leimer
	 *
	 */
	public static class DistributiveLawApplyer implements ExprVisitor 
	{
		private Expr resultExpr;
		
		/**
		 * Constructor
		 */
		public DistributiveLawApplyer()
		{		
		}

		public void finishVisit() 
		{
		}

		public void startVisit() 
		{
		}

		public void visit(NodeValue nv) 
		{
			this.resultExpr = nv;
		}

		public void visit(ExprVar nv) 
		{
			this.resultExpr = nv;
		}

		public void visit(ExprFunction0 func) {
			this.resultExpr = func;
		}

		public void visit(ExprFunction1 curExpr) {
			Expr subExpr;
			if (curExpr instanceof E_LogicalNot)
			{
				subExpr = curExpr;
				// step down
				this.resultExpr = curExpr;
				((ExprFunction1) subExpr).getArg().visit(this);
				this.resultExpr = new E_LogicalNot(this.resultExpr);
			} else {
				this.resultExpr = curExpr;
			}
		}

		public void visit(ExprFunction2 curExpr) {
			Expr leftExpr, rightExpr;
			Expr leftLeftExpr, rightLeftExpr, leftRightExpr, rightRightExpr;
			Expr newAndExpr, newOrExpr1, newOrExpr2;

			if (curExpr instanceof E_LogicalOr || curExpr instanceof E_LogicalAnd)
			{
				// step down
				leftExpr = curExpr.getArg1();
				leftExpr.visit(this);
				leftExpr = this.resultExpr;

				// step down
				rightExpr = curExpr.getArg2();
				rightExpr.visit(this);
				rightExpr = this.resultExpr;
				
				if (curExpr instanceof E_LogicalOr)
				{
					if (!(leftExpr instanceof E_LogicalAnd) && !(rightExpr instanceof E_LogicalAnd))
					{
						// no distributive law - normal or
						this.resultExpr = new E_LogicalOr(leftExpr, rightExpr);
					}else
					{
						// criterion for distributive law - first OR, then AND
						
						if (leftExpr instanceof E_LogicalAnd)
						{
							leftLeftExpr = ((E_LogicalAnd)leftExpr).getArg1();
							rightLeftExpr = ((E_LogicalAnd)leftExpr).getArg2();
							
							// OR AND will become to AND OR OR
							newOrExpr1 = new E_LogicalOr(leftLeftExpr, rightExpr);
							newOrExpr2 = new E_LogicalOr(rightLeftExpr, rightExpr);
							newAndExpr = new E_LogicalAnd(newOrExpr1, newOrExpr2);
							
							this.resultExpr = newAndExpr;
							// apply for subexpression again
							newAndExpr.visit(this);
						}
						
						if (rightExpr instanceof E_LogicalAnd)
						{
							leftRightExpr = ((E_LogicalAnd)rightExpr).getArg1();
							rightRightExpr = ((E_LogicalAnd)rightExpr).getArg2();
						
							// OR AND will become to AND OR OR
							newOrExpr1 = new E_LogicalOr(leftExpr, leftRightExpr);
							newOrExpr2 = new E_LogicalOr(leftExpr, rightRightExpr);
							newAndExpr = new E_LogicalAnd(newOrExpr1, newOrExpr2);
						
							this.resultExpr = newAndExpr;
							// apply for subexpression again
							newAndExpr.visit(this);
						}
					}					
				}else
				{
					// E_LogicalAnd
					this.resultExpr = new E_LogicalAnd(leftExpr, rightExpr);
				}				
			}else
			{
				this.resultExpr = curExpr;
			}
		}

		public void visit(ExprFunction3 func) {
			this.resultExpr = func;
		}

		public void visit(ExprFunctionN func) {
			this.resultExpr = func;
		}

		public void visit(ExprFunctionOp funcOp) {
			this.resultExpr = funcOp;
		}

		public void visit(ExprAggregator eAgg) {
			this.resultExpr = eAgg;
		}

		public Expr result()
		{ 
			return resultExpr; 
		}
	}
}
