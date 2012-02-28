package de.fuberlin.wiwiss.d2rq.expr;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.DummyDB;
import de.fuberlin.wiwiss.d2rq.sql.SQL;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ExpressionTest extends TestCase {
	private AliasMap aliases;
	
	public void setUp() {
		aliases = AliasMap.create1(
				new RelationName(null, "table"), new RelationName(null, "alias"));		
	}
	
	public void testTrue() {
		assertEquals("TRUE", Expression.TRUE.toString());
		assertEquals(Expression.TRUE, Expression.TRUE);
		assertTrue(Expression.TRUE.isTrue());
		assertFalse(Expression.TRUE.isFalse());
	}
	
	public void testFalse() {
		assertEquals("FALSE", Expression.FALSE.toString());
		assertEquals(Expression.FALSE, Expression.FALSE);
		assertFalse(Expression.FALSE.isTrue());
		assertTrue(Expression.FALSE.isFalse());
	}
	
	public void testTrueNotEqualFalse() {
		assertFalse(Expression.TRUE.equals(Expression.FALSE));
	}
	
	public void testConstant() {
		Expression expr = new Constant("foo");
		assertTrue(expr.attributes().isEmpty());
		assertFalse(expr.isFalse());
		assertFalse(expr.isTrue());
		assertEquals(expr, expr.renameAttributes(aliases));
	}
	
	public void testConstantEquals() {
		assertTrue(new Constant("foo").equals(new Constant("foo")));
		assertFalse(new Constant("foo").equals(new Constant("bar")));
		assertFalse(new Constant("foo").equals(Expression.TRUE));
	}
	
	public void testConstantHashCode() {
		assertEquals(new Constant("foo").hashCode(), new Constant("foo").hashCode());
		assertFalse(new Constant("foo").hashCode() == new Constant("bar").hashCode());
	}
	
	public void testConstantToString() {
		assertEquals("Constant(foo)", new Constant("foo").toString());
	}
	
	public void testConstantToSQL() {
		assertEquals("'foo'", new Constant("foo").toSQL(new DummyDB(), AliasMap.NO_ALIASES));
	}
	
	public void testConstantToSQLWithType() {
		Attribute attribute = SQL.parseAttribute("table.col1");
		DummyDB db = new DummyDB();
		db.setColumnType(attribute, ConnectedDB.NUMERIC_COLUMN);
		assertEquals("42", new Constant("42", attribute).toSQL(db, AliasMap.NO_ALIASES));
	}
	
	public void testConstantToSQLWithTypeAndAlias() {
		Attribute attribute = SQL.parseAttribute("table.col1");
		Attribute aliasedAttribute = SQL.parseAttribute("alias.col1");
		DummyDB db = new DummyDB();
		db.setColumnType(attribute, ConnectedDB.NUMERIC_COLUMN);
		assertEquals("42", new Constant("42", aliasedAttribute).toSQL(db, aliases));
	}
	
	public void testConstantTypeAttributeIsRenamed() {
		Attribute attribute = SQL.parseAttribute("table.col1");
		assertEquals("Constant(42@alias.col1)", 
				new Constant("42", attribute).renameAttributes(aliases).toString());
	}
}
