package de.fuberlin.wiwiss.d2rq.optimizer.ops;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.Op2;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.main.OpExtMain;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

public abstract class Op2MatD2RQ extends OpExtMain 
{
	protected final Op left ;
    protected final Op right ;
    protected final Op2 original;
    
    public Op2MatD2RQ(Op2 original, Op left, Op right)
    {
        this.original = original;
    	this.left = left; 
        this.right = right;
    }
	
    public abstract QueryIterator eval(QueryIterator input, ExecutionContext execCxt);

    public boolean equalTo(Op op2, NodeIsomorphismMap labelMap)
    {
		if (!(op2 instanceof Op2MatD2RQ)) return false;
		Op2MatD2RQ other = (Op2MatD2RQ) op2;
        return this.original.equalTo(other, labelMap);
    }

	public int hashCode() 
	{
		return original.getLeft().hashCode()<<1 ^ original.getRight().hashCode() ^ getName().hashCode() ;
	}

	public OpExt copy() 
	{
		return this;
	}

	public Op effectiveOp() 
	{
		return original;
	}

	public Op getLeft() 
	{ 
		return left; 
	}
	
    public Op getRight() 
    { 
    	return right; 
    }

	public Op2 getOriginal() 
	{
		return original;
	}
	
	public abstract String getName();
}
