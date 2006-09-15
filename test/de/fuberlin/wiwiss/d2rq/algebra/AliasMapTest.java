package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Expression;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;

import junit.framework.TestCase;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: AliasMapTest.java,v 1.3 2006/09/15 12:25:25 cyganiak Exp $
 */
public class AliasMapTest extends TestCase {
	private final static RelationName foo = new RelationName(null, "foo");
	private final static RelationName bar = new RelationName(null, "bar");
	private final static RelationName baz = new RelationName(null, "baz");
	private final static Attribute foo_col1 = new Attribute(null, "foo", "col1");
	private final static Attribute bar_col1 = new Attribute(null, "bar", "col1");
	private final static Attribute baz_col1 = new Attribute(null, "baz", "col1");
	private final static Attribute abc_col1 = new Attribute(null, "abc", "col1");
	private final static Attribute xyz_col1 = new Attribute(null, "xyz", "col1");

	private Alias fooAsBar = new Alias(foo, bar);
	private Alias fooAsBaz = new Alias(foo, baz);
	private Alias bazAsBar = new Alias(baz, bar);
	private AliasMap fooAsBarMap;
	
	public void setUp() {
		Map m = new HashMap();
		m.put(bar, foo);
		this.fooAsBarMap = new AliasMap(m);
	}
	
	public void testEmptyMapDoesIdentityTranslation() {
		AliasMap aliases = new AliasMap(Collections.EMPTY_MAP);
		assertFalse(aliases.isAlias(foo));
		assertFalse(aliases.hasAlias(foo));
		assertEquals(foo, aliases.applyTo(foo));
		assertEquals(foo, aliases.originalOf(foo));
	}

	public void testAliasIsTranslated() {
		assertFalse(this.fooAsBarMap.isAlias(foo));
		assertTrue(this.fooAsBarMap.isAlias(bar));
		assertFalse(this.fooAsBarMap.isAlias(baz));
		assertTrue(this.fooAsBarMap.hasAlias(foo));
		assertFalse(this.fooAsBarMap.hasAlias(bar));
		assertFalse(this.fooAsBarMap.hasAlias(baz));
		assertEquals(bar, this.fooAsBarMap.applyTo(foo));
		assertEquals(baz, this.fooAsBarMap.applyTo(baz));
		assertEquals(foo, this.fooAsBarMap.originalOf(bar));
		assertEquals(baz, this.fooAsBarMap.originalOf(baz));
	}
	
	public void testApplyToColumn() {
		assertEquals(baz_col1, this.fooAsBarMap.applyTo(baz_col1));
		assertEquals(bar_col1, this.fooAsBarMap.applyTo(foo_col1));
		assertEquals(bar_col1, this.fooAsBarMap.applyTo(bar_col1));
	}
	
	public void testOriginalOfColumn() {
		assertEquals(baz_col1, this.fooAsBarMap.originalOf(baz_col1));
		assertEquals(foo_col1, this.fooAsBarMap.originalOf(foo_col1));
		assertEquals(foo_col1, this.fooAsBarMap.originalOf(bar_col1));
	}
	
	public void testApplyToMapKeys() {
		Map data = new HashMap();
		data.put(foo_col1, "val1");
		data.put(baz_col1, "val2");
		Map expected = new HashMap();
		expected.put(bar_col1, "val1");
		expected.put(baz_col1, "val2");
		assertEquals(expected, this.fooAsBarMap.applyToMapKeys(data));
	}
	
	public void testApplyToColumnSet() {
		Set data = new HashSet(Arrays.asList(new Attribute[]{foo_col1, baz_col1}));
		Set expected = new HashSet(Arrays.asList(new Attribute[]{bar_col1, baz_col1}));
		assertEquals(expected, this.fooAsBarMap.applyToColumnSet(data));
	}
	
	public void testApplyToJoinSetDoesNotModifyUnaliasedJoin() {
		Join join = new Join();
		join.addCondition(abc_col1, xyz_col1);
		Set joins = Collections.singleton(join);
		assertEquals(joins, this.fooAsBarMap.applyToJoinSet(joins));
	}
	
	public void testApplyToJoinSetDoesModifyAliasedJoin() {
		Join join = new Join();
		join.addCondition(foo_col1, foo_col1);
		Set aliasedSet = this.fooAsBarMap.applyToJoinSet(Collections.singleton(join));
		assertEquals(1, aliasedSet.size());
		Join aliased = (Join) aliasedSet.iterator().next();
		assertEquals(Collections.singleton(bar_col1), aliased.getFirstColumns());
		assertEquals(Collections.singleton(bar_col1), aliased.getSecondColumns());
	}
	
