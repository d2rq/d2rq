package de.fuberlin.wiwiss.d2rq.optimizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVisitor;
import com.hp.hpl.jena.sparql.algebra.Transform;
import com.hp.hpl.jena.sparql.algebra.Transformer;
import com.hp.hpl.jena.sparql.algebra.op.Op0;
import com.hp.hpl.jena.sparql.algebra.op.Op1;
import com.hp.hpl.jena.sparql.algebra.op.Op2;
import com.hp.hpl.jena.sparql.algebra.op.OpAssign;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpConditional;
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
import com.hp.hpl.jena.sparql.algebra.op.OpN;
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
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.util.ALog;
import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.optimizer.transformer.TransformAddFilters;
import de.fuberlin.wiwiss.d2rq.optimizer.transformer.TransformPrepareOpTreeForOptimizing;
import de.fuberlin.wiwiss.d2rq.optimizer.transformer.TransformApplyD2RQOpimizingRules;
import de.fuberlin.wiwiss.d2rq.optimizer.transformer.TransformD2RQ;
import de.fuberlin.wiwiss.d2rq.optimizer.utility.MutableIndex;

/**
 * Class for optimizing an op-tree especially for D2RQ. 
 *  
 * @author Herwig Leimer
 *
 */
public class D2RQTreeOptimizer
{
	/**
	 * Constructor
	 */
    private D2RQTreeOptimizer() 
    {    	
    }
    
    /**
     * Method for optimizing an operator-tree. 
     * In the first step for every operator in the tree, its threated object-vars
     * ale calculated, and put into a labelOp. This means the transformed-object-tree
     * looks like (originalop means the original operator): originalop -> OpLabel -> originalop -> OpLabel ....
     * This information is used for applying the rules for optimizing. 
     * In the second step, all the rules of optimizing (see above) will be applied. This is done to
     * move the Filter-Conditions as far as possible down in the tree. In the optimal way, the filterconditions
     * is the parent of an opBGP 
     * @param op
     * @return
     */
    public static Op optimize(Op op, GraphD2RQ graph)
    {
        Op transformedOp;
        
        // add OpLabels with the threated object-vars for every operator
        transformedOp = Transformer.transform(new TransformPrepareOpTreeForOptimizing(), op);
        // now apply the rules for optimizing the operator-tree, replaces the OpBGPs through
        // OpD2RQs and removes unneeded Ops like labels and filters with no conditions
        transformedOp = optimizeAndGenerate(transformedOp, graph);
        
        return transformedOp;
    }
    
    
    /**
     * Method for optimizing and transforming the operator-tree.
     * @param transform - Transformer for changing the operator-tree
     * @param op - root-node of the operator-tree
     * @return Op - the transformed operator-tree
     */
    private static Op optimizeAndGenerate(Op op, GraphD2RQ graph)
    {  
        if ( op == null )
        {
            ALog.warn(D2RQTreeOptimizer.class, "Attempt to transform a null Op - ignored") ;
            return op ;
        }
        
        // calculate all optional vars and fixed vars
        // this information is used in the relationstobindingsleftjoiniterator
        VarFinder varFinder = new VarFinder(op);
        
        // visitor that applies the rules for moving down the filterconditions
        D2RQOpTreeVisitor d2RQOpTreeVisitor = new D2RQOpTreeVisitor(graph, varFinder) ;
        // start visiting
        op.visit(d2RQOpTreeVisitor) ;
        Op r = d2RQOpTreeVisitor.result() ;
        return r ;
    }
    
    /**
     * Visitor for traversing the operator-tree, moving down the filterconditions
     * as far as possible, and for applying the transformer to change all OpBGPs to OpD2RQs 
     * The optimizing will be done during the top-down-stepping, the transformation during
     * the bottom-up-stepping
     * 
     * @author Herwig Leimer
     *
     */
    private static final class D2RQOpTreeVisitor implements OpVisitor
    {
        private Transform optimizingTransform, d2rqTransform, addFilterTransform;
        private Stack stack;
        private List filterExpr;
        
