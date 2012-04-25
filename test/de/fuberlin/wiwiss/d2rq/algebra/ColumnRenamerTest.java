package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ColumnRenamerTest extends TestCase {
	private final static Attribute col1 = new Attribute(null, "foo", "col1");
	private final static Attribute col2 = new Attribute(null, "foo", "col2");
	private final static Attribute col3 = new Attribute(null, "foo", "col3");

	private ColumnRenamerMap col1ToCol2;

	public void setUp() {
		Map<Attribute,Attribute> m = new HashMap<Attribute,Attribute>();
		m.put(col1, col2);
		this.col1ToCol2 = new ColumnRenamerMap(m);
	}
	
	public void testApplyToUnmappedColumnReturnsSameColumn() {
		assertEquals(col3, this.col1ToCol2.applyTo(col3));
	}
	
	public void testApplyToMappedColumnReturnsNewName() {
		assertEquals(col2, this.col1ToCol2.applyTo(col1));
	}
	
	public void testApplyToNewNameReturnsNewName() {
		assertEquals(col2, this.col1ToCol2.applyTo(col2));
	}
	
	public void testApplyToExpressionReplacesMappedColumns() {
		Expression e = SQLExpression.create("foo.col1=foo.col3");
		assertEquals(SQLExpression.create("foo.col2=foo.col3"), this.col1ToCol2.applyTo(e));
	}
	
	public void testApplyToAliasMapReturnsOriginal() {
		AliasMap aliases = new AliasMap(Collections.singleton(new Alias(
				new RelationName(null, "foo"), new RelationName(null, "bar"))));
		assertEquals(aliases, this.col1ToCol2.applyTo(aliases));
	}
	
	public void testNullRenamerToStringEmpty() {
		assertEquals("ColumnRenamer.NULL", ColumnRenamer.NULL.toString());
	}
	
	public void testEmptyRenamerToStringEmpty() {
		assertEquals("ColumnRenamerMap()", new ColumnRenamerMap(Collections.<Attribute,Attribute>emptyMap()).toString());
	}
	
	public void testToStringOneAlias() {
		assertEquals("ColumnRenamerMap(foo.col1 => foo.col2)", col1ToCol2.toString());
	}
	
	public void testToStringTwoAliases() {
		Map<Attribute,Attribute> m = new HashMap<Attribute,Attribute>();
		m.put(col1, col3);
		m.put(col2, col3);
		// Order is alphabetical by original column name
		assertEquals("ColumnRenamerMap(foo.col1 => foo.col3, foo.col2 => foo.col3)", 
				new ColumnRenamerMap(m).toString());
	}
	
	public void testRenameWithSchema() {
		Attribute foo_c1 = new Attribute("schema", "foo", "col1");
		Attribute bar_c2 = new Attribute("schema", "bar", "col2");
		ColumnRenamer renamer = new ColumnRenamerMap(
				Collections.singletonMap(foo_c1, bar_c2));
		assertEquals(bar_c2, renamer.applyTo(foo_c1));
		assertEquals(col1, renamer.applyTo(col1));
	}
}
