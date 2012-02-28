package de.fuberlin.wiwiss.d2rq.optimizer.transformer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.Transform;
import com.hp.hpl.jena.sparql.algebra.op.Op0;
import com.hp.hpl.jena.sparql.algebra.op.Op1;
import com.hp.hpl.jena.sparql.algebra.op.Op2;
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
import com.hp.hpl.jena.sparql.algebra.op.OpN;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpOrder;
import com.hp.hpl.jena.sparql.algebra.op.OpPath;
import com.hp.hpl.jena.sparql.algebra.op.OpProcedure;
import com.hp.hpl.jena.sparql.algebra.op.OpProject;
import com.hp.hpl.jena.sparql.algebra.op.OpPropFunc;
import com.hp.hpl.jena.sparql.algebra.op.OpQuad;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.algebra.op.OpReduced;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.algebra.op.OpService;
import com.hp.hpl.jena.sparql.algebra.op.OpSlice;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.algebra.op.OpTopN;
import com.hp.hpl.jena.sparql.algebra.op.OpTriple;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.expr.ExprList;

import de.fuberlin.wiwiss.d2rq.optimizer.utility.OpFilterUtility;

/**
 * Creates a new operator-tree and adds for every operator the an OpLabel, 
 * that contains the threated object-vars of this operator (includes also the 
 * object-vars of sub-operators)
 * Additionaly it performs optimizing for filters - checks if tranformation to cnf is better
 * 
 * @author Herwig Leimer
 *
 */
public class TransformPrepareOpTreeForOptimizing implements Transform 
{
	
	/**
	 * Constructor
	 */
	public TransformPrepareOpTreeForOptimizing() 
	{
	}	
	
	public Op transform(OpFilter opFilter, Op subOp)                    
	{ 
		Op newOpFilter;
		ExprList cnfExprList, exprList;
		
		// optimize filter
		// transform to cnf
		cnfExprList = OpFilterUtility.translateFilterExpressionsToCNF(opFilter);
		cnfExprList = ExprList.splitConjunction(cnfExprList);
		
		// get expressions
		exprList = opFilter.getExprs();
		exprList = ExprList.splitConjunction(exprList);
        
		// now check if transformation to cnf is better
		if (cnfExprList.size() > exprList.size())
		{
			newOpFilter = OpFilter.filter(cnfExprList, subOp);
		}else
		{
			// not better
			newOpFilter = OpFilter.filter(exprList, subOp);
		}
		
		// add label with vars for filter
		return addLabelToOp1((Op1)newOpFilter);
	}
	
	public Op transform(OpBGP opBGP)                                
	{ 
    	Set threatedVars;
    	OpLabel opLabel;
    	BasicPattern basicPattern;
		Triple triple;
		Node object;
		OpBGP newOpBGP;
		
		// contains the threated object-vars of the BGP
		threatedVars = new HashSet();
		
		if ((basicPattern = opBGP.getPattern()) != null)
		{
			// extract object-vars from tripple-pattern
	        for (Iterator iter = basicPattern.iterator() ; iter.hasNext() ; )
	        {
	            triple = (Triple)iter.next();
	            object = triple.getObject();
	            
	            if (object != null && object.isVariable())
	            {
	            	threatedVars.add(object);
	            }            
	        }
		}
        
        // copy BGP
        newOpBGP = (OpBGP) opBGP.copy();
        
        // add label
        opLabel = (OpLabel) OpLabel.create(threatedVars, newOpBGP);
   	 	
        return opLabel;
	}


	public Op transform(OpJoin opJoin, Op left, Op right) 
	{
		Op newJoinOp;
		
		// copy join
		newJoinOp = opJoin.copy(left, right);
		
		// add label to join
		return addLabelToOp2((Op2)newJoinOp);		
	}