        /**
         * Constructor
         * @param transform - Transformer to be applied on the operator-tree
         */
        public D2RQOpTreeVisitor(GraphD2RQ graph, VarFinder varFinder)
        { 
        	this.filterExpr = new ArrayList();
        	this.optimizingTransform = new TransformApplyD2RQOpimizingRules();
            this.d2rqTransform = new TransformD2RQ(graph, new MutableIndex(), varFinder);
            this.addFilterTransform = new TransformAddFilters();
            this.stack = new Stack();
        }
        
        /**
         * Returns the changed operator-tree
         * @return Op - root-node of the operator-tree
         */
        public Op result()
        { 
            if ( stack.size() != 1 )
                ALog.warn(this, "Stack is not aligned") ;
            return (Op)this.stack.pop() ; 
        }
                        
        /**
         * When visiting an OpFilter, all its filterconditions are collected during the top-down-stepping.
         * During the bottom-up-stepping all filterconditions which were moved down, are removed
         */
        public void visit(OpFilter opFilter)
        { 
            Op newOp, subOp = null ;
            ExprList exprList;
            
            // get the filterconditions
            exprList = opFilter.getExprs();
            
            // add all the filterconditions to the condition-list
            this.filterExpr.addAll(exprList.getList());

            // step down
            if (opFilter.getSubOp() != null )
            {
                opFilter.getSubOp().visit(this);
                subOp = (Op)this.stack.pop();
            }
            
            // remove available filterexpressions
            opFilter.getExprs().getList().removeAll(this.filterExpr);

            // removes the filter if it has no expressions
            newOp = opFilter.apply(d2rqTransform, subOp);
            
            this.stack.push(newOp);
        }
        
        /**
         * When visiting an OpUnion also 3 conditions for moving down the filterconditions are
         * checked. Only when a condition is satisfied, the filtercondition can be moved
         * down to the next operator in the tree. Otherwise the condition will stay here 
         * and during the bottum-up-stepping an OpFilter containing these remained 
         * filterconditions will be inserted in the operator-tree.
         * Conditions for moving down a filtercondition:
         * (M1, M2 are graphpatterns, F is a filtercondition)
         * 1) Filter(Union(M1, M2), F)) will become Union(Filter(M1, F), M2) when the 
         *    filterexpression is only referenced to M1
         * 2) Filter(Union(M1, M2), F)) will become Union(M1, Filter(M2, F)) when the 
         *    filterexpression is only referenced to M2
         * 3) Filter(Union(M1, M2), F)) will become Join(Union(M1, F), Union(M2, F)) when the 
         *    filterexpression is referenced to M1 and M2
         */
        public void visit(OpUnion opUnion)
        { 
            checkMoveDownFilterExprAndVisitOpUnion(opUnion); 
        }
        
        /**
         * When visiting an OpJoin 3 conditions for moving down the filterconditions are
         * checked. Only when a condition is satisfied, the filtercondition can be moved
         * down to the next operator in the tree. Otherwise the condition will stay here 
         * and during the bottum-up-stepping an OpFilter containing these remained 
         * filterconditions will be inserted in the operator-tree.
         * Conditions for moving down a filtercondition:
         * (M1, M2 are graphpatterns, F is a filtercondition)
         * 1) Filter(Join(M1, M2), F)) will become Join(Filter(M1, F), M2) when the 
         *    filterexpression is only referenced to M1
         * 2) Filter(Join(M1, M2), F)) will become Join(M1, Filter(M2, F)) when the 
         *    filterexpression is only referenced to M2
         * 3) Filter(Join(M1, M2), F)) will become Join(Filter(M1, F), Filter(M2, F)) when the 
         *    filterexpression is referenced to M1 and M2
         */
        public void visit(OpJoin opJoin)
        { 
            checkMoveDownFilterExprAndVisitOpJoin(opJoin);
        }
                

