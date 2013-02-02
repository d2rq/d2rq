package org.d2rq.db;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.d2rq.db.expr.ColumnExpr;
import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.expr.Constant;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.DistinctOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.ExtendOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.OrderOp;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.lang.Microsyntax;
import org.junit.Before;
import org.junit.Test;



public class SelectStatementBuilderTest {
	private DummyDB db;
	private NamedOp table1, table2, table3;
	private ColumnName table1Foo, table2Bar, table3Foo;
	private DatabaseOp table100, simpleExpression, selectStatement;
	
	@Before
	public void setUp() {
		db = new DummyDB();
		table1 = db.table("table1", new String[]{"foo"});
		table2 = db.table("table2", new String[]{"bar"});
		table3 = db.table("table3", new String[]{"foo"});
		table1Foo = ColumnName.parse("table1.foo");
		table2Bar = ColumnName.parse("table2.bar");
		table3Foo = ColumnName.parse("table3.foo");
		table100 = LimitOp.limit(table1, 100, LimitOp.NO_LIMIT);
		simpleExpression = ExtendOp.extend(DatabaseOp.TRUE, 
				Identifier.createUndelimited("EXPR"),
				Constant.create("5", GenericType.NUMERIC), 
				db.vendor());
		selectStatement = new SQLOp(db, "SELECT table.foo FROM table",
				Collections.singletonList(
						new ColumnDef(Identifier.createUndelimited("foo"), 
								GenericType.NUMERIC.dataTypeFor(db.vendor()), false)));
	}
	
	@Test
	public void testNoLimit() {
		assertEquals("SELECT table1.foo FROM table1",
				new SelectStatementBuilder(table1, db.vendor()).getSQL());
	}
	
	@Test
	public void testLimitStandard() {
		assertEquals("SELECT table1.foo FROM table1 LIMIT 100",
				new SelectStatementBuilder(table100, db.vendor()).getSQL());
	}
	
	@Test
	public void testNoLimitMSSQL() {
		db.setVendor(Vendor.SQLServer);
		assertEquals("SELECT TOP 100 table1.foo FROM table1",
				new SelectStatementBuilder(table100, db.vendor()).getSQL());
	}
	
	@Test
	public void testNoLimitOracle() {
		db.setVendor(Vendor.Oracle);
		assertEquals("SELECT table1.foo FROM table1 WHERE (ROWNUM <= 100)",
				new SelectStatementBuilder(table100, db.vendor()).getSQL());
	}
	
	@Test
	public void testTwoLimitsMerged() {
		assertEquals("SELECT table1.foo FROM table1 LIMIT 100",
				new SelectStatementBuilder(LimitOp.limit(
						table100, 200, LimitOp.NO_LIMIT), db.vendor()).getSQL());
	}
	
	@Test
	public void testExpressionOnly() {
		assertEquals("SELECT 5 AS EXPR FROM (VALUES(NULL))",
				new SelectStatementBuilder(simpleExpression, db.vendor()).getSQL().replaceAll("EXPR_[0-9A-F]*", "EXPR"));
	}
	
	@Test
	public void testExpressionOnlyMySQL() {
		db.setVendor(Vendor.MySQL);
		assertEquals("SELECT 5 AS EXPR",
				new SelectStatementBuilder(simpleExpression, db.vendor()).getSQL().replaceAll("EXPR_[0-9A-F]*", "EXPR"));
	}
	
	@Test
	public void testExpressionOnlyOracle() {
		db.setVendor(Vendor.Oracle);
		assertEquals("SELECT 5 EXPR FROM DUAL",
				new SelectStatementBuilder(simpleExpression, db.vendor()).getSQL().replaceAll("EXPR_[0-9A-F]*", "EXPR"));
	}
	
	@Test
	public void testDistinct() {
		assertEquals("SELECT DISTINCT table1.foo FROM table1",
				new SelectStatementBuilder(new DistinctOp(table1), db.vendor()).getSQL());
	}
	
