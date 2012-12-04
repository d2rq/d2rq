package org.d2rq.find;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.DummyDB;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.find.URIMakerRule;
import org.d2rq.find.URIMakerRule.URIMakerRuleChecker;
import org.d2rq.lang.Pattern;
import org.d2rq.nodes.FixedNodeMaker;
import org.d2rq.nodes.TypedNodeMaker;
import org.d2rq.values.ColumnValueMaker;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;


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
public class URIMakerRuleTest {
	private TripleRelation withURIPatternSubject;
	private TripleRelation withURIPatternSubjectAndObject;
	private TripleRelation withURIColumnSubject;
	private TripleRelation withURIPatternSubjectAndURIColumnObject;
	private URIMakerRuleChecker employeeChecker;
	private URIMakerRuleChecker foobarChecker;

	@Before
	public void setUp() {
		DummyDB db = new DummyDB();
		NamedOp base = db.table("employees", new String[]{"ID", "manager", "homepage"});
		this.withURIPatternSubject = new TripleRelation(db, base,
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Pattern("http://test/person@@employees.ID@@").toTemplate(db)),
				new FixedNodeMaker(RDF.type.asNode()),
				new FixedNodeMaker(FOAF.Person.asNode()));
		this.withURIPatternSubjectAndObject = new TripleRelation(db, base,
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Pattern("http://test/person@@employees.ID@@").toTemplate(db)),
				new FixedNodeMaker(FOAF.knows.asNode()),
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Pattern("http://test/person@@employees.manager@@").toTemplate(db)));
		this.withURIColumnSubject = new TripleRelation(db, base,
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new ColumnValueMaker(ColumnName.parse("employees.homepage"))),
				new FixedNodeMaker(RDF.type.asNode()),
				new FixedNodeMaker(FOAF.Document.asNode()));
		this.withURIPatternSubjectAndURIColumnObject = new TripleRelation(db, base,
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new Pattern("http://test/person@@employees.ID@@").toTemplate(db)),
				new FixedNodeMaker(FOAF.homepage.asNode()),
				new TypedNodeMaker(TypedNodeMaker.URI, 
						new ColumnValueMaker(ColumnName.parse("employees.homepage"))));
		this.employeeChecker = new URIMakerRule().createRuleChecker(
				Node.createURI("http://test/person1"));
		this.foobarChecker = new URIMakerRule().createRuleChecker(
				Node.createURI("http://test/foobar"));
	}
	
	@Test
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
	
	@Test
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
	
	@Test
	public void testRuleCheckerStartsAccepting() {
		assertTrue(this.employeeChecker.canMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT)));
		assertTrue(this.employeeChecker.canMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT)));
	}
	
	@Test
	public void testRuleCheckerUnaffectedByNonURIPattern() {
		this.employeeChecker.addPotentialMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT));
		assertTrue(this.employeeChecker.canMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT)));
		assertTrue(this.employeeChecker.canMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT)));
	}
	
	@Test
	public void testRuleCheckerRejectsAfterMatch() {
		this.employeeChecker.addPotentialMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT));
		assertFalse(this.employeeChecker.canMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT)));
		assertTrue(this.employeeChecker.canMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT)));
	}
	
	@Test
	public void testRuleCheckerDoesNotRejectAfterNonMatch() {
		this.foobarChecker.addPotentialMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT));
		assertTrue(this.foobarChecker.canMatch(
				this.withURIColumnSubject.nodeMaker(TripleRelation.SUBJECT)));
		assertTrue(this.foobarChecker.canMatch(
				this.withURIPatternSubject.nodeMaker(TripleRelation.SUBJECT)));
	}
}
