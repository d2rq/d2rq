/*
(c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.IndexValues;
import com.hp.hpl.jena.rdql.parser.*;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.find.SQLStatementMaker;
import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.map.Pattern;
import de.fuberlin.wiwiss.d2rq.rdql.ConstraintHandler.NodeMakerIterator;

/**
 * Translates an RDQL expression into a SQL expression.
 * The SQL expression that may be weaker than the RDQL expression.
 * Idea: traverse the RDQL expression tree. If there is a mapping
 * defined for an operator, translate its arguments, and construct the SQL term. 
 * Issues: 
 * 1) we must keep track of argument types, because a + might be a plus 
 *    or a concatenate. 
 * 2) Negation turns weaker into stronger.
 * 3) Some arguments in AND can be skipped (expression becomes weaker)
 * 4) Translation of variables into Columns needs access to calling context
 * 	  (ConstraintHandler and SQLStatementMaker)
 * TODO:
 *   split code into modules for 
 *   1) Query Language expressions
 *   2) SQL dialects
 *   3) Variable translators
 * @author jgarbers
 *
 */
public class ExpressionTranslator {
    ConstraintHandler handler;
    SQLStatementMaker statementMaker; // provides database and escaper
    VariableBindings variableBindings;
    Map variableNameToNodeConstraint=new HashMap();
    
    /** indicates that the translated sub-expression should be weaker or stronger
     * than the rdql sub-expression. (switched during negation).
     */
    boolean weaker=true;  
    int argType=BoolType;

    static Map opMap; // rdql operator to sql operator map. null means: no map 
//    static Map argTypeMap; // rdql operator to type map
    
    public static final int NoType=0;
    public static final int BoolType=1;
    public static final int BitType=2;
    public static final int StringType=4;
    public static final int NumberType=8;
    public static final int UriType=16; // TODO ?
    public static final int AnyType=31;
    public static final int LeftRightType=32; // different type for left and right operand
    public static final int LeftType=-1;
    public static final int RightType=-2;
    public static final int SameType=-3;
    
    public ExpressionTranslator(ConstraintHandler handler, SQLStatementMaker sql) {
        super();
        this.handler=handler;
        this.statementMaker=sql;
        variableBindings=handler.bindings;
        // variableNameToNodes=variableBindings.variableNameToNodeMap;
        setupOperatorMap();
    }
    
    public static Collection logSqlExpressions=null;
    
    public String translateToString(Expression e) {
        Result r=translate(e);
        if (r==null) {
            Logger.instance().debug("No sql expression 1");
            return null;
        }
        if ((r.getType() & BoolType) != 0) {
            Logger.instance().debug("SQL expression:" + r.getString());
            String res=r.getString();
            if (logSqlExpressions!=null)
                logSqlExpressions.add(res);
            return res;
        }
        Logger.instance().debug("No sql expression 2");
        return null;
    }
    
    /**
     * Translates a Jena RDQL Expression into an SQL expression.
     * We should try to resolve the strongest SQL condition possible.
     * Maybe for each expression we should calculate both the strongest and the weakest
     * condition, so that negation flips the meaning.
     * There is one generic method for operators, that do not need special handling.
     * For others, such as logical operators (and, or, not) 
     * and d2rq-map items (variables, values), there are custom methods that match by type.
     *  
     * @param e
     * @return null if no 1:1 translation possible.
     * @see com.hp.hpl.jena.graph.query.Expression
     */
    public Result translate(Expression e) {
        // in absense of usable parameter overloading...
        if (e instanceof ParsedLiteral)
            return translateParsedLiteral((ParsedLiteral)e);
        if (e instanceof Q_Var)
            return translateQ_Var((Q_Var)e);
        if (e instanceof Expression.Variable)
            return translateExprVariable((Expression.Variable)e);
        if (e instanceof WorkingVar)
            return translateWorkingVar((WorkingVar)e);
        if (e instanceof Q_LogicalAnd)
            return translateAnd((Q_LogicalAnd)e);
        if (e instanceof Q_LogicalOr)
            return translateOr((Q_LogicalOr)e);
        if (e instanceof Q_UnaryNot)
            return translateNot((Q_UnaryNot)e);
             
        // otherwise a functional operator
        OperatorMap op=getSQLOp(e); // see at bottom
        if (op==null)
            return null;
        int args=e.argCount();
        if (op.unary) {
            if (args!=1) 
                return null;
            else 
                return translateUnary(op,e);
        } else {
            if (args<=1) 
                return null;
            else
                return translateBinary(op,e);
        }
    }
    