        /**
         * When there are some filterexpressions which belong to an OpBGP, the OpBGP will be converted
         * to an OpFilteredBGP. A OpFilteredBGP is nearly the same like an OpBGP but it has
         * a link to its parent, which is an OpFilter with the coresponding filter-conditions, 
         * because in the transforming-process of the OpBGPs to OpD2RQs a link to the above OpFilter 
         * is needed. 
         */
        public void visit(OpBGP op)
        { 
        	Op newOp;
            
        	newOp = op;
        	
        	
        	// are there some filterexpression which belong to this BGP ? 
            if (!this.filterExpr.isEmpty())
            {
                // apply the optimizing rules
                // create a filteredOpBGP and a filter
                newOp = op.apply(optimizingTransform);
                // add the current valid filterconditions
                ((OpFilter)newOp).getExprs().getList().addAll(this.filterExpr);
            }
            
            if (newOp instanceof OpFilter)
            {
                // OpFilteredBGP must be transformed 
                newOp = ((OpFilter)newOp).getSubOp();
            }
            
            // apply the tranformation from an OpBGP/OpFilteredBGP to an OpD2RQ
            newOp = ((OpBGP)newOp).apply(d2rqTransform);

            this.stack.push(newOp);
        } 
           
        /**
         * When visiting an OpDiff 3 conditions for moving down the filterconditions are
         * checked. Only when a condition is satisfied, the filtercondition can be moved
         * down to the next operator in the tree. Otherwise the condition will stay here 
         * and during the bottum-up-stepping an OpFilter containing these remained 
         * filterconditions will be inserted in the operator-tree.
         * Conditions for moving down a filtercondition:
         * (M1, M2 are graphpatterns, F is a filtercondition)
         * 1) Filter(Diff(M1, M2), F)) will become Diff(Filter(M1, F), M2) when the 
         *    filterexpression is only referenced to M1
         * 2) Filter(Diff(M1, M2), F)) will become Diff(M1, Filter(M2, F)) when the 
         *    filterexpression is only referenced to M2
         * 3) Filter(Diff(M1, M2), F)) will become Diff(Filter(M1, F), Filter(M2, F)) when the 
         *    filterexpression is referenced to M1 and M2
         */
        public void visit(OpDiff opDiff)
        { 
            // TODO: Regel nochmal ueberdenken !!!
        	checkMoveDownFilterExprAndVisitOpDiff(opDiff);
        }
        
        public void visit(OpConditional opCondition) {
			notMoveDownFilterExprAndVisitOp2(opCondition); //TODO moving down / improvements possible!! Check this
		}
        
        public void visit(OpProcedure opProc)
        { 
            notMoveDownFilterExprAndVisitOp1(opProc); 
        }
        
        public void visit(OpPropFunc opPropFunc)
        { 
            notMoveDownFilterExprAndVisitOp1(opPropFunc); 
        }        

        public void visit(OpTable opTable)
        { 
            notMoveDownFilterExprAndVisitOp0(opTable); 
        }
        
        public void visit(OpQuadPattern quadPattern)
        { 
            notMoveDownFilterExprAndVisitOp0(quadPattern);
        }

        public void visit(OpPath opPath)
        { 
            notMoveDownFilterExprAndVisitOp0(opPath); 
        }
        
        public void visit(OpTriple opTriple)
        { 
            notMoveDownFilterExprAndVisitOp0(opTriple); 
        }
        
        public void visit(OpDatasetNames dsNames)
        { 
            notMoveDownFilterExprAndVisitOp0(dsNames); 
        }
        
        public void visit(OpSequence opSequence)
        { 
            notMoveDownFilterExprAndVisitOpN(opSequence); 
        }
        
        /**
         * When visiting an OpJoin 2 conditions for moving down the filterconditions are
         * checked. Only when a condition is satisfied, the filtercondition can be moved
         * down to the next operator in the tree. Otherwise the condition will stay here 
         * and during the bottum-up-stepping an OpFilter containing these remained 
         * filterconditions will be inserted in the operator-tree.
         * Conditions for moving down a filtercondition:
         * (M1, M2 are graphpatterns, F is a filtercondition)
         * 1) Filter(LeftJoin(M1, M2), F)) will become LeftJoin(Filter(M1, F), M2) when the 
         *    filterexpression is only referenced to M1
         * 2) Filter(LeftJoin(M1, M2), F)) will become LeftJoin(Filter(M1, F), Filter(M2, F)) when the 
         *    filterexpression is referenced to M1 and M2
         */
        public void visit(OpLeftJoin opLeftJoin)
        { 
        	checkMoveDownFilterExprAndVisitOpLeftJoin(opLeftJoin); 
        }

        public void visit(OpGraph opGraph)
        { 
            notMoveDownFilterExprAndVisitOp1(opGraph); 
        }

