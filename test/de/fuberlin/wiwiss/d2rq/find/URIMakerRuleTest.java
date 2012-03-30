package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.find.URIMakerRule.URIMakerRuleChecker;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.values.Column;
import de.fuberlin.wiwiss.d2rq.values.Pattern;

/**
 * :cm1 a d2rq:ClassMap;
 *     d2rq:uriPattern "http://test/person@@employees.ID@@";
 *     d2rq:class foaf:Person;
 *     d2rq:propertyBridge [
 *         d2rq:property foaf:knows;
 *         d2rq:uriPattern "http://test/person@@employees.manager@@";
 *     ];
 *     d2rq:propertyBridge [
 *         d2rq:property foaf:homepage;
 *         d2rq:uriColumn "employees.homepage";
 *     ];
 *     .
 * :cm2 a d2rq:ClassMap;
 *     d2rq:uriColumn "employees.homepage";
 *     d2rq:class foaf:Document;
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class URIMakerRuleTest extends TestCase {
	private TripleRelation withURIPatternSubject;
	private TripleRelation withURIPatternSubjectAndObject;
	private TripleRelation withURIColumnSubject;
	private TripleRelation withURIPatternSubjectAndURIColumnObject;
	private URIMakerRuleChecker employeeChecker;
	private URIMakerRuleChecker foobarChecker;

	public void setUp() {
		Relation base = new RelationImpl(null, AliasMap.NO_ALIASES, 
				Expression.TRUE, Expression.TRUE, 
				Collections.<Join>emptySet(), Collections.<ProjectionSpec>emptySet(), 
				false, OrderSpec.NONE, Relation.NO_LIMIT, Relation.NO_LIMIT);
		this.withURIPatternSubject = new TripleRelation(base,
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Pattern("http://test/person@@employees.ID@@"), true),
				new FixedNodeMaker(RDF.type.asNode(), false),
				new FixedNodeMaker(FOAF.Person.asNode(), false));
		this.withURIPatternSubjectAndObject = new TripleRelation(base,
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Pattern("http://test/person@@employees.ID@@"), true),
				new FixedNodeMaker(FOAF.knows.asNode(), false),
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Pattern("http://test/person@@employees.manager@@"), true));
		this.withURIColumnSubject = new TripleRelation(base,
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Column(new Attribute(null, "employees", "homepage")), false),
				new FixedNodeMaker(RDF.type.asNode(), false),
				new FixedNodeMaker(FOAF.Document.asNode(), false));
		this.withURIPatternSubjectAndURIColumnObject = new TripleRelation(base,
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Pattern("http://test/person@@employees.ID@@"), true),
				new FixedNodeMaker(FOAF.homepage.asNode(), false),
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Column(new Attribute(null, "employees", "homepage")), false));
		this.employeeChecker = new URIMakerRule().createRuleChecker(
				Node.createURI("http://test/person1"));
		this.foobarChecker = new URIMakerRule().createRuleChecker(
				Node.createURI("http://test/foobar"));
	}
	
	public void testComparator() {
		URIMakerRule u = new URIMakerRule();
		assertEquals(0, u.compare(this.withURIPatternSubject, this.withURIPatternSubject));
		assertEquals(1, u.compare(this.withURIPatternSubject, this.withURIPatternSubjectAndObject));
		assertEquals(-1, u.compare(this.withURIPatternSubject, this.withURIColumnSubject));
		assertEquals(-1, u.compare(this.withURIPatternSubject, this.withURIPatternSubjectAndURIColumnObject));

		assertEquals(-1, u.compare(this.withURIPatternSubjectAndObject, this.withURIPatternSubject));
		assertEquals(0, u.compare(this.withURIPatternSubjectAndObject, this.withURIPatternSubjectAndObject));
		assertEquals(-1, u.compare(this.withURIPatternSubjectAndObject, this.withURIColumnSubject));
		assertEquals(-1, u.compare(this.withURIPatternSubjectAndObject, this.withURIPatternSubjectAndURIColumnObject));

		assertEquals(1, u.compare(this.withURIColumnSubject, this.withURIPatternSubject));
		assertEquals(1, u.compare(this.withURIColumnSubject, this.withURIPatternSubjectAndObject));
		assertEquals(0, u.compare(this.withURIColumnSubject, this.withURIColumnSubject));
		assertEquals(1, u.compare(this.withURIColumnSubject, this.withURIPatternSubjectAndURIColumnObject));

		assertEquals(1, u.compare(this.withURIPatternSubjectAndURIColumnObject, this.withURIPatternSubject));
		assertEquals(1, u.compare(this.withURIPatternSubjectAndURIColumnObject, this.withURIPatternSubjectAndObject));
		assertEquals(-1, u.compare(this.withURIPatternSubjectAndURIColumnObject, this.withURIColumnSubject));
		assertEquals(0, u.compare(this.withURIPatternSubjectAndURIColumnObject, this.withURIPatternSubjectAndURIColumnObject));
	}
	
	public void testSort() {
		Collection<TripleRelation> unsorted = new ArrayList<TripleRelation>(Arrays.asList(new TripleRelation[]{
				this.withURIColumnSubject,
				this.withURIPatternSubject, 
				this.withURIPatternSubjectAndObject,
				this.withURIPatternSubjectAndURIColumnObject
		}));
		Collection<TripleRelation> sorted = new ArrayList<TripleRelation>(Arrays.asList(new TripleRelation[]{
				this.withURIPatternSubjectAndObject,
				this.withURIPatternSubject, 
				this.withURIPatternSubjectAndURIColumnObject,
				this.withURIColumnSubject
		}));
		assertEquals(sorted, new URIMakerRule().sortRDFRelations(unsorted));
	}
	
	public void testRuleCheckerStartsAccepting() {
		assertTrue(this.employeeChecker.canMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT)));
		assertTrue(this.employeeChecker.canMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT)));
	}
	
	public void testRuleCheckerUnaffectedByNonURIPattern() {
		this.employeeChecker.addPotentialMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT));
		assertTrue(this.employeeChecker.canMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT)));
		assertTrue(this.employeeChecker.canMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT)));
	}
	
	public void testRuleCheckerRejectsAfterMatch() {
		this.employeeChecker.addPotentialMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT));
		assertFalse(this.employeeChecker.canMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT)));
		assertTrue(this.employeeChecker.canMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT)));
	}
	
	public void testRuleCheckerDoesNotRejectAfterNonMatch() {
		this.foobarChecker.addPotentialMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT));
		assertTrue(this.foobarChecker.canMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT)));
		assertTrue(this.foobarChecker.canMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT)));
	}
}
