package de.fuberlin.wiwiss.d2rq.optimizer.utility;

import com.hp.hpl.jena.sparql.expr.E_LogicalAnd;
import com.hp.hpl.jena.sparql.expr.E_LogicalNot;
import com.hp.hpl.jena.sparql.expr.E_LogicalOr;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprFunction;
import com.hp.hpl.jena.sparql.expr.ExprFunction1;
import com.hp.hpl.jena.sparql.expr.ExprFunction2;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.ExprVisitor;
import com.hp.hpl.jena.sparql.expr.NodeValue;

/**
 * Visitor for a filter-expression. Visits every expression-node of the expression-tree
 * and applies the DeMorgan-law: !(a || b) will become !a && !b
 * 
 * @author Herwig Leimer
 *
 */
public final class DeMorganLawApplyer implements ExprVisitor 
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

	public void visit(ExprFunction curExpr) 
	{
		Expr subExpr, leftExpr, rightExpr;
		Expr newAndExpr, newOrExpr;
		
		if (curExpr instanceof ExprFunction1)
		{
			subExpr = ((ExprFunction1)curExpr).getArg();
			
			if (curExpr instanceof E_LogicalNot)
			{
				// !!a ==> a
				if (subExpr instanceof E_LogicalNot) {
					this.resultExpr = ((ExprFunction1) subExpr).getArg();
				}
				
				// subexpr a AND or OR 
				// apply DeMorgan
				else if (subExpr instanceof E_LogicalAnd || subExpr instanceof E_LogicalOr)
				{
					// step down
					leftExpr = ((ExprFunction2)subExpr).getArg1();
					leftExpr.visit(this);
					leftExpr = this.resultExpr;
	
					// step down
					rightExpr = ((ExprFunction2)subExpr).getArg2();
					rightExpr.visit(this);
					rightExpr = this.resultExpr;
					
					
					// !(a || b) ==> !a && !b
					// !(a && b) ==> !a || !b
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
					
					// change operator
					if (subExpr instanceof E_LogicalAnd)
					{
						newOrExpr = new E_LogicalOr(leftExpr, rightExpr);
						this.resultExpr = newOrExpr;
						
					}else if (subExpr instanceof E_LogicalOr)
					{
						newAndExpr = new E_LogicalAnd(leftExpr, rightExpr);
						this.resultExpr = newAndExpr;
					}
				
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
						
		}else if (curExpr instanceof ExprFunction2)
		{
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
		}else
		{
			// ExpressionFunction like E_Regex or ExpressionFunctionN
			this.resultExpr = curExpr;
		}
	}

	public void visit(NodeValue nv) 
	{
		this.resultExpr = nv;
	}

	public void visit(ExprVar nv) 
	{
		this.resultExpr = nv;
	}

	
	public Expr result()
    { 
        return resultExpr; 
    }
}