        public void visit(OpService opService)
        { 
            notMoveDownFilterExprAndVisitOp1(opService); 
        }
        
        public void visit(OpExt opExt)
        { 
            Op newOp;
                          
            // if there are some filterexpressions which could not be moved down,
            // an opFilter must be inserted that contains this filterexpressions
            if (!this.filterExpr.isEmpty())
            {
                // create the filter
                newOp = addFilterTransform.transform(opExt); 
                // add the conditions
                ((OpFilter)newOp).getExprs().getList().addAll(this.filterExpr);
            }else
            {
                // nothing must be done
                newOp = opExt;
            }
                         
            this.stack.push(newOp);
        }
        
        public void visit(OpNull opNull)
        { 
            notMoveDownFilterExprAndVisitOp0(opNull); 
        }
        
        public void visit(OpLabel opLabel)
        { 
            Op newOp, subOp = null ;

            // check for subtree
            if (opLabel.getSubOp() != null )
            {
                opLabel.getSubOp().visit(this);
                subOp = (Op)this.stack.pop();
            }
            
            // remove the label
            newOp = opLabel.apply(d2rqTransform, subOp);
            this.stack.push(newOp);
            
        }
        
        public void visit(OpList opList)
        { 
            notMoveDownFilterExprAndVisitOp1(opList); 
        }
        
        public void visit(OpOrder opOrder)
        { 
            notMoveDownFilterExprAndVisitOp1(opOrder); 
        }
        
        public void visit(OpProject opProject)
        { 
            notMoveDownFilterExprAndVisitOp1(opProject); 
        }
        
        public void visit(OpDistinct opDistinct)
        { 
            notMoveDownFilterExprAndVisitOp1(opDistinct); 
        }
        
        public void visit(OpReduced opReduced)
        { 
            notMoveDownFilterExprAndVisitOp1(opReduced); 
        }
        
        public void visit(OpAssign opAssign)
        { 
            notMoveDownFilterExprAndVisitOp1(opAssign); 
        }
        
        public void visit(OpSlice opSlice)
        { 
            notMoveDownFilterExprAndVisitOp1(opSlice); 
        }
        
        public void visit(OpGroupAgg opGroupAgg)
        { 
            notMoveDownFilterExprAndVisitOp1(opGroupAgg); 
        }        
        
