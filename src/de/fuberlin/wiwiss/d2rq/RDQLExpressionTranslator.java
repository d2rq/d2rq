/*
 * Created on 31.03.2005 by Joerg Garbers, FU-Berlin
 *
 */
package de.fuberlin.wiwiss.d2rq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.rdql.parser.*;

import de.fuberlin.wiwiss.d2rq.helpers.VariableBindings;

/**
 * RDQLExpressionTranslator translates an RDQL expression into a SQL
 * expression that may be weaker than the RDQL expression
 * @author jgarbers
 *
 */
public class RDQLExpressionTranslator {
    ConstraintHandler handler;
    VariableBindings variableBindings;
    Map variableNameToNodes;
    
    /** indicates that the translated sub-expression should be weaker or stronger
     * than the rdql sub-expression. (switched during negation).
     */
    boolean weaker=true;  
    static Map opMap; // rdql operator to sql operator map. null means: no map 
     
    public RDQLExpressionTranslator(ConstraintHandler handler) {
        super();
        this.handler=handler;
        variableBindings=handler.bindings;
        variableNameToNodes=variableBindings.variableNameToNodeMap();
        setupOperatorMap();
        // TODO Auto-generated constructor stub
    }
    
    
    /**
     * Translates a Jena RDQL Expression into an SQL expression.
     * We should try to resolve the strongest SQL condition possible.
     * Maybe for each expression we should calculate both the strongest and the weakest
     * condition, so that negation flips the meaning.
     * @param e
     * @return null if no 1:1 translation possible.
     * @see com.hp.hpl.jena.graph.query.Expression
     */
    public StringBuffer translate(Expression e) {
        String op=getSQLOp(e); // see at bottom
        if (op==null)
            return null;
        List list=translateArgs(e, true, null);
        if (list==null)
            return null;
        return applyInfix(op,list);        
    }
    
    /**
     * TODO check, if there are problems in SQL when comparing text fields 
     * with numeric values and text values with numeric fields
     * 
     * @param e
     * @return
     */
    public StringBuffer translate(ParsedLiteral e) {
        StringBuffer b=new StringBuffer();
        if (e.isInt())
            return new StringBuffer(Long.toString(e.getInt()));
        else if (e.isDouble())
            return new StringBuffer(Double.toString(e.getDouble()));
        else if (e.isBoolean())
            return e.getBoolean()? trueBuffer : falseBuffer;
        // else if (e.isNode())   
        // else if (e.isURI())
        else if (e.isString())
            return new StringBuffer("\'"+e.getString()+"\'");
        else 
            return null;
    }
    
    public StringBuffer translate(Q_Var var) {
        return translateVariable(var.getName());
    }
    public StringBuffer translate(Expression.Variable var) {
        return translateVariable(var.getName());
    }
    public StringBuffer translate(WorkingVar var) {
        //throw new RuntimeException("WorkingVar in RDQLExpressionTranslator");
        return null; 
    }
    
    // the case where "?x=?y" should be handled before triples are checked for shared variables
    /**
     * node variables that cannot be mapped to an SQL expression
     */ 

    public StringBuffer translateVariable(String varName) {
        // map var to a column-expression
        // we should choose the one that is most simple
        
        Node n=(Node) variableNameToNodes.get(varName);
        Set varIndexSet=(Set)variableBindings.boundVariableToShared.get(n);
        if (varIndexSet!=null) { // bound
            // TODO
            // substitute variable with value
        } else {
            // TODO
            // substitute variable with NodeMaker columns
        }
        return null;       
    }
    
    /**
     * creates an sql expression that contains op at infix positions
     * @param op
     * @param args
     * @return
     */
    public StringBuffer applyInfix(String op, List args) {
        StringBuffer sb=new StringBuffer("(");
        Iterator it=args.iterator();
        boolean first=true;
        while (it.hasNext()) {
            if (first) 
                first=false;
            else {
                sb.append(' ');
                sb.append(op);
                sb.append(' ');
            }
            String item=(String)it.next();
            sb.append(item);
        }
        sb.append(")");
        return sb;
    }
    
    /**
     * creates a unary sql expression
     * @param op
     * @param arg
     * @return
     */
    public StringBuffer applyUnary(String op, StringBuffer arg) {
        StringBuffer result=new StringBuffer("(");
        result.append(op);
        result.append(" ");
        result.append(arg);
        result.append(")");
        return result;
    }
    
    /**
     * translates an RDQL expression
     * @param e the RDQL expression
     * @param strict if set, all arguments must be translatable to create a complex SQL expression
     * @param neutral the neutral element of the current operation
     * @return
     */
    public List translateArgs(Expression e, boolean strict, StringBuffer neutral) {
        List list=new ArrayList();
        int count=e.argCount();
        for (int i=0; i<count;i++) {
           Expression ex=e.getArg(i);
           StringBuffer exStr=translate(ex);
           if (strict && exStr==null)
               return null;
           if ((exStr!=null) && (exStr!=neutral))
               list.add(exStr);
        }
        return list;
    }
    