	public void testApplyToExpression() {
		assertEquals(new Expression("bar.col1"), 
				fooAsBarMap.applyTo(new Expression("foo.col1")));
	}

	public void testNoAliasesConstantEqualsNewEmptyAliasMap() {
		AliasMap noAliases = new AliasMap(Collections.EMPTY_MAP);
		assertTrue(AliasMap.NO_ALIASES.equals(noAliases));
		assertTrue(noAliases.equals(AliasMap.NO_ALIASES));
	}
	
	public void testEmptyMapEqualsItself() {
		assertTrue(AliasMap.NO_ALIASES.equals(AliasMap.NO_ALIASES));
	}
	
	public void testEmptyMapDoesntEqualPopulatedMap() {
		assertFalse(AliasMap.NO_ALIASES.equals(fooAsBarMap));
	}
	
	public void testPopulatedMapDoesntEqualEmptyMap() {
		assertFalse(fooAsBarMap.equals(AliasMap.NO_ALIASES));
	}
	
	public void testPopulatedMapEqualsItself() {
		Map m = new HashMap();
		m.put(bar, foo);
		AliasMap fooAsBar2 = new AliasMap(m);
		assertTrue(fooAsBarMap.equals(fooAsBar2));
		assertTrue(fooAsBar2.equals(fooAsBarMap));
	}
	
	public void testPopulatedMapDoesNotEqualDifferentMap() {
		Map m = new HashMap();
		m.put(baz, foo);
		AliasMap fooAsBaz = new AliasMap(m);
		assertFalse(fooAsBarMap.equals(fooAsBaz));
		assertFalse(fooAsBaz.equals(fooAsBarMap));
	}
	
	public void testEqualMapsHaveSameHashCode() {
		AliasMap m1 = new AliasMap(new HashMap());
		AliasMap m2 = new AliasMap(new HashMap());
		assertEquals(m1.hashCode(), m2.hashCode());
	}
	
	public void testBuildFromSQL() {
		assertEquals(AliasMap.NO_ALIASES, AliasMap.buildFromSQL(Collections.EMPTY_SET));
		assertEquals(fooAsBarMap, AliasMap.buildFromSQL(Collections.singleton("foo AS bar")));
		assertEquals(fooAsBarMap, AliasMap.buildFromSQL(Collections.singleton("foo as bar")));
	}

	public void testParseSQL() {
		assertEquals(
				new Alias(new RelationName(null, "table1"), new RelationName("schema", "table2")),
				AliasMap.buildAlias("table1 AS schema.table2"));
	}

	public void testAliasEquals() {
		Alias fooAsBar2 = new Alias(foo, bar);
		assertEquals(fooAsBar, fooAsBar2);
		assertEquals(fooAsBar2, fooAsBar);
		assertEquals(fooAsBar.hashCode(), fooAsBar2.hashCode());
	}
	
	public void testAliasNotEquals() {
		assertFalse(fooAsBar.equals(fooAsBaz));
		assertFalse(fooAsBaz.equals(fooAsBar));
		assertFalse(fooAsBar.equals(bazAsBar));
		assertFalse(bazAsBar.equals(fooAsBar));
		assertFalse(fooAsBar.hashCode() == fooAsBaz.hashCode());
		assertFalse(fooAsBar.hashCode() == bazAsBar.hashCode());
	}
	
	public void testAliasToString() {
		assertEquals("foo AS bar", fooAsBar.toString());
	}
	
	public void testOriginalOfAliasEmpty() {
		assertEquals(fooAsBar, AliasMap.NO_ALIASES.originalOf(fooAsBar));
	}
	
	public void testOriginalOfAlias() {
		assertEquals(fooAsBaz, fooAsBarMap.originalOf(new Alias(bar, baz)));
	}
	
	public void testToStringEmpty() {
		assertEquals("AliasMap()", AliasMap.NO_ALIASES.toString());
	}
	
	public void testToStringOneAlias() {
		assertEquals("AliasMap(foo AS bar)", fooAsBarMap.toString());
	}
	
	public void testToStringTwoAliases() {
		Map m = new HashMap();
		m.put(bar, foo);
		m.put(new RelationName(null, "xyz"), new RelationName(null, "abc"));
		// Order is alphabetical by alias
		assertEquals("AliasMap(foo AS bar, abc AS xyz)", new AliasMap(m).toString());
	}
	
	public void testWithSchema() {
		RelationName table = new RelationName(null, "table");
		RelationName schema_table = new RelationName("schema", "table");
		RelationName schema_alias = new RelationName("schema", "alias");
		AliasMap m = new AliasMap(Collections.singletonMap(schema_alias, schema_table));
		assertEquals(schema_alias, m.applyTo(schema_table));
		assertEquals(table, m.applyTo(table));
	}
}