        /**
         * Calculates the set of valid filterexpressions as a subset from the whole set of 
         * possibilities. 
         * @param possiblities - the whole set
         * @param opLabel - contains the threated object-vars, which are used to determine
         *                  the valid filterexpressions 
         * @return List - the subset from the set of possiblities
         */
        private List calcValidFilterExpr(List possiblities, OpLabel opLabel)
        {
            Set threatedObjectVars;
            Expr expr;
            Set mentionedVarsInExpr;
            List result;
            
            // get the set of the threated object-vars
            threatedObjectVars = (Set) opLabel.getObject();
            
            result = new ArrayList();
            
            // now check every var of every filterexpression in the set of all 
            // filterexpression-possibilities to check if the filterexpression is valid
            for (Iterator iterator = possiblities.iterator(); iterator.hasNext();)
            {
                expr = (Expr)iterator.next();
              
                // get all in the expression mentioned object-vars
                mentionedVarsInExpr = expr.getVarsMentioned();
                
                // check if the alle mentioned-vars of the expression are a subset
                // of the threated-Object-Vars
                // if yes, the expression is valid, otherwise not
                if (threatedObjectVars.containsAll(mentionedVarsInExpr))
                {
                    result.add(expr);
                }
            }
            
            return result;
        }
        
        
        /**
         * Checks first if a filterexpression can be moved down. 
         * And after visits the operator
         */
        private void checkMoveDownFilterExprAndVisitOpUnion(OpUnion opUnion)
        {
            Op left = null ;
            Op right = null ;
            Op newOp;
            OpLabel opLeftLabel, opRightLabel;
            List filterExprBeforeOpUnion, filterExprAfterOpUnion, notMoveableFilterExpr;
            
            // contains the filterexpressions that are valid before this op2
            filterExprBeforeOpUnion = new ArrayList(this.filterExpr);
            // contains the filterexpressions that are valid after this op2
            // this is needed because during the bottom-up-stepping all filterexpressions
            // which could not be transformed down, must be inserted by means of an OpFilter
            // above this op2
            filterExprAfterOpUnion = new ArrayList();
            
            // check left subtree
            if ((left = opUnion.getLeft()) != null )
            {
                // calculate the set of filterexpressions that are also valid for the 
                // left subtree
                if (left instanceof OpLabel)
                {
                    opLeftLabel = (OpLabel)left;
                    this.filterExpr = calcValidFilterExpr(filterExprBeforeOpUnion, opLeftLabel);
                    filterExprAfterOpUnion.addAll(this.filterExpr);
                }    
                // step down    
                opUnion.getLeft().visit(this);
                left = (Op)this.stack.pop();                                
            }            
            
            // check the right subtree
            if ((right = opUnion.getRight()) != null )
            {
                // calculate the set of filterexpressions that are also valid for the 
                // right subtree
                if (right instanceof OpLabel)
                {
                    opRightLabel = (OpLabel)right;
                    this.filterExpr = calcValidFilterExpr(filterExprBeforeOpUnion, opRightLabel);
                    filterExprAfterOpUnion.addAll(this.filterExpr);
                }
                // step down
                opUnion.getRight().visit(this);
                right = (Op)this.stack.pop();
            }
            
            // note: filterExprAfterOpUnion contains now all filterexpressions which could
            // be moved down
            // now calculate all filterexpressions which were not moveable
            notMoveableFilterExpr = new ArrayList(filterExprBeforeOpUnion);
            notMoveableFilterExpr.removeAll(filterExprAfterOpUnion);

            // if there are some filterexpressions which could not be moved down,
            // an opFilter must be inserted that contains this filterexpressions
            if (!notMoveableFilterExpr.isEmpty())
            {
                // create the filter
                newOp = opUnion.apply(addFilterTransform, left, right); 
                // add the conditions
                ((OpFilter)newOp).getExprs().getList().addAll(notMoveableFilterExpr);
            }else
            {
            	newOp = opUnion.apply(d2rqTransform, left, right);
            }
            
            // restore filterexpressions
            this.filterExpr = filterExprBeforeOpUnion;
            
            this.stack.push(newOp);
        }
           
        /**
         * Checks first if a filterexpression can be moved down. 
         * And after visits the operator
         */
        private void checkMoveDownFilterExprAndVisitOpJoin(OpJoin opJoin)
        {
            Op left = null ;
            Op right = null ;
            Op newOp;
            OpLabel opLeftLabel, opRightLabel;
            List filterExprBeforeOpJoin, filterExprAfterOpJoin, notMoveableFilterExpr;
            
            // contains the filterexpressions that are valid before this op2
            filterExprBeforeOpJoin = new ArrayList(this.filterExpr);
            // contains the filterexpressions that are valid after this op2
            // this is needed because during the bottom-up-stepping all filterexpressions
            // which could not be transformed down, must be inserted by means of an OpFilter
            // above this op2
            filterExprAfterOpJoin = new ArrayList();
            
            // check left subtree
            if ((left = opJoin.getLeft()) != null )
            {
                // calculate the set of filterexpressions that are also valid for the 
                // left subtree
                if (left instanceof OpLabel)
                {
                    opLeftLabel = (OpLabel)left;
                    this.filterExpr = calcValidFilterExpr(filterExprBeforeOpJoin, opLeftLabel);
                    filterExprAfterOpJoin.addAll(this.filterExpr);
                }    
                // step down    
                opJoin.getLeft().visit(this);
                left = (Op)this.stack.pop();                                
            }            
            
            // check the right subtree
            if ((right = opJoin.getRight()) != null )
            {
                // calculate the set of filterexpressions that are also valid for the 
                // right subtree
                if (right instanceof OpLabel)
                {
                    opRightLabel = (OpLabel)right;
                    this.filterExpr = calcValidFilterExpr(filterExprBeforeOpJoin, opRightLabel);
                    filterExprAfterOpJoin.addAll(this.filterExpr);
                }
                // step down
                opJoin.getRight().visit(this);
                right = (Op)this.stack.pop();
            }
            
            // note: filterExprAfterOpUnion contains now all filterexpressions which could
            // be moved down
            // now calculate all filterexpressions which were not moveable
            notMoveableFilterExpr = new ArrayList(filterExprBeforeOpJoin);
            notMoveableFilterExpr.removeAll(filterExprAfterOpJoin);

            // if there are some filterexpressions which could not be moved down,
            // an opFilter must be inserted that contains this filterexpressions
            if (!notMoveableFilterExpr.isEmpty())
            {
                // create the filter
                newOp = opJoin.apply(addFilterTransform, left, right); 
                // add the conditions
                ((OpFilter)newOp).getExprs().getList().addAll(notMoveableFilterExpr);
            }else
            {
                // nothing must be done
            	newOp = opJoin.apply(d2rqTransform, left, right);
            }
            
            // restore filterexpressions
            this.filterExpr = filterExprBeforeOpJoin;
            
            this.stack.push(newOp);
        }
        