    public static StringBuffer trueBuffer=new StringBuffer("true");
    public static StringBuffer falseBuffer=new StringBuffer("false");
        
    public StringBuffer translate(Q_LogicalAnd e) {
        List list=translateArgs(e, false, trueBuffer);
        if (list==null)
            return null;
        int count=list.size();
        if (!weaker && (e.argCount()>count)) // less is weaker!
            return null;
        if (count==0)
            return trueBuffer;
        if (count==1)
            return (StringBuffer) list.get(0);
        return applyInfix("AND",list);        
    }
    
    public StringBuffer translate(Q_LogicalOr e) {
        List list=translateArgs(e, true, falseBuffer);
        if (list==null)
            return null;
        int count=list.size();
        if (weaker && (e.argCount()>count)) // less args is stronger!
            return null;
        if (count==0)
            return falseBuffer;
        if (count==1)
            return (StringBuffer) list.get(0);
        return applyInfix("OR",list);        
    }
    
    /**
     * is this really the logical Not?
     * @param e
     * @return
     */
    public StringBuffer translate(Q_UnaryNot e) {
        boolean oldWeaker=weaker;
        weaker=!weaker;
        Expression ex=e.getArg(0);
        weaker=oldWeaker;
        StringBuffer exStr=translate(ex);
        StringBuffer result=null;
        if (exStr!=null) {
            if (exStr==trueBuffer)
                result=falseBuffer;
            else if (exStr==falseBuffer)
                result=trueBuffer;
            else 
                result=applyUnary("NOT",result);
        }
        return result;
    }
    
    public StringBuffer translateUnary(Expression e, String sqlOp) {
        Expression ex=e.getArg(0);
        StringBuffer exStr=translate(ex);
        if (exStr==null)
            return null;
        StringBuffer result=applyUnary(sqlOp,exStr);
        return result;
    }
    public StringBuffer translate(Q_UnaryMinus e) {
        return translateUnary(e,"-");
    }
    public StringBuffer translate(Q_UnaryPlus e) {
        return translateUnary(e,"-");
    }

    
    /////////////   Massive Mapping /////////////////////////////
    
    // From rdql.parser.ExprNode
    static final String exprBaseURI = "urn:x-jena:expr:" ; 
    String constructURI(String className)
    {
        if ( className.lastIndexOf('.') > -1 )
            className = className.substring(className.lastIndexOf('.')+1) ;
        return exprBaseURI+className ;
    }
    String opURL(String className) {
        return constructURI(className);
    }
    String opURL(Object obj) {
        return constructURI(obj.getClass().getName());
    }
   
    void putOp(String className,String sqlOp) {
        if (sqlOp==null) 
            return;
        String url=opURL(className);
        opMap.put(url,sqlOp);
    }
    /*
    String getOp(String className) {
        return (String)opMap.get(opURL(className));
    }
    String getOp(Object obj) {
        return (String)opMap.get(opURL(obj));
    }
    */
    String getSQLOp(Expression e) {
        return (String)opMap.get(e.getFun());
    }
    
    void setupOperatorMap() {
        if (opMap==null) {
            opMap=new HashMap();
            putOp("Q_Add","+");
            putOp("Q_BitAnd",null);
            putOp("Q_BitOr",null); // "||"
            putOp("Q_BitXor",null);
            putOp("Q_Divide","/");
            putOp("Q_Equal","=");
            putOp("Q_GreaterThan",">");
            putOp("Q_GreaterThanOrEqual",">=");
            putOp("Q_LeftShift",null);
            putOp("Q_LessThan","<");
            putOp("Q_LessThanOrEqual","<=");
            putOp("Q_LogicalAnd","AND");
            putOp("Q_LogicalOr","OR");
            putOp("Q_Modulus",null);
            putOp("Q_Multiply","*");
            putOp("Q_NotEqual","<>");
            putOp("Q_PatternLiteral",null);
            putOp("Q_RightSignedShift",null);
            putOp("Q_RightUnsignedShift",null);
            putOp("Q_StringEqual","=");
            putOp("Q_StringLangEqual",null);
            putOp("Q_StringMatch",null); // "LIKE"
            putOp("Q_StringNoMatch",null); // "NOT LIKE"
            putOp("Q_Subtract","-");
            putOp("Q_UnaryMinus","-");
            putOp("Q_UnaryNot","!");
            putOp("Q_UnaryPlus","+");
        }
    }

}
