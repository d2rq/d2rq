package org.d2rq.db.renamer;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.d2rq.db.DummyDB;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.renamer.ColumnRenamer;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ColumnRenamerTest {
	private ColumnName col1, col2, col3;
	private ColumnRenamer col1ToCol2;
	
	@Before
	public void setUp() {
		col1 = ColumnName.parse("foo.col1");
		col2 = ColumnName.parse("foo.col2");
		col3 = ColumnName.parse("foo.col3");
		col1ToCol2 = new ColumnRenamer(Collections.singletonMap(col1, col2));
	}

	@Test
	public void testApplyToUnmappedColumnReturnsSameColumn() {
		assertEquals(col3, col1ToCol2.applyTo(col3));
	}
	
	@Test
	public void testApplyToMappedColumnReturnsNewName() {
		assertEquals(col2, col1ToCol2.applyTo(col1));
	}
	
	@Test
	public void testApplyToNewNameReturnsNewName() {
		assertEquals(col2, col1ToCol2.applyTo(col2));
	}
	
	@Test
	public void testApplyToExpressionReplacesMappedColumns() {
		assertEquals(
				Equality.createColumnEquality(col2, col3), 
				col1ToCol2.applyTo(Equality.createColumnEquality(col1,  col3)));
	}
	
	@Test
	public void testApplyToAliasReturnsOriginal() {
		AliasOp alias = AliasOp.create(DummyDB.createTable("foo"), "bar");
		assertEquals(alias, col1ToCol2.applyTo(alias));
	}
	
	@Test
	public void testNullRenamerToStringEmpty() {
		assertEquals("Renamer.NULL", Renamer.IDENTITY.toString());
	}
	
	@Test
	public void testEmptyRenamerToStringEmpty() {
		assertEquals("ColumnRenamerMap()", new ColumnRenamer(Collections.<ColumnName,ColumnName>emptyMap()).toString());
	}
	
	@Test
	public void testToStringOneAlias() {
		assertEquals("ColumnRenamerMap(foo.col1 => foo.col2)", col1ToCol2.toString());
	}
	
	@Test
	public void testToStringTwoAliases() {
		Map<ColumnName,ColumnName> m = new HashMap<ColumnName,ColumnName>();
		m.put(col1, col3);
		m.put(col2, col3);
		// Order is alphabetical by original column name
		assertEquals("ColumnRenamerMap(foo.col1 => foo.col3, foo.col2 => foo.col3)", 
				new ColumnRenamer(m).toString());
	}
	
	@Test
	public void testRenameWithSchema() {
		ColumnName foo_c1 = ColumnName.parse("schema.foo.col1");
		ColumnName bar_c2 = ColumnName.parse("schema.bar.col2");
		Renamer renamer = new ColumnRenamer(
				Collections.singletonMap(foo_c1, bar_c2));
		assertEquals(bar_c2, renamer.applyTo(foo_c1));
		assertEquals(col1, renamer.applyTo(col1));
	}
}
