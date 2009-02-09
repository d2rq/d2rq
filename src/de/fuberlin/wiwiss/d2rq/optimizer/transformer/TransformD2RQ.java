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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpLabel;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprNode;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.CompatibleRelationGroup;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.RelationalOperators;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.engine.GraphPatternTranslator;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.engine.OpD2RQ;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.optimizer.LeftJoin;
import de.fuberlin.wiwiss.d2rq.optimizer.VarFinder;
import de.fuberlin.wiwiss.d2rq.optimizer.ops.OpFilteredBGP;
import de.fuberlin.wiwiss.d2rq.optimizer.ops.OpLeftJoinD2RQ;
import de.fuberlin.wiwiss.d2rq.optimizer.utility.ExprUtility;
import de.fuberlin.wiwiss.d2rq.optimizer.utility.MutableIndex;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.values.Pattern;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;

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
	private MutableIndex mutableIndex;
	private VarFinder varFinder;
	
	/**
	 * Constructor
	 * 
	 * @param graph - a D2RQ-Graph
	 */
	public TransformD2RQ(GraphD2RQ graph, MutableIndex mutableIndex, VarFinder varFinder) 
	{
		this.graph = graph;
		this.mutableIndex = mutableIndex;
		this.varFinder = varFinder;
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
	 * In some ways it is possible to perform an OpLeftJoin on database-layer.
	 * When there is an OpLeftJoin with 2 OpD2RQs as children, and this 2 OpD2RQs
	 * have some common variables, it is possible to link them and make an
	 * OpLeftJoinD2RQ, which links the 2 SQL-Statements of the OpD2RQ with a
	 * sql-leftjoin.	
	 */
	public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) 
	{
		OpD2RQ leftOpD2RQ, rightOpD2RQ;
		Relation leftRelation, rightRelation;
		Collection leftBindingMakers, rightBindingMakers;
		BindingMaker leftBindingMaker, rightBindingMaker, mergedBindingMaker;
		Set leftVars, rightVars;
		String variableName;
		NodeMaker nodeMaker;
		TypedNodeMaker leftTypedNodeMaker, rightTypedNodeMaker;
		ValueMaker leftValueMaker, rightValueMaker;
		Pattern leftPattern, rightPattern;
		Attribute leftAttribute, rightAttribute;
		Set leftJoinConditions, mergedLeftJoinConditions;
		Set mergedProjections;
		AliasMap leftAliasMap, rightAliasMap, mergedAliasMap;
		ConnectedDB database;
		Expression mergedConditions;
		Set mergedJoinConditions;
		Relation mergedRelation;
		boolean unique, mergingPossible = false;
		Map variableNamesToNodeMakers;
		Set mentionedTableNames;
		LeftJoin leftJoin;
		
		// only leftjoins that satisfy follow conditions will be transformed
		// a) no expressions (i think this could also perhaps be optimized and transformed to sql???) 
		// b) the two sub-ops are OpD2RQs
		// If neither condition a) nor b) is fulfilled, the leftjoin will pe performed on logical layer.
		// This means for every result-entry of the left-side, an extra sql-statement for the optional values will 
		// be executed.
		if ((opLeftJoin.getExprs() == null || opLeftJoin.getExprs().isEmpty()) && left instanceof OpD2RQ && right instanceof OpD2RQ)
		{
			// collect the values of the two OpD2RQs that are needed
			// to check if a linking with "left join" on sql-layer is possible
			leftOpD2RQ = (OpD2RQ)left;
			rightOpD2RQ = (OpD2RQ)right;
			
			// relations
			leftRelation = leftOpD2RQ.getRelation();
			rightRelation = rightOpD2RQ.getRelation();
			// bindingmakers
			leftBindingMakers = leftOpD2RQ.getBindingMakers();
			rightBindingMakers = rightOpD2RQ.getBindingMakers();
			leftBindingMaker = (BindingMaker)leftBindingMakers.iterator().next();
			rightBindingMaker = (BindingMaker)rightBindingMakers.iterator().next();
			// vars	
			leftVars = leftBindingMaker.variableNames();
			rightVars = rightBindingMaker.variableNames();
			
			// contains all attribute-equality, needed in an left-join on sql-layer
			// for example: left join on attribute1 = attribute2
			leftJoinConditions = new HashSet();
			
			mentionedTableNames = new HashSet();
			
			// now check if right-vars can be linked with left-vars
			// and if a left-join on sql-layer is possible
			for(Iterator iterator = rightVars.iterator(); iterator.hasNext(); )
			{
				// var from right-side
				variableName = (String)iterator.next();
				
				// check if right-side-variable is also part in left-side
				if (leftVars.contains(variableName))
				{
					// value-maker for left-side
					nodeMaker = leftBindingMaker.nodeMaker(Var.alloc(variableName));
					leftTypedNodeMaker = (TypedNodeMaker)nodeMaker;
					leftValueMaker = leftTypedNodeMaker.valueMaker();
					
					// value-maker for right-side
					nodeMaker = rightBindingMaker.nodeMaker(Var.alloc(variableName));
					rightTypedNodeMaker = (TypedNodeMaker)nodeMaker;
					rightValueMaker = rightTypedNodeMaker.valueMaker();
					
					// i do not know if this if-statement is really necessary
					// but definitely is definitely 
					if (leftValueMaker instanceof Pattern && rightValueMaker instanceof Pattern)
					{
						leftPattern = (Pattern)leftValueMaker;
						rightPattern = (Pattern)rightValueMaker;
						
						// check for equality
						// means joining with left-join on sql-layer is possible
						// check for joining-attributes
						if (leftPattern.isEquivalentTo(rightPattern)) 
						{
							// now collect all values that are necessary for a left-join on sql-layer
							// collect joining-attributes for left-join
							// collect the tables for left-join
							for (int i = 0; i < leftPattern.attributes().size(); i++) 
							{
								leftAttribute = (Attribute) leftPattern.attributes().get(i);
								rightAttribute = (Attribute) rightPattern.attributes().get(i);
								leftJoinConditions.add(Equality.createAttributeEquality(leftAttribute, rightAttribute));
								mentionedTableNames.add(leftAttribute.relationName());
								mentionedTableNames.add(rightAttribute.relationName());
							}
							
							// left-join on sql-layer is possible
							mergingPossible = true;
						}
					}
					
				}
			}	

			// check for common subjects
			if (!mergingPossible)
			{
				BasicPattern basicPattern;
				Triple triple;
				Node subjectNode;
				Set leftSubjectValues, rightSubjectValues;
				TripleRelation tripleRelation;
				NodeMaker nodeMaker2;
				ValueMaker valueMaker2;
				Pattern pattern;				
				
				leftSubjectValues = new HashSet();
				rightSubjectValues = new HashSet();
				
				// collect left-subjects
				basicPattern = ((OpBGP)leftOpD2RQ.effectiveOp()).getPattern();
				for(Iterator iterator = basicPattern.iterator(); iterator.hasNext();)
				{
					triple = (Triple)iterator.next();
					subjectNode = triple.getSubject();
					
					if (subjectNode.isURI())
					{
						leftSubjectValues.add(subjectNode);
					}
				}
				
				// collect left-subjects
				basicPattern = ((OpBGP)rightOpD2RQ.effectiveOp()).getPattern();
				for(Iterator iterator = basicPattern.iterator(); iterator.hasNext();)
				{
					triple = (Triple)iterator.next();
					subjectNode = triple.getSubject();
					
					if (subjectNode.isURI())
					{
						rightSubjectValues.add(subjectNode);
					}
				}
							
				if (!rightSubjectValues.isEmpty() && leftSubjectValues.containsAll(rightSubjectValues) && rightSubjectValues.containsAll(leftSubjectValues))
				{
					// left-join is possible
					
					for(Iterator iterator = rightSubjectValues.iterator(); iterator.hasNext();)
					{
						subjectNode = (Node)iterator.next();
						triple = new Triple(subjectNode, Node.ANY, Node.ANY);
						
						for(Iterator tripleRelationsIterator = graph.tripleRelations().iterator(); tripleRelationsIterator.hasNext();)
						{
							tripleRelation = (TripleRelation)tripleRelationsIterator.next();
							nodeMaker2 = tripleRelation.nodeMaker(TripleRelation.SUBJECT);
							
							NodeMaker s = nodeMaker2.selectNode(subjectNode, RelationalOperators.DUMMY);
							
							if (!s.equals(NodeMaker.EMPTY))
							{
								valueMaker2 = ((TypedNodeMaker)nodeMaker2).valueMaker();
								pattern = (Pattern)valueMaker2;
								
								for (Iterator attributesIterator = pattern.attributes().iterator(); attributesIterator.hasNext();) 
								{
									Attribute attribute = (Attribute)attributesIterator.next();
									Attribute attribute1 = leftRelation.aliases().applyTo(attribute);
									Attribute attribute2 = rightRelation.aliases().applyTo(attribute);
									leftJoinConditions.add(Equality.createAttributeEquality(attribute1, attribute2));
									mentionedTableNames.add(attribute1.relationName());
									mentionedTableNames.add(attribute2.relationName());
								}
								break;
							}													
						}
					}
					mergingPossible = true;
				}
				
			}
			
			if (mergingPossible)
			{
				// now start to merge the left-relation and the right-relation and add some left-join-information
				// the result of the merge-process will be in a new relation
				// This means: 1) merge all projections from the right-relation to the left one
				//             2) merge all aliases from the right-relation to the left one
				//             3) merge the binding-makers from the left-relation and right-relation
				//             4) add some left-join-information
				// These information is used in the SelectStatementBuilder-Class to create a left-join on
				// sql-layer
				
				// new leftjoinConditions
				leftJoin = new LeftJoin(new ArrayList(mentionedTableNames), leftJoinConditions);
								
				mergedLeftJoinConditions = new HashSet();
				mergedLeftJoinConditions.add(leftJoin);
				
				// old leftJoinConditions (more than one optional)
				mergedLeftJoinConditions.addAll(leftRelation.leftJoinConditions());
				mergedLeftJoinConditions.addAll(rightRelation.leftJoinConditions());
				
				// merge projections
				mergedProjections = new HashSet();
				mergedProjections.addAll(leftRelation.projections());
				mergedProjections.addAll(rightRelation.projections());

				// merge aliases
				leftAliasMap = leftRelation.aliases();
				rightAliasMap = rightRelation.aliases();
				mergedAliasMap = leftAliasMap.applyTo(rightAliasMap);
				
				// merge all other values
				// TODO: could it possible that the databases of the
				// the two relations are different ?
				database = leftRelation.database();

				if ((unique = leftRelation.isUnique()) == rightRelation.isUnique())
				{
					mergedConditions = leftRelation.condition().and(rightRelation.condition());
					mergedJoinConditions = new HashSet();
					mergedJoinConditions.addAll(leftRelation.joinConditions());
					mergedJoinConditions.addAll(rightRelation.joinConditions());
					// create new merged Relation
					mergedRelation = new RelationImpl(database, mergedAliasMap, mergedConditions, mergedJoinConditions, mergedProjections, mergedLeftJoinConditions, unique);
	
					// now merge all bindingmakers			
					variableNamesToNodeMakers = new HashMap();
					
					// add binding-makers from left-side
					for (Iterator iterator = leftBindingMaker.variableNames().iterator(); iterator.hasNext(); )
					{
						variableName = (String) iterator.next();
						nodeMaker = (NodeMaker) leftBindingMaker.nodeMaker(Var.alloc(variableName));
						variableNamesToNodeMakers.put(variableName, nodeMaker);
					}
					
					// add binding-makers from right-side
					for (Iterator iterator = rightBindingMaker.variableNames().iterator(); iterator.hasNext(); )
					{
						variableName = (String) iterator.next();
						nodeMaker = (NodeMaker) rightBindingMaker.nodeMaker(Var.alloc(variableName));
						
						// check for equal varnames and not equal bindingmaker - 
						// i do not know if this case can really happen
						if (!variableNamesToNodeMakers.containsKey(variableName))
						{
							variableNamesToNodeMakers.put(variableName, nodeMaker);
						}
					}
					
					// create a merged binding-maker
					mergedBindingMaker = new BindingMaker(variableNamesToNodeMakers);
					
					// create an OpLeftJoinD2RQ, that performs a left-join on sql-layer
					// TODO: it could perhaps be a problem that the BGP from the left-relation is used
					// but i think not
					return new OpLeftJoinD2RQ((OpBGP)leftOpD2RQ.effectiveOp(), mergedRelation, Collections.singleton(mergedBindingMaker), this.varFinder);
				}
			}
			
		}
		// perform the leftjoin on logical layer.
		return super.transform(opLeftJoin, left, right);
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
        
        // Check if an unique index over all sql-queries is needed
        // The Index is only used when it is a sparql-query that contains OPTIONALs
        // The index is needed because of merging Relations
        if (this.varFinder.getOpt().isEmpty() && opBGP.getPattern().getList().size() == 1)
        {        	
        	this.mutableIndex.setUseIndex(false);
        }
        
        nodeRelations = new GraphPatternTranslator(opBGP.getPattern().getList(), graph.tripleRelations(), this.mutableIndex).translate();
        
        this.mutableIndex.setUseIndex(true);
        
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
        String varName;
        Map variablesToNodeMakers;
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
    	if (expr instanceof ExprNode) { // only handle SPARQL, no RDQL
	        // convert to a sql-string
	        String sqlString = ExprUtility.convertExprToSQL(expr, nodeRelation);
	        
	        // converting sucessfully ?
	        if (sqlString != null)
	            return SQLExpression.create(sqlString);
    	}
    	
    	return null;
    }
        
}