    public Result translateBinary(OperatorMap op, Expression e) {
        List list=translateArgs(e, true, null);
        if (list==null)
            return null;
        return op.applyList(list);              
    }
    
    /** 
     * no operator checking is performed
     * @param op
     * @param e
     * @return
     */
    public Result translateUnary(OperatorMap op, Expression e) {
        Expression ex=e.getArg(0);
        Result exStr=translate(ex);
        if (exStr==null)
            return null;
        Result result=op.applyUnary(exStr);
        return result;
    }

    
    /**
     * @param e
     * @return
     */
    public Result translate(ParsedLiteral e) {
        return translateParsedLiteral(e);
    }
    public Result translateParsedLiteral(ParsedLiteral e) {
        StringBuffer b=new StringBuffer();
        if (e.isInt())
            return newResult(Long.toString(e.getInt()),NumberType);
        else if (e.isDouble())
            return newResult(Double.toString(e.getDouble()),NumberType);
        else if (e.isBoolean())
            return e.getBoolean()? trueBuffer : falseBuffer ;
        // else if (e.isNode())   
        // else if (e.isURI())
        else if (e.isString())
            return translateString(e.getString());
        else 
            return null;
    }
    
    public Result translateString(String s) {
        return translateString(s,StringType);
    }
    public Result translateString(String s, int resultType) {
        StringBuffer b=new StringBuffer("\'");
        b.append(SQLStatementMaker.escape(s));
        b.append("\'");
        return newResult(b,resultType);
    }
    
    // the case where "?x=?y" should be handled before triples are checked for shared variables
    /**
     * translate a variable.
     * To translate a variable, we must either resolve its value or a reference to a column
     * @param var
     * @return
     */
    public Result translate(Q_Var var) {
        return translateQ_Var(var);
    }
    public Result translateQ_Var(Q_Var var) {
        if (variableBindings.isBound(var.getName()))
            return translateValue(var.eval(null,variableBindings.inputDomain));
        else
            return translateVarName(var.getName());
    }
    public Result translate(Expression.Variable var) {
        return translateExprVariable(var);
    }
    public Result translateExprVariable(Expression.Variable var) {
        return translateVarName(var.getName());
    }
    public Result translate(WorkingVar var) {
        return translateWorkingVar(var);
    }
    public Result translateWorkingVar(WorkingVar var) {
        //throw new RuntimeException("WorkingVar in RDQLExpressionTranslator");
        return translateValue(var); 
    }
    
    public Result translateValue(NodeValue val) {
        if ( val.isInt() ) {
            return newResult(Long.toString(val.getInt()),NumberType);
        } else if ( val.isDouble() ) {
            return newResult(Double.toString(val.getDouble()),NumberType);
        } else if (val.isBoolean()) {
            return (val.getBoolean() ? trueBuffer : falseBuffer);
         } else if (val.isURI()) {
            return translateString(val.getURI(),UriType);
        } else if (val.isString()) {
            return translateString(val.getString(),StringType);
        } else if (val.isNode()) {
            // Graph
        }
        return null;
    }
    
