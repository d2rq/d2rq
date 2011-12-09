package de.fuberlin.wiwiss.d2rq.optimizer.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.E_Add;
import com.hp.hpl.jena.sparql.expr.E_Datatype;
import com.hp.hpl.jena.sparql.expr.E_Divide;
import com.hp.hpl.jena.sparql.expr.E_Equals;
import com.hp.hpl.jena.sparql.expr.E_GreaterThan;
import com.hp.hpl.jena.sparql.expr.E_GreaterThanOrEqual;
import com.hp.hpl.jena.sparql.expr.E_IsBlank;
import com.hp.hpl.jena.sparql.expr.E_IsIRI;
import com.hp.hpl.jena.sparql.expr.E_IsLiteral;
import com.hp.hpl.jena.sparql.expr.E_Lang;
import com.hp.hpl.jena.sparql.expr.E_LangMatches;
import com.hp.hpl.jena.sparql.expr.E_LessThan;
import com.hp.hpl.jena.sparql.expr.E_LessThanOrEqual;
import com.hp.hpl.jena.sparql.expr.E_LogicalNot;
import com.hp.hpl.jena.sparql.expr.E_LogicalOr;
import com.hp.hpl.jena.sparql.expr.E_Multiply;
import com.hp.hpl.jena.sparql.expr.E_NotEquals;
import com.hp.hpl.jena.sparql.expr.E_SameTerm;
import com.hp.hpl.jena.sparql.expr.E_Str;
import com.hp.hpl.jena.sparql.expr.E_Subtract;
import com.hp.hpl.jena.sparql.expr.E_UnaryMinus;
import com.hp.hpl.jena.sparql.expr.E_UnaryPlus;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprFunction;
import com.hp.hpl.jena.sparql.expr.ExprFunction1;
import com.hp.hpl.jena.sparql.expr.ExprFunction2;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.ExprVisitor;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.expr.nodevalue.NodeFunctions;
import com.hp.hpl.jena.sparql.expr.nodevalue.NodeValueBoolean;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ExpressionProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.RelationalOperators;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.expr.Add;
import de.fuberlin.wiwiss.d2rq.expr.Constant;
import de.fuberlin.wiwiss.d2rq.expr.Divide;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.GreaterThan;
import de.fuberlin.wiwiss.d2rq.expr.GreaterThanOrEqual;
import de.fuberlin.wiwiss.d2rq.expr.LessThan;
import de.fuberlin.wiwiss.d2rq.expr.LessThanOrEqual;
import de.fuberlin.wiwiss.d2rq.expr.Multiply;
import de.fuberlin.wiwiss.d2rq.expr.Negation;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.expr.Subtract;
import de.fuberlin.wiwiss.d2rq.expr.UnaryMinus;
import de.fuberlin.wiwiss.d2rq.nodes.DetermineNodeType;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilterImpl;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;

/**
 * Attempts to transform a SPARQL FILTER Expr to a SQL Expression
 * 
 * Notes:
 * <ul>
 * <li>Literals using d2rq:pattern cannot be compared.</li>
 * <li>No XSD type checking/conversion or constructor functions yet.</li>
 * </ul>
 * 
 * @author Herwig Leimer
 * @author Giovanni Mels
 */
public final class TransformExprToSQLApplyer implements ExprVisitor {
	
	private static final Logger logger = LoggerFactory.getLogger(TransformExprToSQLApplyer.class);
	
	// TODO Expression.FALSE and Expression.TRUE are not constants
	private static final Expression CONSTANT_FALSE = new ConstantEx("false", NodeValueBoolean.FALSE.asNode());
	private static final Expression CONSTANT_TRUE  = new ConstantEx("true", NodeValueBoolean.TRUE.asNode());
	
	private final NodeRelation nodeRelation;
	private final Stack expression = new Stack();
	
	private boolean convertable;     // flag if converting was possible
	private String  reason = null;   // reason why converting failed
	
	/**
	 * Creates an expression transformer.
	 * 
	 * @param nodeRelation
	 */
	public TransformExprToSQLApplyer(NodeRelation nodeRelation)
	{	
		this.convertable  = true;
		this.nodeRelation = nodeRelation;
	}
	
