package de.fuberlin.wiwiss.d2rq.optimizer.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.E_Equals;
import com.hp.hpl.jena.sparql.expr.E_NotEquals;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprFunction;
import com.hp.hpl.jena.sparql.expr.ExprFunction1;
import com.hp.hpl.jena.sparql.expr.ExprFunction2;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.ExprVisitor;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.expr.nodevalue.NodeValueNode;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ExpressionProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.RelationalOperators;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.values.Pattern;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;

/**
 * Transforms an expr to a sql-string
 * TODO Move away from sqlExpression.append() and build an expression tree instead
 *  
 * @author Herwig Leimer
 *
 */
public final class TransformExprToSQLApplyer implements ExprVisitor 
{
	private StringBuffer sqlExpression;
	private Expression expression = null;
	private boolean convertAbleSuccessful;     // flag if converting was possible
	private NodeRelation nodeRelation;
	
	/**
	 * Constructor
	 */
	public TransformExprToSQLApplyer(final NodeRelation nodeRelation)
	{	
	    this.sqlExpression = new StringBuffer();
	    this.convertAbleSuccessful = true;
	    this.nodeRelation = nodeRelation;
	}

	public void startVisit() 
	{ 	    
	}
    
    public void visit(ExprFunction func)
    {
        if (this.convertAbleSuccessful)
        {
            if ( func.getOpName() != null && func instanceof ExprFunction2)
            {
                // convert binary sparql-function-expression
                convertFunctionExpr2((ExprFunction2)func) ;
                return ;
            }else if ( func.getOpName() != null && func instanceof ExprFunction1)
            {
                // convert unary sparql-function-expression
                convertFunctionExpr1((ExprFunction1)func) ;
                return ;
            }else if (func instanceof ExprFunction)
            {
                // exists, regex cannot be translated
                this.convertAbleSuccessful = false;
                return ;
            }
            
            sqlExpression.append("(") ;
            for ( int i = 1 ; ; i++ )
            {
                Expr expr = func.getArg(i) ;
                if ( expr == null )
                    break ; 
                if ( i != 1 )
                    sqlExpression.append(", ") ;
                expr.visit(this) ;
            }
            sqlExpression.append(")");
        }
    }

    public void visit(NodeValue nv)
    {
    	if (nv.isLiteral())
    	{
    		if (nv.isDecimal() || nv.isDouble() || nv.isFloat() || nv.isInteger() || nv.isNumber())
    		{
    			sqlExpression.append(nv.asString());
    		}
    		else if (nv.isDateTime())
    		{
    			/*
    			 * Convert xsd:dateTime: CCYY-MM-DDThh:mm:ss
    			 * to SQL-92 TIMESTAMP: CCYY-MM-DD hh:mm:ss[.fraction]
    			 * TODO support time zones (WITH TIME ZONE columns)
    			 */
    			sqlExpression.append("'");
    			sqlExpression.append(nv.asString().replace("T", " "));
    			sqlExpression.append("'");
    		}
    		else
    		{
    			sqlExpression.append("'");
    			sqlExpression.append(nv.asString()) ;
    			sqlExpression.append("'");
    		}
    	}else
    	{
    		this.convertAbleSuccessful = false;
    	}
    	
    }

    public void visit(ExprVar exprVar)
    {        
        String varName;
        List sqlColumnNames;
        
        varName = exprVar.getVarName();
        
        // check for blank-node
        if (!Var.isBlankNodeVarName(varName))
        {
            // get the sql-column for the sparql-var
            sqlColumnNames = getSqlColumnNamesForSparqlVar(exprVar);
            
            if (sqlColumnNames.size() == 1)
            {
                sqlExpression.append(sqlColumnNames.get(0));
            } else
            {
                // no sql-column for sparql-var does exist
                // break up convertion
                this.convertAbleSuccessful = false;
            }
        }else
        {
            // if expression contains a blank node, no convertion to sql can
            // be done
            this.convertAbleSuccessful = false;
        }
        
    }

    public void finishVisit() 
    {        
    }


	/**
	 * Returns the sql-string
	 * @return String - the transformed sql-string if converting 
	 *                  was possible, otherwise null.
	 */
	public Expression result()
    { 
	    if (this.convertAbleSuccessful)
	    	return (expression != null ? expression : SQLExpression.create(sqlExpression.toString()));
	    else
	    	return null;
    }
		
	/**
	 * Converts an unary sparql-function-expression to sql
	 * @param expr - an unary sparql-function-expression
	 */
	private void convertFunctionExpr1(ExprFunction1 expr)
    {
        String sparqlOperator, sqlOperator;
        
        // name of the sparql-operator
        sparqlOperator = expr.getOpName();
        // get the equivalent sql-operator
        sqlOperator = SPARQLToSQLOperatorsTable.getSQLKeyword(sparqlOperator);
        
        if (!sqlOperator.equals(""))
        {
            sqlExpression.append("(");
            sqlExpression.append(sqlOperator);
            sqlExpression.append(" ");
            expr.getArg().visit(this);
            sqlExpression.append(")");
        }else
        {
            // no equivalent sql-operator does exist
            // expression cannot be converted
            convertAbleSuccessful = false;
        }
    }

