package de.fuberlin.wiwiss.d2rq.optimizer.transformer;

import java.util.List;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformBase;
import com.hp.hpl.jena.sparql.algebra.op.OpAssign;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpConditional;
import com.hp.hpl.jena.sparql.algebra.op.OpDatasetNames;
import com.hp.hpl.jena.sparql.algebra.op.OpDiff;
import com.hp.hpl.jena.sparql.algebra.op.OpDisjunction;
import com.hp.hpl.jena.sparql.algebra.op.OpDistinct;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpExtend;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpGraph;
import com.hp.hpl.jena.sparql.algebra.op.OpGroup;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpLabel;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpList;
import com.hp.hpl.jena.sparql.algebra.op.OpMinus;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpOrder;
import com.hp.hpl.jena.sparql.algebra.op.OpPath;
import com.hp.hpl.jena.sparql.algebra.op.OpProcedure;
import com.hp.hpl.jena.sparql.algebra.op.OpProject;
import com.hp.hpl.jena.sparql.algebra.op.OpPropFunc;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.algebra.op.OpReduced;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.algebra.op.OpService;
import com.hp.hpl.jena.sparql.algebra.op.OpSlice;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.algebra.op.OpTopN;
import com.hp.hpl.jena.sparql.algebra.op.OpTriple;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;

import de.fuberlin.wiwiss.d2rq.optimizer.ops.OpFilteredBGP;

/**
 * TODO
 * 
 * @author Herwig Leimer
 *
 */
public class TransformApplyD2RQOpimizingRules extends TransformBase 
{

	/**
	 * Constructor
	 */
	public TransformApplyD2RQOpimizingRules() 
	{
	}	
	
	public Op transform(OpFilter opFilter, Op subOp)                    
	{
		return opFilter;
	}
	
	/**
     * Creates an OpFilteredBGP and the coresponding filter.  A OpFilteredBGP is nearly the same 
     * like an OpBGP but it has a link to its parent, which is an OpFilter. 
     * This is done, because in the transforming-process of the OpBGPs to OpD2RQs a 
     * link to the above OpFilter is needed. 
     */
	public Op transform(OpBGP opBGP)                                
	{ 
		Op newOp, filteredOpBGP;
		
    	// create a new OpFilteredBGP
    	filteredOpBGP = new OpFilteredBGP(opBGP.getPattern());
        // create a Filter with empty filterconditions and link them to the BGP
        newOp = OpFilter.filter(filteredOpBGP);
        ((OpFilteredBGP)filteredOpBGP).setParent(newOp);
        
        return newOp;
	}

	public Op transform(OpJoin opJoin, Op left, Op right) 
    {
		return opJoin;
	}

	public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) 
	{
		return opLeftJoin;
	}

	public Op transform(OpProject opProject, Op subOp) 
	{
		return opProject;
	}

	public Op transform(OpTable opTable) 
	{
		return opTable;
	}

	public Op transform(OpUnion opUnion, Op left, Op right) 
	{
		return opUnion;
	}
		
	public Op transform(OpTriple opTriple) 
	{
		return opTriple;
	}

	public Op transform(OpPath opPath) 
	{
		return opPath;
	}

	public Op transform(OpDatasetNames dsNames) 
	{
		return dsNames;
	}

	public Op transform(OpQuadPattern quadPattern) 
	{
		return quadPattern;
	}

	public Op transform(OpNull opNull) 
	{
		return opNull;
	}

	public Op transform(OpExt opExt) 
	{
		return opExt;
	}

	public Op transform(OpGraph opGraph, Op subOp) 
	{
		return opGraph;
	}

	public Op transform(OpService opService, Op subOp) 
	{
		return opService;
	}

	public Op transform(OpProcedure opProcedure, Op subOp) 
	{
		return opProcedure;
	}

	public Op transform(OpPropFunc opPropFunc, Op subOp) 
	{
		return opPropFunc;
	}

	public Op transform(OpLabel opLabel, Op subOp) 
	{
		return opLabel;
	}

	public Op transform(OpSequence opSequence, List<Op> elts) 
	{
		return opSequence;
	}

	public Op transform(OpList opList, Op subOp) 
	{
		return opList;
	}

	public Op transform(OpOrder opOrder, Op subOp) 
	{
		return opOrder;
	}

	public Op transform(OpAssign opAssign, Op subOp) 
	{
		return opAssign;
	}

	public Op transform(OpDistinct opDistinct, Op subOp) 
	{
		return opDistinct;
	}

	public Op transform(OpReduced opReduced, Op subOp) 
	{
		return opReduced;
	}

	public Op transform(OpSlice opSlice, Op subOp) 
	{
		return opSlice;
	}

	public Op transform(OpGroup opGroup, Op subOp) 
	{
		return opGroup;
	}

	public Op transform(OpDiff opDiff, Op left, Op right) 
	{
		return opDiff;
	}

	public Op transform(OpConditional opCondition, Op left, Op right)
	{
		return opCondition;
	}
}