	public void startVisit()
	{
		logger.debug("transform started");
	}
	
	public void finishVisit()
	{
		logger.debug("transform finished");
	}
	

	public void visit(ExprFunction function)
	{
		logger.debug("visit ExprFunction {}", function);
		
		if (!convertable) {
			expression.push(Expression.FALSE); // prevent stack empty exceptions when conversion
			return;                            // fails in the middle of a multi-arg operator conversion
		}
		
		if (function instanceof ExprFunction1) {
			convertFunction((ExprFunction1) function);
		} else if (function instanceof ExprFunction2) {
			convertFunction((ExprFunction2) function);
		} else if (extensionSupports(function)) {
			for (int i = 0; i < function.numArgs(); i++)
				function.getArg(i + 1).visit(this);
			List args = new ArrayList(function.numArgs());
			
			for (int i = 0; i < function.numArgs(); i++)
				args.add(expression.pop());
			Collections.reverse(args);
			extensionConvert(function, args);
		} else {
			conversionFailed(function);
		}
	}
	
	
	public void visit(ExprVar var)
	{
		logger.debug("visit ExprVar {}", var);
		
		if (!convertable) {
			expression.push(Expression.FALSE); // prevent stack empty exceptions when conversion
			return;                            // fails in the middle of a multi-arg operator conversion
		}
		
		String varName = var.getVarName();
		
		// if expression contains a blank node, no conversion to sql can be done
		if (Var.isBlankNodeVarName(varName)) {
			conversionFailed("blank nodes not supported", var);
			return;
		}
		
		List expressions = toExpression(var);
		if (expressions.size() == 1) {
			expression.push((Expression) expressions.get(0));
		} else {
			// no single sql-column for sparql-var does exist break up conversion
			// (the case for Pattern ValueMakers)
			conversionFailed("multi column pattern valuemakers not supported", var);
		}
	}
	
	
	public void visit(NodeValue value)
	{
		logger.debug("visit NodeValue {}", value);
		
		if (!convertable) {
			expression.push(Expression.FALSE); // prevent stack empty exceptions when conversion
			return;                            // fails in the middle of a multi-arg operator conversion
		}
		
		if (value.isDecimal() || value.isDouble() || value.isFloat() || value.isInteger() || value.isNumber()) {
			expression.push(new ConstantEx(value.asString(), value.asNode()));
		} else if (value.isDateTime()) {
			// Convert xsd:dateTime: CCYY-MM-DDThh:mm:ss
			// to SQL-92 TIMESTAMP: CCYY-MM-DD hh:mm:ss[.fraction]
			// TODO support time zones (WITH TIME ZONE columns)
			expression.push(new ConstantEx(value.asString().replace("T", " "), value.asNode()));
		} else {
			expression.push(new ConstantEx(value.asString(), value.asNode()));
		}
	}
	
	/**
	 * Delivers the corresponding sql-expression for a sparql-var
	 * @param exprVar - a sparql-expr-var
	 * @return List<Expression> - the equivalent sql-expressions
	 */
	private List toExpression(ExprVar exprVar)
	{
		ArrayList result = new ArrayList();
		
		if (this.nodeRelation != null && exprVar != null)
		{
			// get the nodemaker for the expr-var
			NodeMaker nodeMaker = nodeRelation.nodeMaker(exprVar.asVar().getVarName());
			if (nodeMaker instanceof TypedNodeMaker) {
				TypedNodeMaker typedNodeMaker = (TypedNodeMaker) nodeMaker;
				Iterator it = typedNodeMaker.projectionSpecs().iterator();
				if (!it.hasNext()) {
					logger.debug("no projection spec for {}, assuming constant", exprVar);
					Node node = typedNodeMaker.makeNode(null);
					result.add(new ConstantEx(NodeValue.makeNode(node).asString(), node));
				}
				while (it.hasNext()) {
					ProjectionSpec projectionSpec = (ProjectionSpec)it.next();
					
					if (projectionSpec == null)
						return Collections.EMPTY_LIST;
					
					if (projectionSpec instanceof Attribute) {
						result.add(new AttributeExprEx((Attribute) projectionSpec, nodeMaker));
					} else {
						// projectionSpec is a ExpressionProjectionSpec
						ExpressionProjectionSpec expressionProjectionSpec = (ExpressionProjectionSpec) projectionSpec;
						Expression expression = expressionProjectionSpec.toExpression();
						if (expression instanceof SQLExpression)
							result.add(((SQLExpression)expression));
						else
							return Collections.EMPTY_LIST;
					}
				}
			} else if (nodeMaker instanceof FixedNodeMaker) {
				FixedNodeMaker fixedNodeMaker = (FixedNodeMaker) nodeMaker;
				Node node = fixedNodeMaker.makeNode(null);
				result.add(new ConstantEx(NodeValue.makeNode(node).asString(), node));
			}
		}
		
		return result;
	}
	