	/**
     * Converts an binary sparql-function-expression to sql
     * @param expr - an binary sparql-function-expression
     */
    private void convertFunctionExpr2(ExprFunction2 expr)
    {
        String sparqlOperator, sqlOperator;
        boolean compatible = true;
        NodeMaker nodeMakerVar1, nodeMakerVar2;
        ValueMaker valueMakerVar1, valueMakerVar2;
        Pattern varPattern1, varPattern2;
        
        // name of the sparql-operator
        sparqlOperator = expr.getOpName();
        // get the equivalent sql-operator
        sqlOperator = SPARQLToSQLOperatorsTable.getSQLKeyword(sparqlOperator);

        // when arg1 & arg2 are an URL, check for compatiblity of their patterns
        if (expr.getArg1().isVariable() && expr.getArg2().isVariable())
        {
        	nodeMakerVar1 = this.nodeRelation.nodeMaker(expr.getArg1().getVarName());
        	nodeMakerVar2 = this.nodeRelation.nodeMaker(expr.getArg2().getVarName());
        	
        	// i don't know if this is really necessary
        	if (!(nodeMakerVar1 instanceof TypedNodeMaker) || !(nodeMakerVar2 instanceof TypedNodeMaker))
        	{
        		compatible = false;
        	}
        	
        	valueMakerVar1 = ((TypedNodeMaker)nodeMakerVar1).valueMaker();
        	valueMakerVar2 = ((TypedNodeMaker)nodeMakerVar2).valueMaker();
        
        	if (valueMakerVar1 instanceof Pattern && valueMakerVar2 instanceof Pattern)
        	{
        		varPattern1 = (Pattern)valueMakerVar1;
        		varPattern2 = (Pattern)valueMakerVar2;
        		
        		// check if pattern are compatible
        		if (!varPattern1.firstLiteralPart().equals(varPattern2.firstLiteralPart()))
        		{
        			compatible = false;
        		}
        	}
        	
        }
        
        /*
         * Translate node constants
         */
        if ((expr instanceof E_Equals || expr instanceof E_NotEquals) &&
        		((expr.getArg1().isConstant() && expr.getArg2().isVariable())
        		|| (expr.getArg2().isConstant() && expr.getArg1().isVariable()))) {
        	
        	Expr constant = expr.getArg1().isConstant() ? expr.getArg1() : expr.getArg2(); 
        	Expr variable = expr.getArg1().isVariable() ? expr.getArg1() : expr.getArg2(); 
        	
        	NodeMaker nm = this.nodeRelation.nodeMaker(variable.getVarName());
        	if (nm instanceof TypedNodeMaker && constant instanceof NodeValueNode)
            {
        		ValueMaker vm = ((TypedNodeMaker)nm).valueMaker();
        		NodeValueNode nvn =  ((NodeValueNode)constant);
        		Node node = nvn.getNode();
        			
    			if (node.isURI() && vm instanceof Pattern) {
    				if (!nm.selectNode(node, RelationalOperators.DUMMY).equals(NodeMaker.EMPTY)) {
    					if (expr instanceof E_Equals) {
        					expression = vm.valueExpression(node.getURI());
        					return;
    					}
    					else if (expr instanceof E_NotEquals) {
        					expression = Equality.create(Expression.FALSE, vm.valueExpression(node.getURI()));
        					return;
    					}
    				}
    			}
        	}
        }
                
        if (!sqlOperator.equals("") && compatible)
        {
            sqlExpression.append("(") ;
            expr.getArg1().visit(this) ;
            sqlExpression.append(" ") ;
            sqlExpression.append(sqlOperator) ;
            sqlExpression.append(" ") ;
            expr.getArg2().visit(this) ;
            sqlExpression.append(")");
        }else
        {
            // no equivalent sql-operator does exist
            // expression cannot be converted
            this.convertAbleSuccessful = false;
        } 
    }

    /**
     * Delivers the coresponding sql-column-names for a sparql-var
     * @param exprVar - a sparql-expr-var
     * @return String - the equivalent sql-column-name if it does exist, otherwise null
     */
    private List getSqlColumnNamesForSparqlVar(ExprVar exprVar)
    {
        ArrayList sqlVarNames = new ArrayList();
        NodeMaker nodeMaker;
        Attribute attribute;
        ProjectionSpec projectionSpec;
        ExpressionProjectionSpec expressionProjectionSpec;
        Expression expression;
        
        if (this.nodeRelation != null && exprVar != null)
        {
            // get the nodemaker for the expr-var
            nodeMaker = nodeRelation.nodeMaker(exprVar.asVar().getVarName());

            if (nodeMaker instanceof TypedNodeMaker)
            {
            	Iterator it = ((TypedNodeMaker)nodeMaker).projectionSpecs().iterator();
            	while (it.hasNext()) {
	            	projectionSpec = (ProjectionSpec)it.next();
	                
	            	if (projectionSpec == null)
	            		return Collections.EMPTY_LIST;

	            	if (projectionSpec instanceof Attribute)
            		{
            			attribute = (Attribute)projectionSpec;
            			sqlVarNames.add(attribute.qualifiedName());
            		}
            		else
            		{
            			// projectionSpec is a ExpressionProjectionSpec
            			expressionProjectionSpec = (ExpressionProjectionSpec)projectionSpec;
            			expression = expressionProjectionSpec.toExpression();
            			if (expression instanceof SQLExpression)
            			{
            				sqlVarNames.add(((SQLExpression)expression).getExpression());
            			}
            			else
            				return Collections.EMPTY_LIST;
		            }
            	}
            }
        }
        
        return sqlVarNames;
    }
}
