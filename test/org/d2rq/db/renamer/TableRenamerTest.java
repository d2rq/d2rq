package org.d2rq.db.renamer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.d2rq.db.expr.SQLExpression;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType.GenericType;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TableRenamerTest {
	private TableName foo, bar;
	private ColumnName foo_col1, bar_col1, baz_col1;
	private Renamer fooAsBarMap;
	
	@Before
	public void setUp() {
		foo = TableName.parse("foo");
		bar = TableName.parse("bar");
		foo_col1 = ColumnName.parse("foo.col1");
		bar_col1 = ColumnName.parse("bar.col1");
		baz_col1 = ColumnName.parse("baz.col1");
		fooAsBarMap = TableRenamer.create(foo, bar);
	}
	
	@Test
	public void testApplyToColumn() {
		assertEquals(baz_col1, fooAsBarMap.applyTo(baz_col1));
		assertEquals(bar_col1, fooAsBarMap.applyTo(foo_col1));
		assertEquals(bar_col1, fooAsBarMap.applyTo(bar_col1));
	}
	
	@Test
	public void testApplyToSQLExpression() {
		assertEquals(
				SQLExpression.create(
						Arrays.asList(new String[]{"","=1"}), 
						Arrays.asList(new ColumnName[]{bar_col1}), 
						GenericType.BOOLEAN), 
				fooAsBarMap.applyTo(SQLExpression.create(
						Arrays.asList(new String[]{"","=1"}), 
						Arrays.asList(new ColumnName[]{foo_col1}), 
						GenericType.BOOLEAN)));
	}

	@Test
	public void testApplyEmpty() {
		assertEquals(foo, TableRenamer.create(Collections.<TableName,TableName>emptyMap()).applyTo(foo));
		assertEquals(foo_col1, TableRenamer.create(Collections.<TableName,TableName>emptyMap()).applyTo(foo_col1));
	}
}