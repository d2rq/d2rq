package de.fuberlin.wiwiss.d2rq.optimizer.ops;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRoot;
import com.hp.hpl.jena.sparql.engine.main.OpCompiler;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterJoin;

/**
 * Materialized join. Work-around to prevent a streamed evaluation of the join, in
 * the OpCompiler of ARQ.
 *  
 * @author Herwig Leimer
 *
 */
public class OpMatJoinD2RQ extends Op2MatD2RQ 
{
	private static final String NAME = "matjoin";
	
	private OpMatJoinD2RQ(OpJoin original, Op left, Op right)
	{
		super(original, left, right);
	}
	
	public QueryIterator eval(QueryIterator input, ExecutionContext execCxt) 
	{
		QueryIterator leftqIter, rightqIter, qIter;
		OpCompiler opCompiler;
		
		opCompiler = OpCompiler.factory.create(execCxt);
		
		leftqIter = opCompiler.compileOp(left, input) ;
		rightqIter = opCompiler.compileOp(right, QueryIterRoot.create(execCxt)) ;
        qIter = new QueryIterJoin(leftqIter, rightqIter, execCxt) ;
        return qIter ;
	}



	public String getName() 
	{
		return NAME;
	}
	
	public static Op create(OpJoin original, Op left, Op right)
    {
		return new OpMatJoinD2RQ(original, left, right);
    }

}