    public Result translateVarName(String varName) {
        // map var to a column-expression
        // we should choose the one that is most simple
        NodeConstraint c=(NodeConstraint)variableNameToNodeConstraint.get(varName);
        if (c==null) {
            Node n=(Node) variableBindings.variableNameToNodeMap.get(varName);
            c=(NodeConstraint)handler.variableToConstraint.get(n);
            if (c==null) {
                Set varIndexSet=(Set)variableBindings.bindVariableToShared.get(n);
                ConstraintHandler.NodeMakerIterator e=handler.makeNodeMakerIterator(varIndexSet);
                if (e.hasNext()) { // it is not shared, so we do not have to check next occourence
                    NodeMaker m=e.nextNodeMaker();
                    c=new NodeConstraint();
                    m.matchConstraint(c);
                } else
                    return null;
            }
            variableNameToNodeConstraint.put(varName,c);
        }
        return translateNodeConstraint(c);       
    }
    
    public Result translateNodeConstraint(NodeConstraint c) {
        Iterator it;
        if (!weaker)
            return null;
        if (c.fixedNode!=null) {
            return translateNode(c.fixedNode);
        }
        it=c.columns.iterator();
        while (it.hasNext()) {
            Column col=(Column)it.next();
            Result res=translateColumn(col);
            if (res!=null)
                return res;
        }
        it=c.patterns.iterator();
        while (it.hasNext()) {
            Pattern pat=(Pattern)it.next();
            Result res=translatePattern(pat);
            if (res!=null)
                return res;
        }
        return null;
    }
    
    public Result translateColumn(Column col) {
        String columnName=col.getQualifiedName();
        int columnType=statementMaker.columnType(col);
        if (columnType==Database.numericColumnType) {
            return newResult(columnName,NumberType);
        } else if (columnType==Database.textColumnType) {
            return newResult(columnName,StringType);
        } 
        return null;
    }
    
    public Result castToString(Result r) {
        if (r.getType()==StringType)
            return r;
        StringBuffer sb=new StringBuffer("CAST(");
        r.appendTo(sb);
        sb.append(" AS SQL_TEXT)"); // SQL 92
        //sb.append(" AS char)"); // mysql
        return newResult(sb,StringType);
    }
    
    public final static String concatenateOp="Concatenate";
    public Result translatePattern(Pattern p) {
        OperatorMap op=getSQLOp(concatenateOp);
        if (op==null)
            return null;
        Iterator it=p.partsIterator();
        int i=0;
        List list=new ArrayList();
        while (it.hasNext()) {
            i++;
            if (i%2==1) {
                String literalPart=(String)it.next();
                Result escaped=translateString(literalPart);
                list.add(escaped);
            } else {
                Column c=(Column)it.next();
                Result res=castToString(translateColumn(c));
                if (res==null)
                    return null;
                list.add(res);
            }
        }
        return op.applyList(list);
    }
    
    public Result translateNode(Node n) {
        if (n.isLiteral()) {
            LiteralLabel label=n.getLiteral();
            String dType=label.getDatatypeURI();
            String str=label.getValue().toString();
            String lang=label.language();
            if (dType!=null) {
                if ("http://www.w3.org/2001/XMLSchema#int".equals(dType) || 
                        "http://www.w3.org/2001/XMLSchema#float".equals(dType) || 
                        "http://www.w3.org/2001/XMLSchema#double".equals(dType)) {
                    return newResult(str,NumberType);
                } else if ("http://www.w3.org/2001/XMLSchema#string".equals(dType)) {
                    return newResult(SQLStatementMaker.escape(str),StringType);
                }
                return null;
            } else if (lang!=null && !"".equals(lang)) {
                return null;
            }
            return newResult(SQLStatementMaker.escape(str),StringType);
        } else if (n.isURI()) {
            String str=n.getURI();
            return newResult(str,UriType);
        }
        return null;
    }
    
    
    /**
     * translates an RDQL expression
     * @param e the RDQL expression
     * @param strict if set, all arguments must be translatable to create a complex SQL expression
     * @param neutral the neutral element of the current operation
     * @return
     */
    public List translateArgs(Expression e, boolean strict, Result neutral) {
        List list=new ArrayList();
        int count=e.argCount();
        for (int i=0; i<count;i++) {
           Expression ex=e.getArg(i);
           Result exStr=translate(ex);
           if (strict && exStr==null)
               return null;
           if ((exStr!=null) && (exStr!=neutral))
               list.add(exStr);
        }
        return list;
    }
    