	private void convertFunction(ExprFunction1 expr)
	{
		logger.debug("convertFunction {}", expr.toString());
		
		if (expr instanceof E_Str) {
			convertStr((E_Str) expr);
		} else if (expr instanceof E_IsIRI) {
			convertIsIRI((E_IsIRI) expr);
		} else if (expr instanceof E_IsBlank) {
			convertIsBlank((E_IsBlank) expr);
		} else if (expr instanceof E_IsLiteral) {
			convertIsLiteral((E_IsLiteral) expr);
		} else if (expr instanceof E_Datatype) {
			convertDataType((E_Datatype) expr);
		} else if (expr instanceof E_Lang) {
			convertLang((E_Lang) expr);
		} else if (expr instanceof E_LogicalNot) {
			convertLogicalNot((E_LogicalNot) expr);
		} else if (expr instanceof E_UnaryPlus) {
			convert((E_UnaryPlus) expr);
		} else if (expr instanceof E_UnaryMinus) {
			convert((E_UnaryMinus) expr);
		} else if (extensionSupports(expr)) {
			expr.getArg(1).visit(this) ;
			Expression e1 = (Expression) expression.pop();
			List args = Collections.singletonList(e1);
			extensionConvert(expr, args);
		} else {
			conversionFailed(expr);
		}
	}
	
	private void convertFunction(ExprFunction2 expr)
	{
		logger.debug("convertFunction {}", expr.toString());
		
		if (expr instanceof E_LogicalOr) {
			expr.getArg1().visit(this) ;
			expr.getArg2().visit(this) ;
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			expression.push(e1.or(e2));
		} else if (expr instanceof E_LessThan) {
			expr.getArg1().visit(this) ;
			expr.getArg2().visit(this) ;
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			expression.push(new LessThan(e1, e2));
		} else if (expr instanceof E_LessThanOrEqual) {
			expr.getArg1().visit(this) ;
			expr.getArg2().visit(this) ;
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			expression.push(new LessThanOrEqual(e1, e2));
		} else if (expr instanceof E_GreaterThan) {
			expr.getArg1().visit(this) ;
			expr.getArg2().visit(this) ;
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			expression.push(new GreaterThan(e1, e2));
		} else if (expr instanceof E_GreaterThanOrEqual) {
			expr.getArg1().visit(this) ;
			expr.getArg2().visit(this) ;
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			expression.push(new GreaterThanOrEqual(e1, e2));
		} else if (expr instanceof E_Add) {
			expr.getArg1().visit(this) ;
			expr.getArg2().visit(this) ;
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			expression.push(new Add(e1, e2));
		} else if (expr instanceof E_Subtract) {
			expr.getArg1().visit(this) ;
			expr.getArg2().visit(this) ;
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			expression.push(new Subtract(e1, e2));
		} else if (expr instanceof E_Multiply) {
			expr.getArg1().visit(this) ;
			expr.getArg2().visit(this) ;
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			expression.push(new Multiply(e1, e2));
		} else if (expr instanceof E_Divide) {
			expr.getArg1().visit(this) ;
			expr.getArg2().visit(this) ;
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			expression.push(new Divide(e1, e2));
		} else if (expr instanceof E_Equals) {
			convertEquals((E_Equals) expr);
		} else if (expr instanceof E_NotEquals) {
			convertNotEquals((E_NotEquals) expr);
		} else if (expr instanceof E_LangMatches) {
			convertLangMatches((E_LangMatches) expr);
		} else if (expr instanceof E_SameTerm) {
			convertSameTerm((E_SameTerm) expr);
		} else if (extensionSupports(expr)) {
			expr.getArg(1).visit(this);
			expr.getArg(2).visit(this);
			Expression e2 = (Expression) expression.pop();
			Expression e1 = (Expression) expression.pop();
			
			List args = new ArrayList(2);
			
			args.add(e1);
			args.add(e2);
			
			extensionConvert(expr, args);
		} else {
			conversionFailed(expr);
		}
	}
	
