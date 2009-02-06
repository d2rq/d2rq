package de.fuberlin.wiwiss.d2rq.optimizer.ops;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRoot;
import com.hp.hpl.jena.sparql.engine.main.OpCompiler;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterLeftJoin;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;

/**
 * Materialized Leftjoin. Work-around to prevent a streamed evaluation of the leftjoin, in
 * the OpCompiler of ARQ.
 *  
 * @author Herwig Leimer
 *
 */
public class OpMatLeftJoinD2RQ extends Op2MatD2RQ 
{
	private static final String NAME = "matleftjoin";
	private ExprList expressions = null ;
	
	private OpMatLeftJoinD2RQ(OpLeftJoin original, Op left, Op right, ExprList exprs)
	{
		super(original, left, right);
		this.expressions = exprs;
	}

	private OpMatLeftJoinD2RQ(OpLeftJoin original, Op left, Op right, Expr expr)
	{
		super(original, left, right);
		this.expressions = expr == null ? null : new ExprList(expr);
	}
	
	public String getName() 
	{
		return NAME;
	}
	
	public static Op create(OpLeftJoin original, Op left, Op right, ExprList exprs)
    { 
        return new OpMatLeftJoinD2RQ(original, left, right, exprs) ;
    }
    
    public static Op create(OpLeftJoin original, Op left, Op right, Expr expr)
    { 
        return new OpMatLeftJoinD2RQ(original, left, right, expr);
    }

    public ExprList getExprs()      
    { 
    	return expressions ; 
    }

	public QueryIterator eval(QueryIterator input, ExecutionContext execCxt) 
	{
		QueryIterator leftqIter, rightqIter, qIter;
		OpCompiler opCompiler;
		
		opCompiler = OpCompiler.factory.create(execCxt);
		
		leftqIter = opCompiler.compileOp(left, input) ;
		rightqIter = opCompiler.compileOp(right, QueryIterRoot.create(execCxt)) ;
        qIter = new QueryIterLeftJoin(leftqIter, rightqIter, expressions, execCxt) ;
        return qIter ;
	} 
}
