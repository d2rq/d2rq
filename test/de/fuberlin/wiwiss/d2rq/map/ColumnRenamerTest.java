package de.fuberlin.wiwiss.d2rq.map;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Expression;

import junit.framework.TestCase;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ColumnRenamerTest.java,v 1.4 2006/09/11 23:02:50 cyganiak Exp $
 */
public class ColumnRenamerTest extends TestCase {
	private final static Attribute col1 = new Attribute("foo.col1");
	private final static Attribute col2 = new Attribute("foo.col2");
	private final static Attribute col3 = new Attribute("foo.col3");

	private ColumnRenamerMap col1ToCol2;

	public void setUp() {
		Map m = new HashMap();
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
	
	public void testApplyToColumnSetReplacesMappedColumns() {
		Set originals = new HashSet(Arrays.asList(new Attribute[]{col1, col2, col3}));
		Set expected = new HashSet(Arrays.asList(new Attribute[]{col2, col3}));
		assertEquals(expected, col1ToCol2.applyToColumnSet(originals));
	}
	
	public void testApplyToMapKeysReplacesMappedKeys() {
		Map original = new HashMap();
		original.put(col1, "value1");
		original.put(col3, "value3");
		Map expected = new HashMap(); 
		expected.put(col2, "value1");
		expected.put(col3, "value3");
		assertEquals(expected, this.col1ToCol2.applyToMapKeys(original));
	}
	
	public void testApplyToMapKeysWithContradictionReturnsNull() {
		Map original = new HashMap();
		original.put(col1, "value1");
		original.put(col2, "value2");
		assertNull(this.col1ToCol2.applyToMapKeys(original));
	}
	
	public void testApplyToMapKeysWithSameValueIsOK() {
		Map original = new HashMap();
		original.put(col1, "value1");
		original.put(col2, "value1");
		Map expected = new HashMap(); 
		expected.put(col2, "value1");
		assertEquals(expected, this.col1ToCol2.applyToMapKeys(original));
	}
	
	public void testApplyToExpressionReplacesMappedColumns() {
		Expression e = new Expression("foo.col1=foo.col3");
		assertEquals(new Expression("foo.col2=foo.col3"), this.col1ToCol2.applyTo(e));
	}
	
	public void testApplyToAliasMapReturnsOriginal() {
		Map map = new HashMap();
		map.put("bar", "foo");
		AliasMap aliases = new AliasMap(map);
		assertEquals(aliases, this.col1ToCol2.applyTo(aliases));
	}
	
	public void testNullRenamerToStringEmpty() {
		assertEquals("ColumnRenamer.NULL", ColumnRenamer.NULL.toString());
	}
	
	public void testEmptyRenamerToStringEmpty() {
		assertEquals("ColumnRenamerMap()", new ColumnRenamerMap(Collections.EMPTY_MAP).toString());
	}
	
	public void testToStringOneAlias() {
		assertEquals("ColumnRenamerMap(foo.col1 => foo.col2)", col1ToCol2.toString());
	}
	
	public void testToStringTwoAliases() {
		Map m = new HashMap();
		m.put(col1, col3);
		m.put(col2, col3);
		// Order is alphabetical by original column name
		assertEquals("ColumnRenamerMap(foo.col1 => foo.col3, foo.col2 => foo.col3)", 
				new ColumnRenamerMap(m).toString());
	}
}