	/**
	 * Returns the sql Expression
	 * 
	 * @return the transformed sql expression if converting was possible, otherwise null.
	 */
	public Expression result()
	{ 
		if (!convertable) {
			logger.debug("filter conversion failed: {}", reason);
			return null;
		}
		
		if (expression.size() != 1)
			throw new IllegalStateException("something is seriously wrong");
		
		Expression result = (Expression) expression.pop();
		logger.debug("Resulting filter = {}", result);
		return result;
	}
	
	
	private void convertEquals(E_Equals expr)
	{
		logger.debug("convertEquals {}", expr.toString());
		
		convertEquality(expr);
	}
	
	
	private void convertEquality(ExprFunction2 expr)
	{
		expr.getArg1().visit(this);
		expr.getArg2().visit(this);
		Expression e2 = (Expression) expression.pop();
		Expression e1 = (Expression) expression.pop();
		
		// TODO Expression.FALSE and Expression.TRUE are not constants
		if (e1.equals(Expression.FALSE))
			e1 = CONSTANT_FALSE;
		else if (e1.equals(Expression.TRUE))
			e1 = CONSTANT_TRUE;
		if (e2.equals(Expression.FALSE))
			e2 = CONSTANT_FALSE;
		else if (e2.equals(Expression.TRUE))
			e2 = CONSTANT_TRUE;
		
		if (e1 instanceof AttributeExprEx && e2 instanceof Constant || e2 instanceof AttributeExprEx && e1 instanceof Constant) {
			
			AttributeExprEx variable;
			ConstantEx      constant;
			
			if (e1 instanceof AttributeExprEx) {
				variable = (AttributeExprEx) e1;
				constant = (ConstantEx) e2;
			} else {
				variable = (AttributeExprEx) e2;
				constant = (ConstantEx) e1;
			}
			
			logger.debug("isEqual({}, {})", variable, constant);
			
			NodeMaker nm = variable.getNodeMaker();
			
			if (nm instanceof TypedNodeMaker) {
				ValueMaker vm = ((TypedNodeMaker) nm).valueMaker();
				Node node = constant.getNode();
				logger.debug("checking {} with {}", node, nm);
				boolean empty = nm.selectNode(node, RelationalOperators.DUMMY).equals(NodeMaker.EMPTY);
				logger.debug("result {}", new Boolean(empty));
				if (!empty) {
					if (node.isURI())
						expression.push(vm.valueExpression(node.getURI()));
					else if (node.isLiteral())
						expression.push(vm.valueExpression(constant.value()));
					else
						conversionFailed(expr); // TODO blank nodes?
					return;
				} else {
					expression.push(Expression.FALSE);
					return;
				}
			} else {
				logger.warn("nm is not a TypedNodemaker");
			}
		} else if (e1 instanceof ConstantEx && e2 instanceof ConstantEx) {
			logger.debug("isEqual({}, {})", e1, e2);
			ConstantEx constant1 = (ConstantEx) e1;
			ConstantEx constant2 = (ConstantEx) e2;
			boolean equals = constant1.getNode().equals(constant2.getNode());
			logger.debug("constants equal? {}", new Boolean(equals));
			expression.push(equals ? Expression.TRUE : Expression.FALSE);
			return;
		} else if (e1 instanceof AttributeExprEx && e2 instanceof AttributeExprEx) {
			logger.debug("isEqual({}, {})", e1, e2);
			AttributeExprEx variable1 = (AttributeExprEx) e1;
			AttributeExprEx variable2 = (AttributeExprEx) e2;
			
			NodeMaker nm1 = variable1.getNodeMaker();
			NodeMaker nm2 = variable2.getNodeMaker();
			
			NodeSetFilterImpl nodeSet = new NodeSetFilterImpl();
			nm1.describeSelf(nodeSet);
			nm2.describeSelf(nodeSet);
			
			if (nodeSet.isEmpty()) {
				logger.debug("nodes {} {} incompatible", nm1, nm2);
				expression.push(Expression.FALSE);
				return;
			}
		}
		
		expression.push(Equality.create(e1, e2)); 
	}
	
