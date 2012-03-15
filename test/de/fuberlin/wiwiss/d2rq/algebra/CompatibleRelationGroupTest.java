package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.DummyDB;

public class CompatibleRelationGroupTest extends TestCase {
	Set<ProjectionSpec> projections1;
	Set<ProjectionSpec> projections2;
	RelationImpl unique;
	RelationImpl notUnique;
	
	public void setUp() {
		projections1 = Collections.<ProjectionSpec>singleton(new Attribute(null, "table", "unique"));
		projections2 = Collections.<ProjectionSpec>singleton(new Attribute(null, "table", "not_unique"));
		unique = new RelationImpl(
				new DummyDB(), AliasMap.NO_ALIASES, Expression.TRUE, Collections.<Join>emptySet(), 
				projections1, true, null, false, Relation.NO_LIMIT, Relation.NO_LIMIT);
		notUnique = new RelationImpl(
				new DummyDB(), AliasMap.NO_ALIASES, Expression.TRUE, Collections.<Join>emptySet(), 
				projections2, false, null, false, Relation.NO_LIMIT, Relation.NO_LIMIT);
	}
	
	public void testNotUniqueIsNotCompatible() {
		CompatibleRelationGroup group;
		group = new CompatibleRelationGroup();
		group.addRelation(unique);
		assertTrue(group.isCompatible(unique));
		assertFalse(group.isCompatible(notUnique));
		group = new CompatibleRelationGroup();
		group.addRelation(notUnique);
		assertFalse(group.isCompatible(unique));
	}
	
	public void testNotUniqueIsCompatibleIfSameAttributes() {
		CompatibleRelationGroup group = new CompatibleRelationGroup();
		group.addRelation(notUnique);
		assertTrue(group.isCompatible(notUnique));
	}
}
