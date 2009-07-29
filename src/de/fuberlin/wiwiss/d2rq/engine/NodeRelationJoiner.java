package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * TODO @@@ Work in progress, not yet used anywhere 
 * TODO @@@ Doesn't handle prefixing of the relations yet 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeRelationJoiner.java,v 1.2 2009/07/29 12:03:53 fatorange Exp $
 */
public class NodeRelationJoiner {
	private final NodeRelation r1;
	private final NodeRelation r2;
	
	public NodeRelationJoiner(NodeRelation r1, NodeRelation r2) {
		this.r1 = r1;
		this.r2 = r2;
	}

	public NodeRelation joined() {
		NamesToNodeMakersMap nodeMakers = new NamesToNodeMakersMap();
		nodeMakers.addAll(r1);
		nodeMakers.addAll(r2);
		return new NodeRelation(
				combineBaseRelations()
					.select(nodeMakers.constraint())
					.project(nodeMakers.allProjections()), 
				nodeMakers.toMap());
	}
	
	private Relation combineBaseRelations() {
		ConnectedDB database = r1.baseRelation().database();
		AliasMap joinedAliases = r1.baseRelation().aliases().applyTo(r2.baseRelation().aliases());
		Expression expression = Conjunction.create(new ArrayList(2) {{ 
			add(r1.baseRelation().condition()); 
			add(r2.baseRelation().condition());
		}});
		Set joins = new HashSet() {{
			addAll(r1.baseRelation().joinConditions());
			addAll(r2.baseRelation().joinConditions());
		}};
		Set projections = new HashSet() {{
			addAll(r1.baseRelation().projections());
			addAll(r2.baseRelation().projections());
		}};
		
		boolean orderDesc = (r1.baseRelation().order() != null) ? r1.baseRelation().orderDesc() : r2.baseRelation().orderDesc();
		Attribute order = (r1.baseRelation().order() != null) ? r1.baseRelation().order() : r2.baseRelation().order();
		int limit = Relation.combineLimits(r1.baseRelation().limit(), r2.baseRelation().limit());
		int limitInverse = Relation.combineLimits(r1.baseRelation().limitInverse(), r2.baseRelation().limitInverse());
		
		// TODO: @@@ Figure out uniqueness instead of just false
		// I think the new relation is unique if it is joined only on unique node sets.
		// A node set is unique if it is constrained by only unique node makers.
		return new RelationImpl(database, joinedAliases, expression, joins, projections, false, order, orderDesc, limit, limitInverse);
	}
}
