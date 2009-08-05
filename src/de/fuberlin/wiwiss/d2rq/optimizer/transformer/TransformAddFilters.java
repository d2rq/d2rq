package de.fuberlin.wiwiss.d2rq.optimizer.transformer;

import java.util.List;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpAssign;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpDatasetNames;
import com.hp.hpl.jena.sparql.algebra.op.OpDiff;
import com.hp.hpl.jena.sparql.algebra.op.OpDistinct;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpGraph;
import com.hp.hpl.jena.sparql.algebra.op.OpGroupAgg;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpLabel;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpList;
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
import com.hp.hpl.jena.sparql.algebra.op.OpTriple;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.core.VarExprList;

/**
 * Adds needed filters.
 * 
 * @author Herwig Leimer
 * 
 */
public class TransformAddFilters extends TransformCopy
{
	
	/**
	 * Constructor
	 */
	public TransformAddFilters() 
	{
	}
	
	public Op transform(OpUnion opUnion, Op left, Op right)         
	{  
		OpUnion newOpUnion;
		
		newOpUnion = new OpUnion(left, right);
	    return OpFilter.filter(newOpUnion);
	}
	
	public Op transform(OpJoin opJoin, Op left, Op right)
	{
		OpJoin newOpJoin;
		
		newOpJoin = (OpJoin) OpJoin.create(left, right);
	    return OpFilter.filter(newOpJoin);        
	}
    
    public Op transform(OpTable opUnit)
    {
        return OpFilter.filter(opUnit);
    }

    public Op transform(OpTriple opTriple)
    {
        return OpFilter.filter(opTriple);
    }

    public Op transform(OpPath opPath)
    {
        return OpFilter.filter(opPath);
    }

    public Op transform(OpDatasetNames dsNames)
    {
        return OpFilter.filter(dsNames);
    }

    public Op transform(OpQuadPattern quadPattern)
    {
        return OpFilter.filter(quadPattern);
    }

    public Op transform(OpNull opNull)
    {
        return OpFilter.filter(opNull);
    }

    public Op transform(OpExt opExt)
    {
        return OpFilter.filter(opExt);
    }

    public Op transform(OpGraph opGraph, Op subOp)
    {
    	// TODO: how to create new opGraph ?
        return OpFilter.filter(opGraph);
    }

    public Op transform(OpService opService, Op subOp)
    {
    	OpService newOpService;
    	
    	newOpService = new OpService(opService.getService(), subOp);
        return OpFilter.filter(newOpService);
    }

    public Op transform(OpProcedure opProcedure, Op subOp)
    {
    	OpProcedure newOpProcedure;
    	
    	newOpProcedure = new OpProcedure(opProcedure.getProcId(), opProcedure.getArgs(), subOp);
        return OpFilter.filter(newOpProcedure);
    }

    public Op transform(OpPropFunc opPropFunc, Op subOp)
    {
    	OpPropFunc newOpPropFunc;
    	
    	newOpPropFunc = new OpPropFunc(opPropFunc.getProperty(), opPropFunc.getSubjectArgs(), opPropFunc.getObjectArgs(), subOp);
        return OpFilter.filter(newOpPropFunc);
    }

    public Op transform(OpLabel opLabel, Op subOp)
    {
        // remove all labels
        return subOp;
    }

    public Op transform(OpSequence opSequence, List elts)
    {
        return OpFilter.filter(opSequence);
    }

    public Op transform(OpList opList, Op subOp)
    {
    	OpList newOpList;
    	
    	newOpList = new OpList(subOp);
        return OpFilter.filter(newOpList);
    }

    public Op transform(OpOrder opOrder, Op subOp)
    {
    	OpOrder newOpOrder;
    	
    	newOpOrder = new OpOrder(subOp, opOrder.getConditions());
        return OpFilter.filter(newOpOrder);
    }

    public Op transform(OpProject opProject, Op subOp)
    {
    	OpProject newOpProject;
    	
    	newOpProject = new OpProject(subOp, opProject.getVars());
        return OpFilter.filter(newOpProject);
    }

    public Op transform(OpAssign opAssign, Op subOp)
    {
    	Op newOpAssign;
    	
    	newOpAssign = OpAssign.assign(subOp, opAssign.getVarExprList());
        return OpFilter.filter(newOpAssign);
    }

    public Op transform(OpDistinct opDistinct, Op subOp)
    {
    	OpDistinct newOpDistinct;
    	
    	newOpDistinct = new OpDistinct(subOp);
        return OpFilter.filter(newOpDistinct);
    }

    public Op transform(OpReduced opReduced, Op subOp)
    {
    	OpReduced newOpReduced;
    	
    	newOpReduced = new OpReduced(subOp);
        return OpFilter.filter(newOpReduced);
    }

    public Op transform(OpSlice opSlice, Op subOp)
    {
    	OpSlice newOpSlice;
    	
    	newOpSlice = new OpSlice(subOp, opSlice.getStart(), opSlice.getLength());
        return OpFilter.filter(newOpSlice);
    }

    public Op transform(OpGroupAgg opGroupAgg, Op subOp)
    {
    	OpGroupAgg newOpGroupAgg;
    	
    	newOpGroupAgg = new OpGroupAgg(subOp, opGroupAgg.getGroupVars(), opGroupAgg.getAggregators());
        return OpFilter.filter(newOpGroupAgg);
    }

    public Op transform(OpLeftJoin opLeftJoin, Op left, Op right)
    {
    	OpLeftJoin newOpLeftJoin;
    	
    	newOpLeftJoin = (OpLeftJoin) OpLeftJoin.create(left, right, opLeftJoin.getExprs());
        return OpFilter.filter(newOpLeftJoin);
    }

    public Op transform(OpDiff opDiff, Op left, Op right)
    {
    	OpDiff newOpDiff;
    	
    	newOpDiff = (OpDiff) OpDiff.create(left, right);
        return OpFilter.filter(newOpDiff);
    }

    public Op transform(OpBGP opBGP)
    {
    	OpBGP newOpBGP;
    	
    	newOpBGP = new OpBGP(opBGP.getPattern());
        return OpFilter.filter(newOpBGP);
    }
    
}
