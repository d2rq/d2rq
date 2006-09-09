package de.fuberlin.wiwiss.d2rq.map;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ColumnRenamerTest.java,v 1.1 2006/09/09 15:40:05 cyganiak Exp $
 */
public class ColumnRenamerTest extends TestCase {
	private final static Column col1 = new Column("foo.col1");
	private final static Column col2 = new Column("foo.col2");
	private final static Column col3 = new Column("foo.col3");

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
		Set originals = new HashSet(Arrays.asList(new Column[]{col1, col2, col3}));
		Set expected = new HashSet(Arrays.asList(new Column[]{col2, col3}));
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
	
	public void testWithOriginalKeysDoesNotAffectUnmappedColumns() {
		Map replaced = new HashMap();
		replaced.put(col3.getQualifiedName(), "value3");
		assertEquals(replaced, this.col1ToCol2.withOriginalKeys(replaced));
	}
	
	public void testWithOriginalKeysAffectsMappedColumns() {
		Map replaced = new HashMap();
		replaced.put(col2.getQualifiedName(), "value2");
		Map original = new HashMap();
		original.put(col2.getQualifiedName(), "value2");
		original.put(col1.getQualifiedName(), "value2");
		assertEquals(original, this.col1ToCol2.withOriginalKeys(replaced));
	}
	
	public void testWithOriginalKeysHandlesMultipleOriginals() {
		Map replacements = new HashMap();
		replacements.put(col1, col3);
		replacements.put(col2, col3);
		Map replaced = new HashMap();
		replaced.put(col3.getQualifiedName(), "value3");
		Map original = new HashMap();
		original.put(col1.getQualifiedName(), "value3");
		original.put(col2.getQualifiedName(), "value3");
		original.put(col3.getQualifiedName(), "value3");
		assertEquals(original, new ColumnRenamerMap(replacements).withOriginalKeys(replaced));
	}
	
	public void testWithOriginalKeysConflictThrowsException() {
		Map replaced = new HashMap();
		replaced.put(col1, "value1");
		replaced.put(col2, "value2");
		try {
			this.col1ToCol2.withOriginalKeys(replaced);
			fail("Must throw exception because col1 should never occur on the replaced side");
		} catch (Exception ex) {
			// is expected
		}
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