    public static Result trueBuffer=newResult("true",BoolType);
    public static Result falseBuffer=newResult("false",BoolType);
        
    public Result translate(Q_LogicalAnd e) {
        return translateAnd(e);
    }

    public Result translateAnd(Q_LogicalAnd e) {
        OperatorMap op=getSQLOp(e); // see at bottom
        List list=translateArgs(e, false, trueBuffer);
        if (list==null)
            return null;
        int count=list.size();
        if (!weaker && (e.argCount()>count)) // less is weaker!
            return null;
        if (count==0)
            return trueBuffer;
        if (count==1)
            return (Result) list.get(0);
        return op.applyList(list);        
    }
    
    public Result translate(Q_LogicalOr e) {
        return translateOr(e);
    }
    public Result translateOr(Q_LogicalOr e) {
        OperatorMap op=getSQLOp(e); // see at bottom
        List list=translateArgs(e, true, falseBuffer);
        if (list==null)
            return null;
        int count=list.size();
        if (weaker && (e.argCount()>count)) // less args is stronger!
            return null;
        if (count==0)
            return falseBuffer;
        if (count==1)
            return (Result) list.get(0);
        return op.applyList(list);        
    }
    
    /**
     * is this really the logical Not or bit not or both?
     * @param e
     * @return
     */
    public Result translate(Q_UnaryNot e) {
        return translateNot(e);
    }
    public Result translateNot(Q_UnaryNot e) {
        OperatorMap op=getSQLOp(e); // see at bottom
        boolean oldWeaker=weaker;
        weaker=!weaker;
        Expression ex=e.getArg(0);
        weaker=oldWeaker;
        Result exStr=translate(ex);
        Result result=null;
        if (exStr!=null) {
            if (exStr==trueBuffer)
                result=falseBuffer;
            else if (exStr==falseBuffer)
                result=trueBuffer;
            else 
                result=op.applyUnary(exStr);
        }
        return result;
    }
    
/*
    public Result translate(Q_UnaryMinus e) {
        return translateUnary(e);
    }
    public Result translate(Q_UnaryPlus e) {
        return translateUnary(e);
    }
*/
    
    /////////////   Massive Mapping /////////////////////////////
    
    // From rdql.parser.ExprNode
    static final String exprBaseURI = "urn:x-jena:expr:" ; 
    String constructURI(String className)
    {
        if ( className.lastIndexOf('.') > -1 )
            className = className.substring(className.lastIndexOf('.')+1) ;
        return exprBaseURI+className ;
    }
    String opURL(Object obj) {
        return constructURI(obj.getClass().getName());
    }
   
    OperatorMap putOp(String className,String sqlOp) {
        return putOp(className,sqlOp,NoType);
    }
    OperatorMap putOp(String className,String sqlOp, int argTypes) {
        return putOp(className, sqlOp, argTypes, argTypes, SameType, LeftType);
    }        
    OperatorMap putOp(String className,String sqlOp, int argTypes, int returnType) {
        return putOp(className, sqlOp, argTypes, argTypes, SameType, returnType);
    }        
    OperatorMap putOp(String className,String sqlOp, int leftTypes, int rightTypes, 
                int leftRightConstraint, int returnType) {
        if (sqlOp==null) 
            return null;
        String url=constructURI(className);
        OperatorMap op=new OperatorMap();
        opMap.put(url,op);
        op.rdqlOperator=url;
        op.sqlOperator=sqlOp;
        // op.argTypes=argType;
        op.sameType=(leftRightConstraint==SameType);
        op.leftTypes=leftTypes;
        op.rightTypes=rightTypes;
        op.returnType=returnType;
        return op;
    }
    /*
    OperatorMap getOp(String className) {
        return (OperatorMap)opMap.get(constructURI(className));
    }
    OperatorMap getOp(Object obj) {
        return (OperatorMap)opMap.get(opURL(obj));
    }
    */
    
