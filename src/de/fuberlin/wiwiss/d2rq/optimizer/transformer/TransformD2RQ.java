package de.fuberlin.wiwiss.d2rq.optimizer.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpConditional;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpLabel;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.main.LeftJoinClassifier;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprNode;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.algebra.CompatibleRelationGroup;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.engine.GraphPatternTranslator;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.engine.OpD2RQ;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.optimizer.ops.OpFilteredBGP;
import de.fuberlin.wiwiss.d2rq.optimizer.utility.ExprUtility;


/**
 * Transforms OpBGPs / OpFilteredBGPs to OpD2RQs.
 * 
 * @author Herwig Leimer
 * 
 */
public class TransformD2RQ extends TransformCopy
{

	private Logger logger = LoggerFactory.getLogger(TransformD2RQ.class);
	
	static final boolean LINEAR_LEFT_JOIN = false;
	
	private GraphD2RQ graph;
	
	/**
	 * Constructor
	 * 
	 * @param graph - a D2RQ-Graph
	 */
	public TransformD2RQ(GraphD2RQ graph) 
	{
		this.graph = graph;
	}
	
	
    ///////////// EXPERIMENTAL /////////////////
    public Op transform(OpLeftJoin opLeftJoin, Op opLeft, Op opRight)
    {
        if (LINEAR_LEFT_JOIN) {
            // Test whether we can do an indexed substitute into the right if possible.
            boolean canDoLinear = LeftJoinClassifier.isLinear(opLeftJoin);
            
            logger.debug("TransformD2RQ.transform() OpLeftJoin linear? {}", String.valueOf(canDoLinear));
            if (canDoLinear) {
                // Pass left into right for substitution before right side evaluation.
                // In an indexed left join, the LHS bindings are visible to the
                // RHS execution so the expression is evaluated by moving it to be 
                // a filter over the RHS pattern. 
                // In D2R, this means that for each LHS binding a new SQL is launched for the RHS pattern.
                // In the other case only one (unconstrained) SQL query is launched for the RHS and the LHS
                // and RHS bindings must be left-joined in memory. This can easily go wrong, because simple RHS patterns
                // can yield a HUGE set of bindings. 
                
                
                if (opLeftJoin.getExprs() != null )
                     opRight = OpFilter.filter(opLeftJoin.getExprs(), opRight) ;
                return new OpConditional(opLeft, opRight) ;
            }
        }
        // Not index-able (or disabled) 
        return super.transform(opLeftJoin, opLeft, opRight) ;
    }
    ///////////// EXPERIMENTAL /////////////////
    
    
	/**
	 * If the filter does not contain any filterexpressions, then remove it 
	 */
	public Op transform(OpFilter opFilter, Op subOp)                    
	{ 
		Op newOp;
		
		newOp = opFilter;
		
		// are there some expressions left ?
		if (opFilter.getExprs().isEmpty())
		{
			newOp = subOp;
		}
		
		return newOp;
	}
		
	/**
	 * Transforms an OpBGP to an OpD2RQ
	 */
    public Op transform(OpBGP opBGP)
    {
    	OpFilter opFilter = null;
    	Op newOp;
    	
    	// OpFilteredBGP ?
        if (opBGP instanceof OpFilteredBGP)
        {
        	opFilter = (OpFilter) ((OpFilteredBGP)opBGP).getParent();
        }
        
        // start the transforming-process
        newOp = transformOpBGP(opBGP, opFilter); 
        
        return newOp;
    }    
    
    public Op transform(OpLabel opLabel, Op subOp)
    {
    	// remove all labels
    	return subOp;
    }
    
