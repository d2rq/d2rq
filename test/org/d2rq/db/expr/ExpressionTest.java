package org.d2rq.db.expr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.d2rq.db.DummyDB;
import org.d2rq.db.DummyDB.DummyTable;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType.GenericType;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ExpressionTest {
	private DummyDB db;
	private DummyTable table;
	private AliasOp alias;
	
	@Before
	public void setUp() {
		db = new DummyDB();
		table = db.table("TABLE", "COL1");
		alias = AliasOp.create(table, TableName.parse("ALIAS"));		
	}
	
	@Test
	public void testTrue() {
		assertEquals("TRUE", Expression.TRUE.toString());
		assertEquals(Expression.TRUE, Expression.TRUE);
		assertTrue(Expression.TRUE.isTrue());
		assertFalse(Expression.TRUE.isFalse());
	}
	
	@Test
	public void testFalse() {
		assertEquals("FALSE", Expression.FALSE.toString());
		assertEquals(Expression.FALSE, Expression.FALSE);
		assertFalse(Expression.FALSE.isTrue());
		assertTrue(Expression.FALSE.isFalse());
	}
	
	@Test
	public void testTrueNotEqualFalse() {
		assertFalse(Expression.TRUE.equals(Expression.FALSE));
	}
	
	@Test
	public void testConstant() {
		Expression expr = Constant.create("foo", GenericType.CHARACTER);
		assertTrue(expr.getColumns().isEmpty());
		assertFalse(expr.isFalse());
		assertFalse(expr.isTrue());
		assertEquals(expr, expr.rename(alias.getRenamer()));
	}
	
	@Test
	public void testConstantEquals() {
		assertTrue(Constant.create("foo", GenericType.CHARACTER).equals(Constant.create("foo", GenericType.CHARACTER)));
		assertFalse(Constant.create("foo", GenericType.CHARACTER).equals(Constant.create("bar", GenericType.CHARACTER)));
		assertFalse(Constant.create("foo", GenericType.CHARACTER).equals(Expression.TRUE));
	}
	
	@Test
	public void testConstantHashCode() {
		assertEquals(Constant.create("foo", GenericType.CHARACTER).hashCode(), Constant.create("foo", GenericType.CHARACTER).hashCode());
		assertFalse(Constant.create("foo", GenericType.CHARACTER).hashCode() == Constant.create("bar", GenericType.CHARACTER).hashCode());
	}
	
	@Test
	public void testConstantToString() {
		assertEquals("Constant(foo@CHARACTER)", Constant.create("foo", GenericType.CHARACTER).toString());
	}
	
	@Test
	public void testConstantToSQL() {
		assertEquals("'foo'", Constant.create("foo", GenericType.CHARACTER).toSQL(DatabaseOp.TRUE, db.vendor()));
	}
	
	@Test
	public void testConstantToSQLWithType() {
		table.setDataType("COL1", GenericType.NUMERIC);
		ColumnName column = ColumnName.parse("TABLE.COL1");
		assertEquals("42", Constant.create("42", table.getColumnType(column)).toSQL(DatabaseOp.TRUE, db.vendor()));
	}
	
	@Test
	public void testConstantToSQLWithTypeAndAlias() {
		table.setDataType("COL1", GenericType.NUMERIC);
		assertEquals("42", Constant.create("42", alias.getColumnType(ColumnName.parse("COL1"))).toSQL(DatabaseOp.TRUE, db.vendor()));
		assertEquals("42", Constant.create("42", alias.getColumnType(ColumnName.parse("ALIAS.COL1"))).toSQL(DatabaseOp.TRUE, db.vendor()));
	}
}
