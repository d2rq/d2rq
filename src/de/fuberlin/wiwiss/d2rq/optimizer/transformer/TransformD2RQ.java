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

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpLabel;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.Expr;
import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.algebra.CompatibleRelationGroup;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.engine.GraphPatternTranslator;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.engine.OpD2RQ;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.optimizer.ops.OpFilteredBGP;
import de.fuberlin.wiwiss.d2rq.optimizer.ops.OpMatJoinD2RQ;
import de.fuberlin.wiwiss.d2rq.optimizer.ops.OpMatLeftJoinD2RQ;
import de.fuberlin.wiwiss.d2rq.optimizer.utility.ExprUtility;

/**
 * Transforms OpBGPs / OpFilteredBGPs to OpD2RQs.
 * 
 * @author Herwig Leimer
 * 
 */
public class TransformD2RQ extends TransformCopy
{
//	private final Log log = LogFactory.getLog(TransformD2RQAndRemoveUnneededOps.class);
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
	 * Convert to OpMatJoin to prevent streaming evaluation of OpJoin in ARQ.
	 */
//	public Op transform(OpJoin opJoin, Op left, Op right) 
//	{
//		Op opMatJoin;
//		
//		opMatJoin = OpMatJoinD2RQ.create(opJoin, left, right);
//		
//		return opMatJoin;
//		
//	}

	/**
	 * Convert to OpMatJoin to prevent streaming evaluation of OpLeftJoin in ARQ.
	 */
//	public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) 
//	{
//		Op opMatLeftJoin;
//		
//		opMatLeftJoin = OpMatLeftJoinD2RQ.create(opLeftJoin, left, right, opLeftJoin.getExprs());
//		
//		return opMatLeftJoin;
//	}
    
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
        
//        log.trace(opBGP);
        nodeRelations = new GraphPatternTranslator(opBGP.getPattern().getList(), graph.tripleRelations()).translate();
        
        // no noderelation available 
        if (nodeRelations.isEmpty()) 
        {
            return OpNull.create();
        }
        
        // when a filter is available, try to integrate its expressions into the noderelations
        if (opFilter != null)
        {
            nodeRelations = integrateFilterExprIntoNodeRelations(nodeRelations, opFilter);
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
        List exprList, newNodeRelations;
        Expr expr;
        Set mentionedVars;
        NodeRelation nodeRelation;
        Expression sqlExpression;
        MutableRelation mutableRelation;
        Map variablesToNodeMakers;
        String varName;
        Var var;
        
        sqlExpression = null;
        // all expressions of the filter
        exprList = new ArrayList(opFilter.getExprs().getList());
        
        // check every expression
        for(Iterator exprIterator = exprList.iterator(); exprIterator.hasNext();)
        {
            expr = (Expr)exprIterator.next();
            
            mentionedVars = new HashSet();
            
            // all possible vars of the expression
            // workaround to remove the questionmark from the varname
            for(Iterator varIterator = expr.getVarsMentioned().iterator(); varIterator.hasNext();)
            {
                var = (Var)varIterator.next();
                mentionedVars.add(var.getName());
            }           
            
            
            // contains the new nodeRelations
            newNodeRelations = new ArrayList();
            
            // now check every noderelation for integrating the expression
            for(Iterator nodeRelationIterator = nodeRelations.iterator(); nodeRelationIterator.hasNext(); )
            {
                nodeRelation = (NodeRelation)nodeRelationIterator.next();
                
                if (nodeRelation.variableNames().containsAll(mentionedVars) && (sqlExpression = convertFilterExprToSQLExpression(expr, nodeRelation)) != null)
                {
                    // noderelation contains all vars of the expression
                    // the expr can be transformed to sql
                    // sqlExpression contains the transformed sql-string
                    
                    // make the noderelation mutable
                    mutableRelation = new MutableRelation(nodeRelation.baseRelation());
                    // add the new sqlExpression, that will later be added to the sql-query
                    mutableRelation.select(sqlExpression);
                    
                    // workaround - this map is needed to create a noderelation
                    variablesToNodeMakers = new HashMap();
                    for(Iterator varNamesIterator = nodeRelation.variableNames().iterator(); varNamesIterator.hasNext();)
                    {
                        varName = (String)varNamesIterator.next();
                        variablesToNodeMakers.put(varName, nodeRelation.nodeMaker(varName));
                    }
                    // now create the nodeRelation from the mutablenoderelation
                    nodeRelation = new NodeRelation(mutableRelation.immutableSnapshot(), variablesToNodeMakers);
                    // remove the expression from the filter, because it was convertable to sql
                    opFilter.getExprs().getList().remove(expr);
                }
                
                // add noderelation (either the changed or original one, depending on the filterexpr)
                newNodeRelations.add(nodeRelation);
            }
            // new noderelations with the (possible) converted expressions
            nodeRelations = newNodeRelations;
        }
        
        return nodeRelations;
    }
    
    
    /**
     * Tries to convert a filterexpression to a SQLExpression. 
     * 
     * @param expr
     * @return
     */
    private Expression convertFilterExprToSQLExpression(Expr expr, NodeRelation nodeRelation)
    {
        Expression sqlExpression;
        String sqlString;
        
        sqlExpression = null;
        
        // convert to a sql-string
        sqlString = ExprUtility.convertExprToSQL(expr, nodeRelation);
        
        // converting sucessfully ?
        if (sqlString != null)
        {
            // create SQLExpression
            sqlExpression = SQLExpression.create(sqlString);
        }
        
        return sqlExpression;
    }
    
}