	@Test
	public void testCondition() {
		assertEquals("SELECT table1.foo FROM table1 WHERE table1.foo=1",
				new SelectStatementBuilder(SelectOp.select(table1, 
						Equality.createColumnValue(table1Foo, "1", 
								GenericType.NUMERIC.dataTypeFor(db.vendor()))), db.vendor()).getSQL());
	}
	
	@Test
	public void testOrderBy() {
		assertEquals("SELECT table1.foo FROM table1 ORDER BY table1.foo",
				new SelectStatementBuilder(new OrderOp(
						Collections.singletonList(new OrderSpec(new ColumnExpr(table1Foo))), 
						table1), db.vendor()).getSQL());
	}
	
	@Test
	public void testRawSelect() {
		assertEquals("SELECT table.foo FROM table",
				new SelectStatementBuilder(selectStatement, db.vendor()).getSQL());
	}
	
	@Test
	public void testEmpty() {
		assertEquals("SELECT table1.foo FROM table1 WHERE 0",
				new SelectStatementBuilder(EmptyOp.create(table1), db.vendor()).getSQL());
	}
	
	@Test
	public void testAliasTable() {
		assertEquals("SELECT t2.foo FROM table1 AS t2",
				new SelectStatementBuilder(AliasOp.create(table1, "t2"), db.vendor()).getSQL());
	}
	
	@Test
	public void testAliasRawSelect() {
		assertEquals("SELECT t2.foo FROM (SELECT table.foo FROM table) AS t2",
				new SelectStatementBuilder(AliasOp.create(selectStatement, "t2"), db.vendor()).getSQL());
	}
	
	@Test
	public void testInnerJoinTwoTables() {
		DatabaseOp join = InnerJoinOp.join(Arrays.asList(new NamedOp[]{table1, table2}), 
				Collections.singleton(ColumnListEquality.create(table1Foo, table2Bar)));
		assertEquals("SELECT table1.foo, table2.bar FROM table1, table2 WHERE table1.foo=table2.bar",
				new SelectStatementBuilder(join, db.vendor()).getSQL());
	}
	
	@Test
	public void testInnerJoinNested() {
		NamedOp rightSide = AliasOp.create(
				InnerJoinOp.join(
						Arrays.asList(new NamedOp[]{table2, table3}), 
						Collections.singleton(ColumnListEquality.create(table2Bar, table3Foo))),
				"table4");
		DatabaseOp join = InnerJoinOp.join(
				Arrays.asList(new NamedOp[]{table1, rightSide}),
				Collections.singleton(ColumnListEquality.create(table1Foo, ColumnName.parse("table4.bar"))));
		assertEquals("SELECT table1.foo, table4.bar, table4.foo FROM table1, (SELECT table2.bar, table3.foo FROM table2, table3 WHERE table2.bar=table3.foo) AS table4 WHERE table1.foo=table4.bar",
				new SelectStatementBuilder(join, db.vendor()).getSQL());
	}
	
	@Test
	public void testReturnedProjectionSpecIsColumn() {
		Expression expr = Microsyntax.parseSQLExpression("t1.foo>0", GenericType.BOOLEAN);
		Identifier col = ExtendOp.createUniqueIdentifierFor(expr);
		DatabaseOp projection = ExtendOp.extend(table1, col, expr, db.vendor());
		ColumnList columns = new SelectStatementBuilder(
				projection, db.vendor()).getColumns();
		assertEquals(ColumnList.create(table1Foo, ColumnName.create(col)), columns);
	}
	
	@Test
	public void testNestedProjections() {
		Expression expr = Constant.create("bar", GenericType.CHARACTER);
		DatabaseOp projection = ExtendOp.extend(table1, 
				Identifier.createUndelimited("EXPR"), expr, db.vendor());
		projection = ProjectOp.project(projection, projection.getColumns());
		assertEquals("SELECT table1.foo, 'bar' AS EXPR FROM table1",
				new SelectStatementBuilder(projection, db.vendor()).getSQL());
	}
}