	private void convertNotEquals(E_NotEquals expr)
	{
		logger.debug("convertNotEquals {}", expr.toString());
		
		expr.getArg1().visit(this);
		expr.getArg2().visit(this);
		Expression e2 = (Expression) expression.pop();
		Expression e1 = (Expression) expression.pop();
		
		// TODO Expression.FALSE and Expression.TRUE are not constants
		if (e1.equals(Expression.FALSE))
			e1 = CONSTANT_FALSE;
		else if (e1.equals(Expression.TRUE))
			e1 = CONSTANT_TRUE;
		if (e2.equals(Expression.FALSE))
			e2 = CONSTANT_FALSE;
		else if (e2.equals(Expression.TRUE))
			e2 = CONSTANT_TRUE;
		
		if (e1 instanceof AttributeExprEx && e2 instanceof Constant || e2 instanceof AttributeExprEx && e1 instanceof Constant) {
			AttributeExprEx variable;
			ConstantEx      constant;
			
			if (e1 instanceof AttributeExprEx) {
				variable = (AttributeExprEx) e1;
				constant = (ConstantEx) e2;
			} else {
				variable = (AttributeExprEx) e2;
				constant = (ConstantEx) e1;
			}
			
			logger.debug("isNotEqual({}, {})", variable, constant);
			
			NodeMaker nm = variable.getNodeMaker();
			
			if (nm instanceof TypedNodeMaker) {
				ValueMaker vm = ((TypedNodeMaker) nm).valueMaker();
				Node node = constant.getNode();
				logger.debug("checking {} with {}", node, nm);
				boolean empty = nm.selectNode(node, RelationalOperators.DUMMY).equals(NodeMaker.EMPTY);
				logger.debug("result {}", new Boolean(empty));
				if (!empty) {
					if (node.isURI())
						expression.push(new Negation(vm.valueExpression(node.getURI())));
					else if (node.isLiteral())
						expression.push(new Negation(vm.valueExpression(constant.value())));
					else
						conversionFailed(expr); // TODO blank nodes?
					return;
				} else {
					expression.push(Expression.TRUE);
					return;
				}
			}
		} else if (e1 instanceof ConstantEx && e2 instanceof ConstantEx) {
			logger.debug("isNotEqual({}, {})", e1, e2);
			boolean equals = e1.equals(e2);
			logger.debug("constants equal? {}", new Boolean(equals));
			expression.push(equals ? Expression.FALSE : Expression.TRUE);
			return;
		} else if (e1 instanceof AttributeExprEx && e2 instanceof AttributeExprEx) {
			logger.debug("isNotEqual({}, {})", e1, e2);
			AttributeExprEx variable1 = (AttributeExprEx) e1;
			AttributeExprEx variable2 = (AttributeExprEx) e2;
			
			NodeMaker nm1 = variable1.getNodeMaker();
			NodeMaker nm2 = variable2.getNodeMaker();
			
			NodeSetFilterImpl nodeSet = new NodeSetFilterImpl();
			nm1.describeSelf(nodeSet);
			nm2.describeSelf(nodeSet);
			
			if (nodeSet.isEmpty()) {
				logger.debug("nodes {} {} incompatible", nm1, nm2);
				expression.push(Expression.TRUE);
				return;
			}
		}
		
		expression.push(new Negation(Equality.create(e1, e2)));
	}
	
	private void convertLogicalNot(E_LogicalNot expr)
	{
		expr.getArg().visit(this);
		Expression e1 = (Expression) expression.pop();
		if (e1 instanceof Negation)
			expression.push(((Negation) e1).getBase());
		else
			expression.push(new Negation(e1));
	}
	
	private void convert(E_UnaryPlus expr)
	{
		expr.getArg().visit(this);
	}
	
