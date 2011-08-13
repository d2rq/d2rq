package de.fuberlin.wiwiss.d2rq.optimizer.transformer;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.engine.OpD2RQ;

/**
 * Removes empty OpUnion leafs.
 * 
 * This Transformer prunes empty <code>OpUnion</code>s leaf nodes from an <code>Op</code> tree.
 * 
 * This is only needed when trying to log the <code>Op</code> tree, which caused <code>StackOverflowException</code>s
 * when the tree was too deeply nested.
 *  
 * @author G. Mels
 */
public class TransformRemoveEmptyUnion extends TransformCopy {

	private static boolean isNull(Op op)
	{
		return op instanceof OpNull || op instanceof OpD2RQ && ((OpD2RQ) op).getRelation() == Relation.EMPTY; 
	}
	
	/**
	 * A &cup; &empty; = &empty; &cup; A = A.
	 */
	public Op transform(OpUnion opUnion, Op left, Op right)
	{
		boolean leftNull  = isNull(left);
		boolean rightNull = isNull(right);
		
		if (leftNull && rightNull)
			return OpNull.create();
		
		if (leftNull)
			return right;
		
		if (rightNull)
			return left;
		
		return super.transform(opUnion, left, right);
	}
	
}