	/**
     * Transforms an OpBGP/OpFilteredBGP into an D2RQ.
     * When there is a corresponding opfilter available, the conditions are checked
     * if they can be transformed to SQL.
     * @param opBGP - an opBGP
     * @param opFilter - corresponding Filter
     * @return Op - either an (1) OpD2RQ or an (2) OpFilter and an OpD2RQ. The second case
     * happens, if either all or only a subset of filterexpressions cannot be transformed
     * to SQL.
     */
    private Op transformOpBGP(OpBGP opBGP, OpFilter opFilter) 
    {
        List nodeRelations;
        Op op, tree = null;
        NodeRelation nodeRelation;
        Collection compatibleGroups;
        Iterator it;
        CompatibleRelationGroup group;
        OpD2RQ opD2RQ;
        
        nodeRelations = new GraphPatternTranslator(opBGP.getPattern().getList(), graph.tripleRelations(), graph.getConfiguration().getUseAllOptimizations()).translate();
        
        // no noderelation available 
        if (nodeRelations.isEmpty()) 
        {
            return OpNull.create();
        }
        
        // when a filter is available, try to integrate its expressions into the noderelations
        if (opFilter != null && graph.getConfiguration().getUseAllOptimizations())
        {
            nodeRelations = integrateFilterExprIntoNodeRelations(nodeRelations, opFilter);
        }
        
        // no noderelation available 
        if (nodeRelations.isEmpty()) 
        {
            return OpNull.create();
        }
        
        // only one noderelation
        if (nodeRelations.size() == 1) 
        {
            nodeRelation = (NodeRelation) nodeRelations.iterator().next();
         
            // create an opD2RQ
            opD2RQ = new OpD2RQ(opBGP, nodeRelation.baseRelation(), Collections.singleton(new BindingMaker(nodeRelation)));
            
            // could all filterexpression be converted to sql ?
            if (opFilter != null && !opFilter.getExprs().isEmpty())
            {
                // no, a filter must be inserted before the new OpD2RQ
                opFilter = (OpFilter) OpFilter.filter(opFilter.getExprs(), opD2RQ);
                return opFilter;
            }
            
            return opD2RQ;
        }
        
        
        compatibleGroups = CompatibleRelationGroup.groupNodeRelations(nodeRelations);
        it = compatibleGroups.iterator();
        
        while (it.hasNext()) 
        {
            group = (CompatibleRelationGroup) it.next();
            op = new OpD2RQ(opBGP, group.baseRelation(), group.bindingMakers());
            if (tree == null) {
                tree = op;
            } else {
                tree = new OpUnion(tree, op);
            }
        }
        
        // could all filterexpression be converted to sql ?
        if (opFilter != null && !opFilter.getExprs().isEmpty())
        {
            // no, a filter must be inserted before the new OpD2RQ
            opFilter = (OpFilter)OpFilter.filter(opFilter.getExprs(), tree);
            return opFilter;
        }
        // hack
//      opFilter.getExprs().getList().clear();
        
        return tree;
    }
    
    
    /**
     * Tries to integrate the available filterexpressions into the noderelations.
     * To integrate a filterexpression into a noderelation these conditions must be fullfilled:
     *          1) All vars of an expression must belong to the one noderelation
     *          2) Every expressionfunction that is contained in the expression
     *             must be transferable to sql
     * @param nodeRelations - list of available noderelations
     * @param opFilter - the filter with the filterexpressions
     * @return List - list with the noderealtions that include the filterexpressions, which 
     *                could be transformed to SQL 
     */
    private List integrateFilterExprIntoNodeRelations(List nodeRelations, OpFilter opFilter)
    {
        // all expressions of the filter
        List exprList = new ArrayList(opFilter.getExprs().getList());
        
        int numberOfNodeRelations = nodeRelations.size();
        
        // check every expression
        for(Iterator exprIterator = exprList.iterator(); exprIterator.hasNext();)
        {
            Expr expr = (Expr)exprIterator.next();
            
            Set mentionedVars = new HashSet();
            
            // all possible vars of the expression
            // workaround to remove the questionmark from the varname
            for(Iterator varIterator = expr.getVarsMentioned().iterator(); varIterator.hasNext();)
            {
                Var var = (Var)varIterator.next();
                mentionedVars.add(var.getName());
            }           
            
            
            // contains the new nodeRelations
            List newNodeRelations = new ArrayList();
            
            // now check every noderelation for integrating the expression
            boolean filterConversionSuccesfulForEveryNodeRelation = true;
            for(Iterator nodeRelationIterator = nodeRelations.iterator(); nodeRelationIterator.hasNext(); )
            {
                NodeRelation nodeRelation = (NodeRelation)nodeRelationIterator.next();
                
                if (nodeRelation.variableNames().containsAll(mentionedVars))
                {
                    // noderelation contains all vars of the expression
                    
                    Expression expression = convertFilterExprToSQLExpression(expr, nodeRelation);
                    
                    if (expression != null) {
                        // the expr can be transformed to sql
                        
                        if (expression.isTrue()) {
                            // no change needed
                        } if (expression.isFalse()) {
                            nodeRelation = NodeRelation.empty(nodeRelation.variableNames());
                        } else {
                            // make the noderelation mutable
                            MutableRelation mutableRelation = new MutableRelation(nodeRelation.baseRelation());
                            // add the new sqlExpression, that will later be added to the sql-query
                            mutableRelation.select(expression);
                            
                            // workaround - this map is needed to create a noderelation
                            Map variablesToNodeMakers = new HashMap();
                            for(Iterator varNamesIterator = nodeRelation.variableNames().iterator(); varNamesIterator.hasNext();)
                            {
                                String  varName = (String)varNamesIterator.next();
                                variablesToNodeMakers.put(varName, nodeRelation.nodeMaker(varName));
                            }
                            // now create the nodeRelation from the mutablenoderelation
                            nodeRelation = new NodeRelation(mutableRelation.immutableSnapshot(), variablesToNodeMakers);
                        }
                    } else {
                        filterConversionSuccesfulForEveryNodeRelation = false;
                    }
                } else {
                    logger.warn("contains not all vars {} {}", nodeRelation, expr);
                    filterConversionSuccesfulForEveryNodeRelation = false; // TODO correct?
                }
                
                // add noderelation (either the changed or original one, depending on the filterexpr)
                if (nodeRelation.baseRelation() != Relation.EMPTY)
                    newNodeRelations.add(nodeRelation);
            }
            // new noderelations with the (possible) converted expressions
            nodeRelations = newNodeRelations;
            logger.debug("{} convertable for every node relation? {}", expr, Boolean.valueOf(filterConversionSuccesfulForEveryNodeRelation));
            if (filterConversionSuccesfulForEveryNodeRelation) {
                // remove the expression from the filter, because it was convertable to sql
                opFilter.getExprs().getList().remove(expr);
            } 
        }
        
        logger.debug("remaining filters {}", opFilter.getExprs().getList());
        
        if (logger.isDebugEnabled()) {
            logger.debug("NodeRelations remaining: " + nodeRelations.size() + " (was " + numberOfNodeRelations + ")");
            for (int i = 0; i < nodeRelations.size(); i++) {
                logger.debug("{}", nodeRelations.get(i));
            }
        }
        return nodeRelations; // better solution would be to return list of (noderelation, not convertable filters for that noderelation) pairs
    }
    
    
    /**
     * Tries to convert a SPARQL filter expression to a SQL expression. 
     * 
     * @param expr
     * @return
     */
    private Expression convertFilterExprToSQLExpression(Expr expr, NodeRelation nodeRelation)
    {
    	if (expr instanceof ExprNode) { // only handle SPARQL, no RDQL
	        // convert to a sql-string
	        return ExprUtility.convertExprToSQL(expr, nodeRelation);
    	}
    	
    	return null;
    }
        
}
