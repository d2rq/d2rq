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
 * and applies the Distributive-law: a || (b && c) will become (a || b) && (a || c)
 * 
 * @author Herwig Leimer
 *
 */
public final class DistributiveLawApplyer implements ExprVisitor 
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

	public void visit(ExprFunction curExpr) 
	{
		Expr subExpr, leftExpr, rightExpr;
		Expr leftLeftExpr, rightLeftExpr, leftRightExpr, rightRightExpr;
		Expr newAndExpr, newOrExpr1, newOrExpr2;
		
		if (curExpr instanceof E_LogicalNot)
		{
			subExpr = curExpr;
			// step down
			this.resultExpr = curExpr;
			((ExprFunction1) subExpr).getArg().visit(this);
			this.resultExpr = new E_LogicalNot(this.resultExpr);
			
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
		}else
		{
			this.resultExpr = curExpr; // TODO correct?
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