        /**
         * Checks first if a filterexpression can be moved down. 
         * And after visits the operator
         */
        private void checkMoveDownFilterExprAndVisitOpLeftJoin(OpLeftJoin opLeftJoin)
        {
            Op left = null ;
            Op right = null ;
            Op newOp;
            OpLabel opLeftLabel, opRightLabel;
            List filterExprBeforeOpLeftJoin, filterExprAfterOpLeftJoin, notMoveableFilterExpr, filterExprRightSide, validFilterExprRightSide;
            Expr expr;
            
            // contains the filterexpressions that are valid before this op2
            filterExprBeforeOpLeftJoin = new ArrayList(this.filterExpr);
            // contains the filterexpressions that are valid after this op2
            // this is needed because during the bottom-up-stepping all filterexpressions
            // which could not be transformed down, must be inserted by means of an OpFilter
            // above this op2
            filterExprAfterOpLeftJoin = new ArrayList();
            
            	
            // check left subtree
            if ((left = opLeftJoin.getLeft()) != null )
            {
                // calculate the set of filterexpressions that are also valid for the 
                // left subtree
                if (left instanceof OpLabel)
                {
                    opLeftLabel = (OpLabel)left;
                    this.filterExpr = calcValidFilterExpr(filterExprBeforeOpLeftJoin, opLeftLabel);
                    filterExprAfterOpLeftJoin.addAll(this.filterExpr);
                }    
                // step down    
                opLeftJoin.getLeft().visit(this);
                left = (Op)this.stack.pop();                                
            }            
            
            // check the right subtree
            if ((right = opLeftJoin.getRight()) != null )
            {
                // calculate the set of filterexpressions that are also valid for the 
                // right subtree
                if (right instanceof OpLabel)
                {
                    opRightLabel = (OpLabel)right;
                    
                    filterExprRightSide = calcValidFilterExpr(filterExprBeforeOpLeftJoin, opRightLabel);
                    validFilterExprRightSide = new ArrayList();
                    
                    // now check for expr that are vaild for left-side and right-side
                    // only when an expr is vaid for left-side, it can be vaild for right-side
                    for(Iterator iterator = filterExprRightSide.iterator(); iterator.hasNext();)
                    {
                    	expr = (Expr)iterator.next();
                    	
                    	if (this.filterExpr.contains(expr))
                    	{
                    		// valid
                    		validFilterExprRightSide.add(expr);
                    	}
                    }
                    
                    this.filterExpr = validFilterExprRightSide;
                    filterExprAfterOpLeftJoin.addAll(this.filterExpr);
                }
                // step down
                opLeftJoin.getRight().visit(this);
                right = (Op)this.stack.pop();
            }
            
            
            
            // note: filterExprAfterOpUnion contains now all filterexpressions which could
            // be moved down
            // now calculate all filterexpressions which were not moveable
            notMoveableFilterExpr = new ArrayList(filterExprBeforeOpLeftJoin);
            notMoveableFilterExpr.removeAll(filterExprAfterOpLeftJoin);

            // try to merge left and right op and create an OpLeftJoinD2RQ
        	// if possible, newOp is an OpLeftJoinD2RQ
            // if not possible newOp is an OpLeftJoin
            newOp = opLeftJoin.apply(d2rqTransform, left, right);	

            
            // if there are some filterexpressions which could not be moved down,
            // an opFilter must be inserted that contains this filterexpressions
            if (!notMoveableFilterExpr.isEmpty())
            {        	
            	if (newOp instanceof OpLeftJoin)
            	{
	                // create the filter for an opleftjoin
	                newOp = opLeftJoin.apply(addFilterTransform, left, right); 
            	}else
            	{
            		// newOp is an OpLeftJoinD2RQ
            		newOp = OpFilter.filter(newOp);
            	}
	            	// add the conditions
                ((OpFilter)newOp).getExprs().getList().addAll(notMoveableFilterExpr);
            }
            
            // restore filterexpressions
            this.filterExpr = filterExprBeforeOpLeftJoin;
            
            this.stack.push(newOp);
        }
        
        
        /**
         * Checks first if a filterexpression can be moved down. 
         * And after visits the operator
         */
        private void checkMoveDownFilterExprAndVisitOpDiff(Op2 opDiff)
        {
            Op left = null ;
            Op right = null ;
            Op newOp;
            OpLabel opLeftLabel, opRightLabel;
            List filterExprBeforeOpUnionOpJoin, filterExprAfterOpUnionOpJoin, notMoveableFilterExpr;
            
            // contains the filterexpressions that are valid before this op2
            filterExprBeforeOpUnionOpJoin = new ArrayList(this.filterExpr);
            // contains the filterexpressions that are valid after this op2
            // this is needed because during the bottom-up-stepping all filterexpressions
            // which could not be transformed down, must be inserted by means of an OpFilter
            // above this op2
            filterExprAfterOpUnionOpJoin = new ArrayList();
            
            // check left subtree
            if ((left = opDiff.getLeft()) != null )
            {
                // calculate the set of filterexpressions that are also valid for the 
                // left subtree
                if (left instanceof OpLabel)
                {
                    opLeftLabel = (OpLabel)left;
                    this.filterExpr = calcValidFilterExpr(filterExprBeforeOpUnionOpJoin, opLeftLabel);
                    filterExprAfterOpUnionOpJoin.addAll(this.filterExpr);
                }    
                // step down    
                opDiff.getLeft().visit(this);
                left = (Op)this.stack.pop();                                
            }            
            
            // check the right subtree
            if ((right = opDiff.getRight()) != null )
            {
                // calculate the set of filterexpressions that are also valid for the 
                // right subtree
                if (right instanceof OpLabel)
                {
                    opRightLabel = (OpLabel)right;
                    this.filterExpr = calcValidFilterExpr(filterExprBeforeOpUnionOpJoin, opRightLabel);
                    filterExprAfterOpUnionOpJoin.addAll(this.filterExpr);
                }
                // step down
                opDiff.getRight().visit(this);
                right = (Op)this.stack.pop();
            }
            
            // note: filterExprAfterOpUnion contains now all filterexpressions which could
            // be moved down
            // now calculate all filterexpressions which were not moveable
            notMoveableFilterExpr = new ArrayList(filterExprBeforeOpUnionOpJoin);
            notMoveableFilterExpr.removeAll(filterExprAfterOpUnionOpJoin);

            // if there are some filterexpressions which could not be moved down,
            // an opFilter must be inserted that contains this filterexpressions
            if (!notMoveableFilterExpr.isEmpty())
            {
                // create the filter
                newOp = opDiff.apply(addFilterTransform, left, right); 
                // add the conditions
                ((OpFilter)newOp).getExprs().getList().addAll(notMoveableFilterExpr);
            }else
            {
                // nothing must be done
            	newOp = opDiff.apply(d2rqTransform, left, right);
            }
            
            // restore filterexpressions
            this.filterExpr = filterExprBeforeOpUnionOpJoin;
            
            this.stack.push(newOp);
        }
        
        
        /**
         * Handels and visits all op0 for which no filterexpression can be moved down. 
         * @param op0 - an Op0
         */
        private void notMoveDownFilterExprAndVisitOp0(Op0 op0)
        {
            Op newOp;    
            
            // if there are some filterexpressions which could not be moved down,
            // an opFilter must be inserted that contains this filterexpressions
            if (!this.filterExpr.isEmpty())
            {
                // create the filter
                newOp = op0.apply(addFilterTransform); 
                // add the conditions
                ((OpFilter)newOp).getExprs().getList().addAll(this.filterExpr);
            }else
            {
                // nothing must be done
                newOp = op0.apply(d2rqTransform);
            }
                         
            this.stack.push(newOp);
        }
        