    OperatorMap getSQLOp(String className) {
        String fun=constructURI(className);
        OperatorMap op=(OperatorMap)opMap.get(fun);
        return op; // .sqlOperator;
   }
    OperatorMap getSQLOp(Object e) { // Expression e) {
        // String fun=e.getFun(); sometimes returns exprBaseURI:..., 
        // sometimes the fully qualified classname. 
        // So we skip this "feature"
        String fun=opURL(e); 
       OperatorMap op=(OperatorMap)opMap.get(fun);
        return op; // .sqlOperator;
    }
    
    /**
     * Defines operator maps between RDQL classes and SQL operators or functions.
     * null means: no mapping.
     */
    void setupOperatorMap() {
        int NumberStringType=NumberType+StringType;
        if (opMap==null) {
            opMap=new HashMap();
            putOp("Q_Add","+",NumberType);
            putOp("Q_BitAnd",null,NumberType);
            putOp("Q_BitOr",null,NumberType); // "||"
            putOp("Q_BitXor",null,NumberType);
            putOp("Q_Divide","/",NumberType);
            putOp("Q_Equal","=",AnyType,AnyType,SameType,BoolType);
            putOp("Q_GreaterThan",">",NumberType,BoolType);
            putOp("Q_GreaterThanOrEqual",">=",NumberType,BoolType);
            putOp("Q_LeftShift",null,NumberType);
            putOp("Q_LessThan","<",NumberType,BoolType);
            putOp("Q_LessThanOrEqual","<=",NumberType,BoolType);
            putOp("Q_LogicalAnd","AND",BoolType,BoolType);
            putOp("Q_LogicalOr","OR",BoolType,BoolType);
            putOp("Q_Modulus",null);
            putOp("Q_Multiply","*",NumberType);
            putOp("Q_NotEqual","<>",AnyType,AnyType,SameType,BoolType);
            putOp("Q_PatternLiteral",null);
            putOp("Q_RightSignedShift",null);
            putOp("Q_RightUnsignedShift",null);
            putOp("Q_StringEqual","=",StringType,StringType,SameType,BoolType);
            putOp("Q_StringLangEqual",null);
            putOp("Q_StringMatch",null); // "LIKE"
            putOp("Q_StringNoMatch",null); // "NOT LIKE"
            putOp("Q_Subtract","-",NumberType);
            putOp("Q_UnaryMinus","-",NumberType).unary=true;
            putOp("Q_UnaryNot","NOT",BoolType).unary=true;
            putOp("Q_UnaryPlus","+",NumberType).unary=true;
            // putOp(concatenateOp,"||",StringType); // SQL 92
            putOp(concatenateOp,"CONCAT",StringType).functional=true; // mySQL
        }
    }
    
    // Auxiliary constructs
 
    /**
     * A Result is an auxiliary construct used with the ExpressionTranslator class only.
     * The Result of a translation is a SQL expression (string) that will have a
     * type when evaluated by the database. The type information is necessary for
     * expression casting.
     * @author jgarbers
     *
     */
    public interface Result {
        public int getType(); // a concrete type, not a combination of types!
        public void setType(int type);
        public String getString();
        public StringBuffer getStringBuffer();
        public void appendTo(StringBuffer sb);
    }

    static public Result newResult(String expr, int type) {
        return new SQLExpr(expr,type);
        // return new StringResult(expr,type);
    }
    static public Result newResult(StringBuffer expr, int type) {
        return new SQLExpr(expr,type);
        // return new StringBufferResult(expr,type);
    }

}
