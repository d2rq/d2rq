package de.fuberlin.wiwiss.d2rq.optimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.sparql.expr.E_Function;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.NodeRelation;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.engine.GraphPatternTranslator;
import de.fuberlin.wiwiss.d2rq.engine.MapFixture;
import de.fuberlin.wiwiss.d2rq.expr.AttributeExpr;
import de.fuberlin.wiwiss.d2rq.expr.Constant;
import de.fuberlin.wiwiss.d2rq.expr.TextMatches;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.optimizer.expr.TransformExprToSQLApplyer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.DummyDB;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType.GenericType;
import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class TextMatchesExprTransformTest extends TestCase {

	private static final String FUNCTION_URI = "http://d2rq.org/function#textMatches";
	private static final String MAPPING_FILE = "optimizer/filtertests.n3";

	public void testTexualColumn() {
		Collection<NodeRelation> rels = translate(pattern(), MAPPING_FILE, new DummyDB(Vendor.PostgreSQL));

		NodeRelation label = search("table1", "label", rels);

		TextMatches expected = 
			new TextMatches(
				new AttributeExpr(new Attribute(null, "table1", "label")),
				new Constant("something"));

		Expression result = TransformExprToSQLApplyer.convert(textMatchesExpr(), label);

		assertEquals(expected, result);
	}

	public void testNonTextualColumn() {
		HashMap<String,GenericType> columnTypes = new HashMap<String,GenericType>();
		columnTypes.put("table1.label", GenericType.NUMERIC);

		Collection<NodeRelation> rels = translate(pattern(), MAPPING_FILE, new DummyDB(Vendor.PostgreSQL, columnTypes));

		NodeRelation label = search("table1", "label", rels);

		Expression result = TransformExprToSQLApplyer.convert(textMatchesExpr(), label);

		assertEquals(Expression.FALSE, result);
	}

	public void testConstantValue() {
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), RDF.type.asNode(), Node.createVariable("o")));

		Collection<NodeRelation> rels = translate(pattern, MAPPING_FILE, new DummyDB(Vendor.PostgreSQL));

		NodeRelation type = search("table1", "id", rels);

		Expression result = TransformExprToSQLApplyer.convert(textMatchesExpr(), type);

		assertEquals(Expression.FALSE, result);
	}

	public void testSQLExpressionColumn() {
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), Node.createURI("http://example.org/computed"), Node.createVariable("o")));

		Collection<NodeRelation> rels = translate(pattern, MAPPING_FILE, new DummyDB(Vendor.PostgreSQL));

		NodeRelation value = rels.iterator().next();

		Expression result = TransformExprToSQLApplyer.convert(textMatchesExpr(), value);

		assertEquals(Expression.FALSE, result);
	}

	public void testUnsupportedDatabase() {
		Collection<NodeRelation> rels = translate(pattern(), MAPPING_FILE, new DummyDB(Vendor.SQL92));

		NodeRelation label = search("table1", "label", rels);

		try {
			TransformExprToSQLApplyer.convert(textMatchesExpr(), label);
			fail("Should have failed because database does not support free text search");
		} catch (D2RQException e) {
			assertEquals(D2RQException.DATABASE_DOES_NOT_SUPPORT_FREE_TEXT_SEARCH, e.errorCode());
		}
	}

	public void testQueryIsVariable() {
		testInvalidArgumentTypes(new ExprVar("o"), new ExprVar("q"));
	}

	public void testQueryIsNoString() {
		testInvalidArgumentTypes(new ExprVar("o"), NodeValue.makeNodeInteger(1));
	}

	public void testNoVariable() {
		testInvalidArgumentTypes(NodeValue.makeNodeString("literal text"), NodeValue.makeNodeString("match"));
	}

	private void testInvalidArgumentTypes(Expr arg1, Expr arg2) {
		Collection<NodeRelation> rels = translate(pattern(), MAPPING_FILE, new DummyDB(Vendor.PostgreSQL));

		NodeRelation label = search("table1", "label", rels);

		try {
			ExprList arguments = new ExprList();
			arguments.add(arg1);
			arguments.add(arg2);
			Expr textMatches = new E_Function(FUNCTION_URI, arguments);
			TransformExprToSQLApplyer.convert(textMatches, label);
			fail("Should have failed because invalid argument types");
		} catch (QueryBuildException e) {
			assertEquals("Function 'textMatches' takes a variable and a string", e.getMessage());
		}
	}

	public void testNoArguments() {
		testInvalidNumberOfArguments(new ExprList());
	}

	public void test1Argument() {
		testInvalidNumberOfArguments(new ExprList(new ExprVar("o")));
	}

	public void test3Arguments() {
		ExprList arguments = new ExprList(textMatchesExpr().getArgs());
		arguments.add(new ExprVar("x"));
		testInvalidNumberOfArguments(arguments);
	}

	private void testInvalidNumberOfArguments(ExprList arguments) {
		Collection<NodeRelation> rels = translate(pattern(), MAPPING_FILE, new DummyDB(Vendor.PostgreSQL));

		NodeRelation label = search("table1", "label", rels);
		E_Function textMatches = new E_Function(FUNCTION_URI, arguments);

		try {
			TransformExprToSQLApplyer.convert(textMatches, label);
			fail("Should have failed because wrong number of arguments given");
		} catch (QueryBuildException e) {
			assertEquals("Function 'textMatches' takes two arguments", e.getMessage());
		}
	}

	private List<Triple> pattern() {
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), RDFS.label.asNode(), Node.createVariable("o")));
		return pattern;
	}

	private E_Function textMatchesExpr() {
		ExprList args = new ExprList();
		args.add(new ExprVar("o"));
		args.add(NodeValue.makeNodeString("something"));
		return new E_Function(FUNCTION_URI, args);
	}

	private Collection<NodeRelation> translate(List<Triple> pattern, String mappingFile, ConnectedDB database) {
		return new GraphPatternTranslator(pattern, MapFixture.loadPropertyBridges(mappingFile, database), true).translate();
	}

	private NodeRelation search(String tableName, String attributeName, Collection<NodeRelation> relations) {
		for (NodeRelation rel : relations) {
			for (ProjectionSpec p: rel.baseRelation().projections()) {
				Attribute attribute = (Attribute) p;
				if (attribute.tableName().equals(tableName) && attribute.attributeName().equals(attributeName))
					return rel;
			}
		}
		
		return null;
	}
}