        /**
         * Handels and visits all op1 for which no filterexpression can be moved down. 
         * @param op1 - an Op1
         */
        private void notMoveDownFilterExprAndVisitOp1(Op1 op1)
        {
            List notMoveableFilterExpr;
            Op newOp, subOp = null ;

            // all available FilterExpr could not be moved down
            notMoveableFilterExpr = new ArrayList(this.filterExpr);
            // clear the filterExpr
            filterExpr.clear();
            
            // check for subtree
            if (op1.getSubOp() != null )
            {
                op1.getSubOp().visit(this);
                subOp = (Op)this.stack.pop();
            }
            
            // if there are some filterexpressions which could not be moved down,
            // an opFilter must be inserted that contains this filterexpressions
            if (!notMoveableFilterExpr.isEmpty())
            {
                // create the filter
                newOp = op1.apply(addFilterTransform, subOp); 
                // add the conditions
                ((OpFilter)newOp).getExprs().getList().addAll(notMoveableFilterExpr);
            }else
            {
            	newOp = op1.apply(d2rqTransform, subOp);
            }
            
            // restore filterexpressions
            this.filterExpr = notMoveableFilterExpr; 
            this.stack.push(newOp);
        }
        
        /**
         * Handels and visits all op2 for which no filterexpression can be moved down. 
         * @param op2 - an Op2
         */
        private void notMoveDownFilterExprAndVisitOp2(Op2 op2)
        {
            List notMoveableFilterExpr;
            Op left = null ;
            Op right = null ;
            Op newOp;
            
            // all available FilterExpr could not be moved down
            notMoveableFilterExpr = new ArrayList(this.filterExpr);
            // clear the filterExpr
            filterExpr.clear();
            
            // check for left subtree
            if (op2.getLeft() != null )
            {
                // step down
                op2.getLeft().visit(this);
                left = (Op)this.stack.pop();
            }
            if (op2.getRight() != null )
            {
                // step down
                op2.getRight().visit(this);
                right = (Op)this.stack.pop();
            }
            
            // if there are some filterexpressions which could not be moved down,
            // an opFilter must be inserted that contains this filterexpressions
            if (!notMoveableFilterExpr.isEmpty())
            {
                // create the filter
                newOp = op2.apply(addFilterTransform, left, right); 
                // add the conditions
                ((OpFilter)newOp).getExprs().getList().addAll(notMoveableFilterExpr);
            }else
            {
                // nothing must be done
            	newOp = op2.apply(d2rqTransform, left, right);
            }
            
            // restore filterexpressions
            this.filterExpr = notMoveableFilterExpr; 
            this.stack.push(newOp);            
        }
        
