package de.fuberlin.wiwiss.d2rq.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.d2rq.algebra.Attribute;
import org.d2rq.algebra.AttributeProjectionSpec;
import org.d2rq.db.DummyDB;
import org.d2rq.db.ResultRow;
import org.d2rq.db.expr.Disjunction;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.renamer.ColumnRenamerMap;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.values.ColumnValueMaker;
import org.d2rq.values.ValueMaker;
import org.junit.Before;
import org.junit.Test;

import de.fuberlin.wiwiss.d2rq.nodes.NodeSetConstraintBuilder;

/**
 * TODO Temporarily abandoned until we work out how to support relative IRIs
 */
public class RelativeIRIValueMakerTest {
	private final String baseIRI1 = "http://example.com/";
	private final String notBaseIRI = "http://d2rq.org/";
	private final static Attribute column1 = DummyDB.column("TABLE1", "COLUMN1");
	private final static Attribute column2 = DummyDB.column("TABLE1", "COLUMN2");
	private final static ColumnValueMaker c1 = new ColumnValueMaker(column1);
	private final static AttributeProjectionSpec column1spec = AttributeProjectionSpec.create(column1);
	
	private ValueMaker relativeBase1;
	
	@Before
	public void setUp() {
		relativeBase1 = null;//new RelativeIRIValueMaker(c1, baseIRI1);
	}
	
	@Test
	public void testToString() {
		assertEquals("RelativeIRI(http://exmaple.com/(Column(TABLE1.COLUMN1))",
				relativeBase1.toString());
	}
	
	@Test
	void sameProjectionSpecs() {
		assertEquals(c1.projectionSpecs(), 
				relativeBase1.projectionSpecs());
	}
	
	@Test
	void sameOrderSpecs() {
		assertEquals(c1.orderSpecs(true), relativeBase1.orderSpecs(true));
		assertEquals(c1.orderSpecs(false), relativeBase1.orderSpecs(false));
	}
	
	@Test
	void renameColumns() {
		Renamer renamer = new ColumnRenamerMap(Collections.singletonMap(column1, column2));
		assertEquals("RelativeIRI(http://example.com/(Column(TABLE1.COLUMN2))",
				relativeBase1.rename(renamer));
	}
	
	@Test
	void makeNodeNullFromNull() {
		assertNull(relativeBase1.makeValue(ResultRow.NO_ATTRIBUTES));
	}
	
	@Test
	void makeNodeFromIRI() {
		assertEquals(notBaseIRI, 
				relativeBase1.makeValue(ResultRow.createOne(column1spec, notBaseIRI)));
	}
	
	@Test
	void makeNodeFromNonIRIUsingBase() {
		assertEquals(baseIRI1 + "local",
				relativeBase1.makeValue(ResultRow.createOne(column1spec, "local")));
	}
	
	@Test
	void makeNullFromNonIRIThatCannotBeResolved() {
		assertNull(relativeBase1.makeValue(ResultRow.createOne(column1spec, "not valid in IRIs")));
	}

	@Test
	void expressionFalseForNonIRI() {
		assertEquals(Expression.FALSE, relativeBase1.valueExpression("noIRI"));
	}
	
	@Test
	void expressionEqualsForIRINotInBase() {
		assertEquals(Equality.createAttributeValue(column1, notBaseIRI),
				relativeBase1.valueExpression(notBaseIRI));
	}
	
	@Test
	void iriInBaseMatchesRelativeOrAbsolute() {
		assertEquals(
				Disjunction.create(
						Equality.createAttributeValue(column1, "local"), 
						Equality.createAttributeValue(column1, baseIRI1 + "local")),
				relativeBase1.valueExpression(baseIRI1 + "local"));
	}
	
	@Test
	void describeSelf() {
		NodeSetConstraintBuilder builder = new NodeSetConstraintBuilder();
		relativeBase1.describeSelf(builder);
		builder.limitValuesToColumn(column2);
	}
}
