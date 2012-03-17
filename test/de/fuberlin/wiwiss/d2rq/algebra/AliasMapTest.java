package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
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
	private AliasMap fooAsBarMap = new AliasMap(Collections.singleton(new Alias(foo, bar)));
	
	public void testEmptyMapDoesIdentityTranslation() {
		AliasMap aliases = AliasMap.NO_ALIASES;
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
	
	public void testApplyToJoinSetDoesNotModifyUnaliasedJoin() {
		Join join = new Join(abc_col1, xyz_col1, Join.DIRECTION_RIGHT);
		Set<Join> joins = Collections.singleton(join);
		assertEquals(joins, this.fooAsBarMap.applyToJoinSet(joins));
	}
	
	public void testApplyToJoinSetDoesModifyAliasedJoin() {
		Join join = new Join(foo_col1, foo_col1, Join.DIRECTION_RIGHT);
		Set<Join> aliasedSet = this.fooAsBarMap.applyToJoinSet(Collections.singleton(join));
		assertEquals(1, aliasedSet.size());
		Join aliased = (Join) aliasedSet.iterator().next();
		assertEquals(Collections.singletonList(bar_col1), aliased.attributes1());
		assertEquals(Collections.singletonList(bar_col1), aliased.attributes2());
	}
	
	public void testApplyToSQLExpression() {
		assertEquals(SQLExpression.create("bar.col1 = 1"), 
				fooAsBarMap.applyTo(SQLExpression.create("foo.col1 = 1")));
	}

	public void testNoAliasesConstantEqualsNewEmptyAliasMap() {
		AliasMap noAliases = new AliasMap(Collections.<Alias>emptyList());
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
		AliasMap fooAsBar2 = new AliasMap(Collections.singleton(new Alias(foo, bar)));
		assertTrue(fooAsBarMap.equals(fooAsBar2));
		assertTrue(fooAsBar2.equals(fooAsBarMap));
	}
	
	public void testPopulatedMapDoesNotEqualDifferentMap() {
		AliasMap fooAsBaz = new AliasMap(Collections.singleton(new Alias(foo, baz)));
		assertFalse(fooAsBarMap.equals(fooAsBaz));
		assertFalse(fooAsBaz.equals(fooAsBarMap));
	}
	
	public void testEqualMapsHaveSameHashCode() {
		AliasMap m1 = new AliasMap(new ArrayList<Alias>());
		AliasMap m2 = new AliasMap(new ArrayList<Alias>());
		assertEquals(m1.hashCode(), m2.hashCode());
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
	
	public void testApplyToAliasEmpty() {
		assertEquals(fooAsBar, AliasMap.NO_ALIASES.applyTo(fooAsBar));
	}
	
	public void testApplyToAlias() {
		assertEquals(new Alias(baz, bar), fooAsBarMap.applyTo(new Alias(baz, foo)));
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
		Collection<Alias> aliases = new ArrayList<Alias>();
		aliases.add(fooAsBar);
		aliases.add(new Alias(new RelationName(null, "abc"), new RelationName(null, "xyz")));
		// Order is alphabetical by alias
		assertEquals("AliasMap(foo AS bar, abc AS xyz)", new AliasMap(aliases).toString());
	}
	
	public void testWithSchema() {
		RelationName table = new RelationName(null, "table");
		RelationName schema_table = new RelationName("schema", "table");
		RelationName schema_alias = new RelationName("schema", "alias");
		AliasMap m = new AliasMap(Collections.singleton(new Alias(schema_table, schema_alias)));
		assertEquals(schema_alias, m.applyTo(schema_table));
		assertEquals(table, m.applyTo(table));
	}
}