        /**
         * Handels and visits all opN for which no filterexpression can be moved down. 
         * @param opN - an OpN
         */
        private void notMoveDownFilterExprAndVisitOpN(OpN opN)
        {
            List list;
            List notMoveableFilterExpr;
            Op subOp, op, newOp;
            
            // all available FilterExpr could not be moved down
            notMoveableFilterExpr = new ArrayList(this.filterExpr);
            // clear the filterExpr
            filterExpr.clear();
            
            list = new ArrayList(opN.size()) ;
            
            // check for subtrees
            for (Iterator iterator = opN.iterator(); iterator.hasNext();)
            {
                subOp = (Op)iterator.next();
                subOp.visit(this);
                
                op = (Op)this.stack.pop();
                if (op != null)
                {
                    list.add(op);
                }
            }
            
            // if there are some filterexpressions which could not be moved down,
            // an opFilter must be inserted that contains this filterexpressions
            if (!notMoveableFilterExpr.isEmpty())
            {
                // create the filter
                newOp = opN.apply(addFilterTransform, list); 
                // add the conditions
                ((OpFilter)newOp).getExprs().getList().addAll(notMoveableFilterExpr);
            }else
            {
                // nothing must be done
            	newOp = opN.apply(d2rqTransform, list);
            }
            
            // restore filterexpressions
            this.filterExpr = notMoveableFilterExpr; 
            this.stack.push(newOp);
        }
        
    }
    
}