	private void convert(E_UnaryMinus expr)
	{
		expr.getArg().visit(this);
		Expression e1 = (Expression) expression.pop();
		if (e1 instanceof UnaryMinus)
			expression.push(((UnaryMinus) e1).getBase());
		else
			expression.push(new UnaryMinus(e1));
	}
	
	/*
	 * See http://www.w3.org/TR/rdf-sparql-query § 11.4.2
	 * 
	 * "(ISIRI) Returns true if term is an IRI. Returns false otherwise."
	 * 
	 * @see http://www.w3.org/TR/rdf-sparql-query 
	 */
	private void convertIsIRI(E_IsIRI expr)
	{
		logger.debug("convertIsIRI {}", expr.toString());
		
		expr.getArg().visit(this);
		
		Expression arg = (Expression) expression.pop();
		
		if (arg instanceof AttributeExprEx) {
			AttributeExprEx variable = (AttributeExprEx) arg;
			NodeMaker nm = variable.getNodeMaker();
			DetermineNodeType filter = new DetermineNodeType();
			nm.describeSelf(filter);
			expression.push(filter.isLimittedToURIs() ? Expression.TRUE : Expression.FALSE);
		} else if (arg instanceof ConstantEx) {
			ConstantEx constant = (ConstantEx) arg;
			Node node = constant.getNode();
			expression.push(node.isLiteral() ? Expression.TRUE : Expression.FALSE);
		} else {
			conversionFailed(expr);
		}
	}
	
	/*
	 * See http://www.w3.org/TR/rdf-sparql-query § 11.4.3
	 * 
	 * "(ISBLANK) Returns true if term is a blank node. Returns false otherwise."
	 * 
	 * @see http://www.w3.org/TR/rdf-sparql-query 
	 */
	private void convertIsBlank(E_IsBlank expr)
	{
		logger.debug("convertIsBlank {}", expr.toString());
		
		expr.getArg().visit(this);
		
		Expression arg = (Expression) expression.pop();
		
		if (arg instanceof AttributeExprEx) {
			AttributeExprEx variable = (AttributeExprEx) arg;
			NodeMaker nm = variable.getNodeMaker();
			DetermineNodeType filter = new DetermineNodeType();
			nm.describeSelf(filter);
			expression.push(filter.isLimittedToBlankNodes() ? Expression.TRUE : Expression.FALSE);
		} else if (arg instanceof ConstantEx) {
			ConstantEx constant = (ConstantEx) arg;
			Node node = constant.getNode();
			expression.push(node.isBlank() ? Expression.TRUE : Expression.FALSE);
		} else {
			conversionFailed(expr);
		}
	}
	
	/*
	 * See http://www.w3.org/TR/rdf-sparql-query § 11.4.4
	 * 
	 * "(ISLITERAL) Returns true if term is a literal. Returns false otherwise."
	 * 
	 * @see http://www.w3.org/TR/rdf-sparql-query 
	 */
	private void convertIsLiteral(E_IsLiteral expr)
	{
		logger.debug("convertIsLiteral {}", expr.toString());
		
		expr.getArg().visit(this);
		
		Expression arg = (Expression) expression.pop();
		
		logger.debug("arg {} ", arg);
		
		if (arg instanceof AttributeExprEx) {
			AttributeExprEx variable = (AttributeExprEx) arg;
			NodeMaker nm = variable.getNodeMaker();
			
			DetermineNodeType filter = new DetermineNodeType();
			nm.describeSelf(filter);
			expression.push(filter.isLimittedToLiterals() ? Expression.TRUE : Expression.FALSE);
		} else if (arg instanceof ConstantEx) {
			ConstantEx constant = (ConstantEx) arg;
			Node node = constant.getNode();
			expression.push(node.isLiteral() ? Expression.TRUE : Expression.FALSE);
		} else {
			conversionFailed(expr);
		}
	}
	
