package de.fuberlin.wiwiss.d2rq.optimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.E_Datatype;
import com.hp.hpl.jena.sparql.expr.E_Equals;
import com.hp.hpl.jena.sparql.expr.E_IsBlank;
import com.hp.hpl.jena.sparql.expr.E_IsIRI;
import com.hp.hpl.jena.sparql.expr.E_IsLiteral;
import com.hp.hpl.jena.sparql.expr.E_Lang;
import com.hp.hpl.jena.sparql.expr.E_LangMatches;
import com.hp.hpl.jena.sparql.expr.E_LogicalOr;
import com.hp.hpl.jena.sparql.expr.E_SameTerm;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.expr.nodevalue.NodeValueNode;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.NodeRelation;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.engine.GraphPatternTranslator;
import de.fuberlin.wiwiss.d2rq.engine.MapFixture;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.optimizer.expr.TransformExprToSQLApplyer;


public class ExprTransformTest2 extends TestCase {


	NodeRelation search(String tableName, String attributeName, NodeRelation[] relation)
	{
		for (int i = 0; i < relation.length; i++) {
			NodeRelation rel = relation[i];
			for (ProjectionSpec p: rel.baseRelation().projections()) {
				Attribute attribute = (Attribute) p;
				if (attribute.tableName().equals(tableName) && attribute.attributeName().equals(attributeName))
					return rel;
			}
		}
		
		return null;
	}
	
	
	public void testLang()
	{
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), RDFS.label.asNode(), Node.createVariable("o")));
		NodeRelation[] rels = translate(pattern, "optimizer/filtertests.n3");
		
		NodeRelation label_fr_be  = search("table1", "label_fr_be", rels);
		NodeRelation label_en     = search("table1", "label_en", rels);
		NodeRelation label_noLang = search("table1", "label", rels);
		
		Expr filterFR    = new E_Equals(new E_Lang(new ExprVar("o")), NodeValue.makeString("fr"));
		Expr filterEN_TAG_EN = new E_Equals(new E_Lang(new ExprVar("o")), NodeValue.makeNode("en", "en", (String) null));
		Expr filterFR_BE = new E_Equals(new E_Lang(new ExprVar("o")), NodeValue.makeString("fr-BE"));
		Expr filter      = new E_Equals(new E_Lang(new ExprVar("o")), NodeValue.makeString(""));
		
		assertEquals("LANG(label_fr_be) = \"fr\" should be FALSE",   Expression.FALSE, TransformExprToSQLApplyer.convert(filterFR, label_fr_be));
		assertEquals("LANG(label_en) = \"fr\" should be FALSE",      Expression.FALSE, TransformExprToSQLApplyer.convert(filterFR, label_en));
		assertEquals("LANG(label_fr_be) = \"fr_be\" should be TRUE", Expression.TRUE,  TransformExprToSQLApplyer.convert(filterFR_BE, label_fr_be));
		assertEquals("LANG(label_en) = \"en\"@en should be FALSE",   Expression.FALSE, TransformExprToSQLApplyer.convert(filterEN_TAG_EN, label_en));
		assertEquals("LANG(label_noLang) = \"\" should be TRUE",     Expression.TRUE,  TransformExprToSQLApplyer.convert(filter, label_noLang));
	}
	
	public void testLangMatches()
	{
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), RDFS.label.asNode(), Node.createVariable("o")));
		NodeRelation[] rels = translate(pattern, "optimizer/filtertests.n3");
		
		NodeRelation label_fr_be = search("table1", "label_fr_be", rels);
		NodeRelation label_en = search("table1", "label_en", rels);
		NodeRelation label = search("table1", "label", rels);
		
		Expr filterFR    = new E_LangMatches(new E_Lang(new ExprVar("o")), NodeValue.makeString("fr"));
		Expr filterEN    = new E_LangMatches(new E_Lang(new ExprVar("o")), NodeValue.makeString("en"));
		Expr filterFR_BE = new E_LangMatches(new E_Lang(new ExprVar("o")), NodeValue.makeString("fr-BE"));
		Expr filterALL   = new E_LangMatches(new E_Lang(new ExprVar("o")), NodeValue.makeString("*"));
		
		assertEquals("LANGMATCHES(LANG(label_fr_be), \"fr\") should be TRUE",   Expression.TRUE,  TransformExprToSQLApplyer.convert(filterFR, label_fr_be));
		assertEquals("LANGMATCHES(LANG(label_en), \"fr\") should be FALSE",     Expression.FALSE, TransformExprToSQLApplyer.convert(filterFR, label_en));
		assertEquals("LANGMATCHES(LANG(label_en), \"en\") should be TRUE",      Expression.TRUE,  TransformExprToSQLApplyer.convert(filterEN, label_en));
		assertEquals("LANGMATCHES(LANG(label_fr_BE, \"fr_BE\") should be TRUE", Expression.TRUE , TransformExprToSQLApplyer.convert(filterFR_BE, label_fr_be));
		
		assertEquals("LANGMATCHES(LANG(label), \"en\") should be FALSE",        Expression.FALSE, TransformExprToSQLApplyer.convert(filterEN, label));
		assertEquals("LANGMATCHES(LANG(label_fr_BE, \"*\") should be TRUE",     Expression.TRUE,  TransformExprToSQLApplyer.convert(filterALL, label_fr_be));
		assertEquals("LANGMATCHES(LANG(label_en, \"*\") should be TRUE",        Expression.TRUE,  TransformExprToSQLApplyer.convert(filterALL, label_en));
		assertEquals("LANGMATCHES(LANG(label, \"*\") should be FALSE",          Expression.FALSE, TransformExprToSQLApplyer.convert(filterALL, label));
	}
	
	public void testIsLiteral()
	{
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), RDFS.label.asNode(), Node.createVariable("o")));
		NodeRelation[] rels = translate(pattern, "optimizer/filtertests.n3");
				
		NodeRelation label = search("table1", "label", rels);
		NodeRelation label_en = search("table1", "label_en", rels);
		
		pattern.clear();
		pattern.add(Triple.create(Node.createVariable("s"), Node.createURI("http://example.org/value"), Node.createVariable("o")));
		rels = translate(pattern, "optimizer/filtertests.n3");
		
		NodeRelation intvalue = search("table2", "intvalue", rels);
		
		Expr subject = new E_IsLiteral(new ExprVar("s"));
		Expr object  = new E_IsLiteral(new ExprVar("o"));
		
		assertEquals("ISLITERAL(literal) should be TRUE",      Expression.TRUE,  TransformExprToSQLApplyer.convert(object, label));
		assertEquals("ISLITERAL(literal@lang) should be TRUE", Expression.TRUE,  TransformExprToSQLApplyer.convert(object, label_en));
		assertEquals("ISLITERAL(uri) should be FALSE",         Expression.FALSE, TransformExprToSQLApplyer.convert(subject, label));
		assertEquals("ISLITERAL(intvalue) should be TRUE",     Expression.TRUE,  TransformExprToSQLApplyer.convert(object, intvalue));
		assertEquals("ISLITERAL(blanknode) should be FALSE",   Expression.FALSE, TransformExprToSQLApplyer.convert(subject, intvalue));	
	}
		
	public void testIsIRI()
	{
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), RDFS.label.asNode(), Node.createVariable("o")));
		NodeRelation[] rels = translate(pattern, "optimizer/filtertests.n3");
				
		NodeRelation label = search("table1", "label", rels);
		NodeRelation label_en = search("table1", "label_en", rels);
		
		pattern.clear();
		pattern.add(Triple.create(Node.createVariable("s"), Node.createURI("http://example.org/value"), Node.createVariable("o")));
		rels = translate(pattern, "optimizer/filtertests.n3");
		
		NodeRelation intvalue = search("table2", "intvalue", rels);
		
		Expr subject = new E_IsIRI(new ExprVar("s"));
		Expr object  = new E_IsIRI(new ExprVar("o"));
		
		assertEquals("ISIRI(literal) should be FALSE",      Expression.FALSE, TransformExprToSQLApplyer.convert(object, label));
		assertEquals("ISIRI(literal@lang) should be FALSE", Expression.FALSE, TransformExprToSQLApplyer.convert(object, label_en));
		assertEquals("ISIRI(uri) should be TRUE",           Expression.TRUE,  TransformExprToSQLApplyer.convert(subject, label));
		assertEquals("ISIRI(intvalue) should be FALSE",     Expression.FALSE, TransformExprToSQLApplyer.convert(object, intvalue));
		assertEquals("ISIRI(blanknode) should be FALSE",    Expression.FALSE, TransformExprToSQLApplyer.convert(subject, intvalue));	
	}
	
	
	public void testIsBlank()
	{
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), RDFS.label.asNode(), Node.createVariable("o")));
		NodeRelation[] rels = translate(pattern, "optimizer/filtertests.n3");
				
		NodeRelation label = search("table1", "label", rels);
		NodeRelation label_en = search("table1", "label_en", rels);
		
		pattern.clear();
		pattern.add(Triple.create(Node.createVariable("s"), Node.createURI("http://example.org/value"), Node.createVariable("o")));
		rels = translate(pattern, "optimizer/filtertests.n3");
		
		NodeRelation intvalue = search("table2", "intvalue", rels);
		
		Expr subject = new E_IsBlank(new ExprVar("s"));
		Expr object  = new E_IsBlank(new ExprVar("o"));
		
		assertEquals("ISBLANK(literal) should be FALSE",      Expression.FALSE, TransformExprToSQLApplyer.convert(object, label));
		assertEquals("ISBLANK(literal@lang) should be FALSE", Expression.FALSE, TransformExprToSQLApplyer.convert(object, label_en));
		assertEquals("ISBLANK(uri) should be FALSE",          Expression.FALSE, TransformExprToSQLApplyer.convert(subject, label));
		assertEquals("ISBLANK(intvalue) should be FALSE",     Expression.FALSE, TransformExprToSQLApplyer.convert(object, intvalue));
		assertEquals("ISBLANK(blanknode) should be TRUE",     Expression.TRUE,  TransformExprToSQLApplyer.convert(subject, intvalue));	
	}
	
	public void testDataType()
	{
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), Node.createURI("http://example.org/value"), Node.createVariable("o")));
		NodeRelation[] rels = translate(pattern, "optimizer/filtertests.n3");
		
		NodeRelation intvalue = search("table2", "intvalue", rels);
		NodeRelation value = search("table2", "value", rels);
		
		pattern.clear();
		pattern.add(Triple.create(Node.createVariable("s"), RDFS.label.asNode(), Node.createVariable("o")));
		rels = translate(pattern, "optimizer/filtertests.n3");
		
		NodeRelation langliteral = search("table1", "label_en", rels);
		
		Expr filterint    = new E_Equals(new E_Datatype(new ExprVar("o")), NodeValueNode.makeNode(Node.createURI(XSDDatatype.XSDint.getURI())));
		Expr filterstring = new E_Equals(new E_Datatype(new ExprVar("o")), NodeValueNode.makeNode(Node.createURI(XSDDatatype.XSDstring.getURI())));
		
		assertEquals("DATATYPE(intliteral) = xsd:int should be TRUE",       Expression.TRUE, TransformExprToSQLApplyer.convert(filterint, intvalue));
		assertEquals("DATATYPE(simpleliteral) = xsd:string should be TRUE", Expression.TRUE, TransformExprToSQLApplyer.convert(filterstring, value));
		assertEquals("DATATYPE(langliteral) = xsd:string should be TRUE",   Expression.TRUE, TransformExprToSQLApplyer.convert(filterstring, langliteral));
	}
	
	public void testDisjunction()
	{
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), Node.createURI("http://example.org/value"), Node.createVariable("o")));
		NodeRelation[] rels = translate(pattern, "optimizer/filtertests.n3");
		
		NodeRelation intvalue = search("table2", "intvalue", rels);
	
		Expr disjunction = new E_LogicalOr(new E_Equals(new ExprVar("o"),  NodeValue.makeNode("1", XSDDatatype.XSDint)), new E_Equals(new ExprVar("o"), NodeValue.makeNode("2", XSDDatatype.XSDint)));
		
		Expression result = TransformExprToSQLApplyer.convert(disjunction, intvalue);
		TypedNodeMaker nm = (TypedNodeMaker) intvalue.nodeMaker(Var.alloc("o"));
		Expression e1 = nm.valueMaker().valueExpression("1");
		Expression e2 = nm.valueMaker().valueExpression("2");
		Expression expected = e1.or(e2);
		
		assertEquals("?o = \"1\"^^xsd:int || ?o = \"2\"^^xsd:int", expected, result);
	}
	
	public void testSameTerm()
	{
		List<Triple> pattern = new ArrayList<Triple>();
		pattern.add(Triple.create(Node.createVariable("s"), Node.createURI("http://example.org/value"), Node.createVariable("o")));
		NodeRelation[] rels = translate(pattern, "optimizer/filtertests.n3");
		
		NodeRelation intvalue = search("table2", "intvalue", rels);
	
		Expr sameTerm = new E_SameTerm(new ExprVar("o"),  NodeValue.makeNode("1", XSDDatatype.XSDint));
		
		Expression result = TransformExprToSQLApplyer.convert(sameTerm, intvalue);
		TypedNodeMaker nm = (TypedNodeMaker) intvalue.nodeMaker(Var.alloc("o"));
		Expression expected = nm.valueMaker().valueExpression("1");
		
		assertEquals("sameTerm(?o, \"1\"^^xsd:int)", expected, result);
		
		sameTerm = new E_SameTerm(new ExprVar("o"),  NodeValue.makeNode("1", XSDDatatype.XSDdecimal));
		
		result = TransformExprToSQLApplyer.convert(sameTerm, intvalue);
		
		assertEquals("sameTerm(?o, \"1\"^^xsd:decimal)", Expression.FALSE, result);
	}
	
	private NodeRelation[] translate(List<Triple> pattern, String mappingFile) {
		Collection<NodeRelation> rels = new GraphPatternTranslator(pattern,
				MapFixture.loadPropertyBridges(mappingFile), true).translate();
		return (NodeRelation[]) rels.toArray(new NodeRelation[rels.size()]);
	}

}