	public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) 
	{
		Op newOpLeftJoin;
		
		// copy leftjoin
		newOpLeftJoin = opLeftJoin.copy(left, right);
		
		// add label
		return addLabelToOp2((Op2)newOpLeftJoin);
	}

	public Op transform(OpProject opProject, Op subOp) 
	{
		Op newOpProject;
		
		// copy project
		newOpProject = opProject.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpProject);
	}

	public Op transform(OpTable opTable) 
	{
		Op newOpTable;
		
		// copy optable
		newOpTable = (OpTable) opTable.copy();
		
		// add label
		return addLabelToOp0((Op0)newOpTable);
	}

	public Op transform(OpUnion opUnion, Op left, Op right) 
	{
		Op newOpUnion;
		
		// copy opunion
		newOpUnion = opUnion.copy(left, right);
		
		// add label
		return addLabelToOp2((Op2)newOpUnion);
	}
		
	public Op transform(OpTriple opTriple) 
	{
		Op newOpTriple;
		Triple triple;
		Set threatedVars;
		OpLabel opLabel;
		Node object;
		
		// contains the threated object-vars of the Triple
		threatedVars = new HashSet();
		
		triple = opTriple.getTriple();
		
		if (triple != null)
		{
			// extract object-vars from tripple
			object = triple.getObject();
            
            if (object != null && object.isVariable())
            {
            	threatedVars.add(object);
            }	
		}
		
		// copy optriple
		newOpTriple = opTriple.copy();
		
		// add label
        opLabel = (OpLabel) OpLabel.create(threatedVars, newOpTriple);
		
		// add label
		return opLabel;
	}

	public Op transform(OpPath opPath) 
	{
		Op newOpPath;
		TriplePath triplePath;
		Node object;
		Set threatedVars;
		OpLabel opLabel;
		
		// contains the threated object-vars of the tripplepath
		threatedVars = new HashSet();
		
		triplePath = opPath.getTriplePath();
		
		if (triplePath != null)
		{
			// extract object-vars from tripplepath
			object = triplePath.getObject();
			
			if (object != null && object.isVariable())
            {
            	threatedVars.add(object);
            }			
		}
		// copy oppath
		newOpPath = opPath.copy();
		
		// add label
        opLabel = (OpLabel) OpLabel.create(threatedVars, newOpPath);
        
		return opLabel;
	}

	public Op transform(OpDatasetNames dsNames) 
	{
		Op newDsNames;
		
		// copy opdsnames
		newDsNames = dsNames.copy();
		
		// add label
		return addLabelToOp0((Op0)newDsNames);
	}

	public Op transform(OpQuadPattern quadPattern) 
	{
		Op newQuadPattern;
		Set threatedVars;
    	BasicPattern basicPattern;
		OpLabel opLabel;
		Triple triple;
		Node object;
		
		// contains the threated object-vars of the BGP
		threatedVars = new HashSet();
		
		if ((basicPattern = quadPattern.getBasicPattern()) != null)
		{
			// extract object-vars from tripple-pattern
	        for (Iterator iter = basicPattern.iterator() ; iter.hasNext() ; )
	        {
	            triple = (Triple)iter.next();
	            object = triple.getObject();
	            
	            if (object != null && object.isVariable())
	            {
	            	threatedVars.add(object);
	            }            
	        }
		}
        
		// copy opquadpattern
		newQuadPattern = quadPattern.copy();
        
        // add label
        opLabel = (OpLabel) OpLabel.create(threatedVars, newQuadPattern);
   	 	
        return opLabel;
	}

	public Op transform(OpNull opNull) 
	{
		Op newOpNull;
		
		// copy opnull
		newOpNull = opNull.copy();
		
		// add label
		return addLabelToOp0((Op0)newOpNull);
	}

	public Op transform(OpExt opExt) 
	{
		// copy of OpExt currently not possible
		throw new RuntimeException("Processing OpExt not implemented.");
	}

	public Op transform(OpGraph opGraph, Op subOp) 
	{
		Op newOpGraph;
		
		// copy opgraph
		newOpGraph = (OpGraph) opGraph.copy(subOp);

		// add label
		return addLabelToOp1((Op1)newOpGraph);
	}

	public Op transform(OpService opService, Op subOp) 
	{
		Op newOpService;
		
		// copy opservice
		newOpService = opService.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpService);
	}

	public Op transform(OpProcedure opProcedure, Op subOp) 
	{
		Op newOpProcedure;
		
		// copy opprocedure
		newOpProcedure = opProcedure.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpProcedure);
	}

	public Op transform(OpPropFunc opPropFunc, Op subOp) 
	{
		Op newOpPropFunc;
		
		// copy opPropFunc
		newOpPropFunc = opPropFunc.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpPropFunc);
	}

	public Op transform(OpLabel opLabel, Op subOp) 
	{
		Op newOpLabel;
		
		// copy oplabel
		newOpLabel = opLabel.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpLabel);
	}

	public Op transform(OpSequence opSequence, List<Op> elts) 
	{
		Op newOpSequence;
		
		// copy opsequence
		newOpSequence = opSequence.copy(elts);
		
		// add label
		return addLabelToOpN((OpN)newOpSequence);
	}

	public Op transform(OpList opList, Op subOp) 
	{
		Op newOpList;
		
		// copy opList
		newOpList = (OpList) opList.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpList);
	}

	public Op transform(OpOrder opOrder, Op subOp) 
	{
		Op newOpOrder;
		
		// copy oporder
		newOpOrder = opOrder.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpOrder);
	}

	public Op transform(OpAssign opAssign, Op subOp) 
	{
		Op newOpAssign;
		
		// copy opassign
		newOpAssign = opAssign.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpAssign);
	}

	public Op transform(OpDistinct opDistinct, Op subOp) 
	{
		Op newOpDistinct;
		
		// copy opdistinct
		newOpDistinct = opDistinct.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpDistinct);
	}

	public Op transform(OpReduced opReduced, Op subOp) 
	{
		Op newOpReduced;
		
		// copy opreduced
		newOpReduced = opReduced.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpReduced);
	}

	public Op transform(OpSlice opSlice, Op subOp) 
	{
		Op newOpSlice;
		
		// copy opslice
		newOpSlice = opSlice.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpSlice);
	}

	public Op transform(OpGroup opGroup, Op subOp) 
	{
		Op newOpGroup;
		
		// copy opgroupagg
		newOpGroup = opGroup.copy(subOp);
		
		// add label
		return addLabelToOp1((Op1)newOpGroup);
	}

	public Op transform(OpDiff opDiff, Op left, Op right) 
	{
		Op newOpDiff;
		
		// copy opdiff
		newOpDiff = opDiff.copy(left, right);
		
		// add label
		return addLabelToOp2((Op2)newOpDiff);
	}
	
	public Op transform(OpConditional opCondition, Op left, Op right) {
		Op newOpCond;
		
		// copy opcond
		newOpCond = opCondition.copy(left, right);

		// add label
		return addLabelToOp2((Op2)newOpCond);
	}
	
	@Override
	public Op transform(OpExtend opExtend, Op subOp) {
		return addLabelToOp1((Op1) opExtend.copy(subOp));
	}

	@Override
	public Op transform(OpMinus opMinus, Op left, Op right) {
		return addLabelToOp2((Op2) opMinus.copy(left, right));
	}

	@Override
	public Op transform(OpDisjunction opDisjunction, List<Op> elts) {
		return addLabelToOpN((OpN) opDisjunction.copy(elts));
	}

	@Override
	public Op transform(OpTopN opTop, Op subOp) {
		return addLabelToOp1((Op1) opTop.copy(subOp));
	}

	@Override
	public Op transform(OpQuad opQuad) {
		return addLabelToOp0((Op0) opQuad.copy());
	}

	/**
	 * Method for adding an oplabel to an op0. The oplabel
	 * contains the threated object-vars of the op0. Only if the 
	 * op0 is an OpBGP, it will contain the object-vars, otherwise, the set will be empty. 
	 * @param op0 - operator that should be wrapped by the oplabel
	 * @return Op - returns an OpLabel with the threated object-vars
	 */
	private Op addLabelToOp0(Op0 op0)
	{
		Set threatedVars;
		
		threatedVars = new HashSet();
		
		// add the label
		return OpLabel.create(threatedVars, op0);
	}
	
	/**
	 * Adds an oplabel to an op1. The oplabel
	 * contains the threated object-vars of the op0.
	 * @param op1 - operator that should be wrapped by the oplabel
	 * @return Op - returns an OpLabel with the threated object-vars
	 */
	private Op addLabelToOp1(Op1 op1)
	{
		Set threatedVars;
		Op opLabel, subOp;
		
		// contains all threated object-vars
		threatedVars = new HashSet();
		
		subOp = op1.getSubOp();
		
		// collect the object-vars, threated from the sub-operator
		if (subOp != null && subOp instanceof OpLabel)
		{
			threatedVars.addAll((Set)((OpLabel)subOp).getObject());			
		}
		
		// create the new label for the op1
		opLabel = OpLabel.create(threatedVars, op1);
		
		return opLabel;
	}
	
	/**
	 * Method for adding an oplabel to an op2. The oplabel
	 * contains the threated object-vars of the op2.
	 * @param op2 - operator that should be wrapped by the oplabel
	 * @return Op - returns an OpLabel with the threated object-vars  
	 * 
	 */
	private Op addLabelToOp2(Op2 op2)
	{
		Op left, right;
		Set threatedVars;
		
		// contains all threated object-vars
		threatedVars = new HashSet();
		
		left = op2.getLeft();
		right = op2.getRight();
		
		// collect the object-vars, threated from the left-operator
		if (left != null && left instanceof OpLabel)
		{
			threatedVars.addAll((Set)((OpLabel)left).getObject());
		}

		// collect the object-vars, threated from the right-operator
		if (right != null && right instanceof OpLabel)
		{
			threatedVars.addAll((Set)((OpLabel)right).getObject());
		}
		
		// create the new label for the op2
		return OpLabel.create(threatedVars, op2);
	}
	
	/**
	 * Method for adding an oplabel to an op2. The oplabel
	 * contains the threated object-vars of the op2.
	 * @param opN - operator that should be wrapped by the oplabel
	 * @return Op - returns an OpLabel with the threated object-vars  
	 * 
	 */
	private Op addLabelToOpN(OpN opN)
	{
		Op op;
		Set threatedVars;
		
		// contains all threated object-vars
		threatedVars = new HashSet();
		
		// collect the object-vars, threated from all the sub-operators
		for(Iterator iterator = ((OpN)opN).iterator(); iterator.hasNext();)
		{
			op = (Op) iterator.next();
			
			if (op instanceof OpLabel)
			{
				threatedVars.addAll((Set)((OpLabel)op).getObject());	
			}
			
		}
		
		// create label
		return OpLabel.create(threatedVars, opN);
	}
		
	/**
	 * Method for adding an oplabel to an opext. The oplabel
	 * contains the threated object-vars of the opext.  
	 * @param opExt - operator that should be wrapped by the oplabel
	 * @return Op - returns an OpLabel with the threated object-vars
	 */
	private Op addLabelToOpExt(OpExt opExt)
	{
		Set threatedVars;
		
		threatedVars = new HashSet();
		
		// add the label
		return OpLabel.create(threatedVars, opExt);
	}
}
