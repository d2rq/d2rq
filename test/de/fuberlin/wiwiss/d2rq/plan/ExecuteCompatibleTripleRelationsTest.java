package de.fuberlin.wiwiss.d2rq.plan;

import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.DummyDB;

public class ExecuteCompatibleTripleRelationsTest extends TestCase {

	public void testNotUniqueIsNotCompatible() {
		Set projections1 = Collections.singleton(new Attribute(null, "table", "unique"));
		Set projections2 = Collections.singleton(new Attribute(null, "table", "not_unique"));
		RelationImpl unique = new RelationImpl(
				new DummyDB(), AliasMap.NO_ALIASES, Expression.TRUE, Collections.EMPTY_SET, 
				projections1, true);
		RelationImpl notUnique = new RelationImpl(
				new DummyDB(), AliasMap.NO_ALIASES, Expression.TRUE, Collections.EMPTY_SET, 
				projections2, false);
		assertTrue(ExecuteCompatibleTripleRelations.areCompatible(unique, unique));
		assertFalse(ExecuteCompatibleTripleRelations.areCompatible(unique, notUnique));
	}
}