	/*
	 * See http://www.w3.org/TR/rdf-sparql-query § 11.4.4
	 * 
	 * "(STR) Returns the lexical form of a literal; returns the codepoint representation of an IRI."
	 * 
	 * @see http://www.w3.org/TR/rdf-sparql-query 
	 */
	private void convertStr(E_Str expr)
	{
		logger.debug("convertStr {}", expr.toString());
		
		expr.getArg().visit(this);
		
		Expression arg = (Expression) expression.pop();
		
		if (arg instanceof AttributeExprEx) {
			// make a new AttributeExprEx with changed NodeMaker, which returns plain literal
			// TODO this seems to work, but needs more testing.
			AttributeExprEx attribute = (AttributeExprEx) arg;
			TypedNodeMaker nodeMaker = (TypedNodeMaker) attribute.getNodeMaker();
			TypedNodeMaker newNodeMaker = new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, nodeMaker.valueMaker(), nodeMaker.isUnique());
			logger.debug("changing nodemaker {} to {}", nodeMaker, newNodeMaker);
			expression.push(new AttributeExprEx((Attribute) attribute.attributes().iterator().next(), newNodeMaker));
		} else if (arg instanceof ConstantEx) {
			ConstantEx constant = (ConstantEx) arg;
			Node node = constant.getNode();
			String lexicalForm = node.getLiteral().getLexicalForm();
			node = Node.createLiteral(lexicalForm);
			ConstantEx constantEx = new ConstantEx(NodeValue.makeNode(node).asString(), node);
			logger.debug("pushing {}", constantEx);
			expression.push(constantEx);
		} else {
			conversionFailed(expr);
		}
	}
	
	/*
	 * See http://www.w3.org/TR/rdf-sparql-query § 11.4.6
	 * 
	 * "(LANG) Returns the language tag of ltrl, if it has one. It returns "" if ltrl has no language tag."
	 * @see http://www.w3.org/TR/rdf-sparql-query 
	 */
	private void convertLang(E_Lang expr)
	{
		logger.debug("convertLang {}", expr.toString());
		
		expr.getArg().visit(this);
		
		Expression arg = (Expression) expression.pop();
		
		if (arg instanceof AttributeExprEx) {
			AttributeExprEx variable = (AttributeExprEx) arg;
			NodeMaker nm = variable.getNodeMaker();
			DetermineNodeType filter = new DetermineNodeType();
			nm.describeSelf(filter);
			String lang = filter.getLanguage();
			logger.debug("lang {}", lang);
			if (lang == null)
				lang = ""; 
			
			// NodeValue.makeString(lang); TODO better?
			Node node = Node.createLiteral(lang);
			
			ConstantEx constantEx = new ConstantEx(NodeValue.makeNode(node).asString(), node);
			logger.debug("pushing {}", constantEx);
			expression.push(constantEx);
		} else if (arg instanceof ConstantEx) {
			ConstantEx constant = (ConstantEx) arg;
			Node node = constant.getNode();
			String lang = node.getLiteralLanguage();
			logger.debug("lang {}", lang);
			if (lang == null)
				lang = "";
			
			node = Node.createLiteral(lang);
			
			ConstantEx constantEx = new ConstantEx(NodeValue.makeNode(node).asString(), node);
			logger.debug("pushing {}", constantEx);
			expression.push(constantEx);
		} else {
			conversionFailed(expr);
		}
	}
	
	/*
	 * See http://www.w3.org/TR/rdf-sparql-query § 11.4.7
	 * 
	 * "(DATATYPE) Returns the datatype IRI of typedLit; returns xsd:string if the parameter is a simple literal."
	 * 
	 * @see http://www.w3.org/TR/rdf-sparql-query 
	 */
	private void convertDataType(E_Datatype expr)
	{
		logger.debug("convertDataType {}", expr.toString());
		
		expr.getArg().visit(this);
		
		Expression arg = (Expression) expression.pop();
		
		if (arg instanceof AttributeExprEx) {
			AttributeExprEx variable = (AttributeExprEx) arg;
			NodeMaker nm = variable.getNodeMaker();
			DetermineNodeType filter = new DetermineNodeType();
			nm.describeSelf(filter);
			if (!filter.isLimittedToLiterals()) {
				// type error, return false?
				logger.warn("type error: {} is not a literal, returning FALSE", variable);
				expression.push(Expression.FALSE);
				return;
			}
			RDFDatatype datatype = filter.getDatatype();
			logger.debug("datatype {}", datatype);
			
			Node node = Node.createURI((datatype != null) ? datatype.getURI() : XSDDatatype.XSDstring.getURI());
			
			ConstantEx constantEx = new ConstantEx(NodeValue.makeNode(node).asString(), node);
			logger.debug("pushing {}", constantEx);
			expression.push(constantEx);
		} else if (arg instanceof ConstantEx) {
			ConstantEx constant = (ConstantEx) arg;
			Node node = constant.getNode();
			if (!node.isLiteral()) {
				// type error, return false?
				logger.warn("type error: {} is not a literal, returning FALSE", node);
				expression.push(Expression.FALSE);
				return;
			}
			RDFDatatype datatype = node.getLiteralDatatype();
			logger.debug("datatype {}", datatype);
			node = Node.createURI((datatype != null) ? datatype.getURI() : XSDDatatype.XSDstring.getURI());
			ConstantEx constantEx = new ConstantEx(NodeValue.makeNode(node).asString(), node);
			logger.debug("pushing {}", constantEx);
			expression.push(constantEx);
		} else {
			conversionFailed(expr);
		}
	}
	
	/*
	 * See http://www.w3.org/TR/rdf-sparql-query § 11.4.11
	 * 
	 * "(SAMETERM) Returns TRUE if term1 and term2 are the same RDF term as defined
	 * in Resource Description Framework (RDF): Concepts and Abstract Syntax [CONCEPTS]; returns FALSE otherwise."
	 * 
	 * @see http://www.w3.org/TR/rdf-sparql-query
	 * @see http://www.w3.org/TR/rdf-concepts/
	 */
	private void convertSameTerm(E_SameTerm expr)
	{
		logger.debug("convertSameTerm {}", expr.toString());
		
		convertEquality(expr); // TODO this is probably not correct
	}
	
	/*
	 * See http://www.w3.org/TR/rdf-sparql-query § 11.4.12
	 * 
	 * "(LANGMATCHES) returns true if language-tag (first argument) matches language-range (second argument)
	 * per the basic filtering scheme defined in [RFC4647] section 3.3.1. language-range is a basic language
	 * range per Matching of Language Tags [RFC4647] section 2.1. A language-range of "*" matches any non-empty
	 * language-tag string."
	 * 
	 * @see http://www.w3.org/TR/rdf-sparql-query 
	 */
	private void convertLangMatches(E_LangMatches expr)
	{
		logger.debug("convertLangMatches {}", expr.toString());
		
		expr.getArg1().visit(this);
		expr.getArg2().visit(this);
		Expression e2 = (Expression) expression.pop();
		Expression e1 = (Expression) expression.pop();
		
		if (e1 instanceof ConstantEx && e2 instanceof ConstantEx) {
			ConstantEx lang1 = (ConstantEx) e1;
			ConstantEx lang2 = (ConstantEx) e2;
			NodeValue nv1 = NodeValue.makeString(lang1.getNode().getLiteral().getLexicalForm());
			NodeValue nv2 = NodeValue.makeString(lang2.getNode().getLiteral().getLexicalForm());
			NodeValue match = NodeFunctions.langMatches(nv1, nv2);
			expression.push(match.equals(NodeValue.TRUE) ? Expression.TRUE : Expression.FALSE);
		} else {
			expression.push(Expression.FALSE);
		}
	}
	
	
	private void conversionFailed(Expr unconvertableExpr)
	{
		// prevent stack empty exceptions when conversion fails in the middle of a multi-arg operator conversion
		expression.push(Expression.FALSE); 
		convertable = false;
		if (reason == null)
			reason = "cannot convert " + unconvertableExpr.toString();
	}
	
	private void conversionFailed(String message, Expr unconvertableExpr)
	{
		// prevent stack empty exceptions when conversion fails in the middle of a multi-arg operator conversion
		expression.push(Expression.FALSE);
		convertable = false;
		if (reason == null)
			reason = "cannot convert " + unconvertableExpr.toString() + ": " + message;
	}
	
	// extension mechanism
	

	protected boolean extensionSupports(ExprFunction function)
	{
		return false;
	}
	
	protected void extensionConvert(ExprFunction function, List args)
	{
	}
	
}
