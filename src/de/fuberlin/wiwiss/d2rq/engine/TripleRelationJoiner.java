package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class TripleRelationJoiner {
	
	public static TripleRelationJoiner create(boolean allOptimizations) {
		return new TripleRelationJoiner(new NamesToNodeMakersMap(), 
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, allOptimizations);
	}

	private final NamesToNodeMakersMap nodeSets;
	private final List joinedTriplePatterns;
	private final List joinedTripleRelations;
	private final boolean useAllOptimizations;
	
	private TripleRelationJoiner(NamesToNodeMakersMap nodeSets, 
			List patterns, List relations, boolean useAllOptimizations) {
		this.nodeSets = nodeSets;
		this.joinedTriplePatterns = patterns;
		this.joinedTripleRelations = relations;
		this.useAllOptimizations = useAllOptimizations;
	}
	
	public List joinAll(Triple pattern, List candidates) {
		List results = new ArrayList();
		Iterator it = candidates.iterator();
		while (it.hasNext()) {
			TripleRelation tripleRelation = (TripleRelation) it.next();
			TripleRelationJoiner nextJoiner = join(pattern, tripleRelation);
			if (nextJoiner != null) {
				results.add(nextJoiner);
			}
		}
		return results;
	}
	
	public TripleRelationJoiner join(Triple pattern, TripleRelation relation) {
		List newPatterns = new ArrayList(joinedTriplePatterns);
		newPatterns.add(pattern);
		List newRelations = new ArrayList(joinedTripleRelations);
		newRelations.add(relation);

		NamesToNodeMakersMap nodeSets = new NamesToNodeMakersMap();
		for (int i = 0; i < newPatterns.size(); i++) {
			Triple t = (Triple) newPatterns.get(i);
			TripleRelation r = (TripleRelation) newRelations.get(i);

			if (useAllOptimizations) {
				/*
				 * Before adding a NodeMaker, try to adopt aliases from existing NodeMakers
				 * in order to prevent unnecessary self-joins (#2798308) 
				 */
				ArrayList names = new ArrayList();
	
				if (t.getSubject().isVariable())
					names.add(t.getSubject().getName());
				if (t.getPredicate().isVariable())
					names.add(t.getPredicate().getName());
				if (t.getObject().isVariable())
					names.add(t.getObject().getName());
	
				Iterator nameIterator = names.iterator();
				while (nameIterator.hasNext()) {
					String name = (String)nameIterator.next();
					Object n = nodeSets.toMap().get(name);
					if (n != null && n instanceof TypedNodeMaker) {
						Iterator projectionIterator = ((TypedNodeMaker)n).projectionSpecs().iterator();
						
						/*
						 * Determine the attributes that make up the node maker
						 * All must stem from the same table 
						 */
						ArrayList attributeNames = new ArrayList();
						RelationName relationName = null;
						boolean err = !projectionIterator.hasNext();
						
						while (projectionIterator.hasNext()) {
							Object o = projectionIterator.next();
							if (o instanceof Attribute) {
								Attribute a = (Attribute)o;
								if (relationName == null)
									relationName = a.relationName();
								else if (!relationName.equals(a.relationName())) {
									err = true;
									break;
								}
								attributeNames.add(a.attributeName());
							} else {
								err = true;
								break;
							}
						}
						
						/*
						 * If we would set an alias to this table...
						 */
						if (err == false) {
							RelationName originalName = ((AliasMap)nodeSets.relationAliases().get(name)).originalOf(relationName);
							if (r.baseRelation().aliases().hasAlias(originalName)) {
								/*
								 * ... and indexes are in place to guarantee uniqueness of the attribute combination...
								 */
								HashMap uniqueKeys = r.baseRelation().database().getUniqueKeyColumns(originalName);
								if (uniqueKeys != null) {
									boolean found = false;
									Iterator keyIterator = uniqueKeys.values().iterator();
									while (!found && keyIterator.hasNext()) {
										List indexColumns = (List)(keyIterator.next());
										found = attributeNames.containsAll(indexColumns);
									}
									if (found) {
										/*
										 * ... then apply alias.
										 */
										r = r.renameSingleRelation(r.baseRelation().aliases().applyTo(originalName), relationName);
										newRelations.set(i, r);
									}							
								}
							}
						}
					}
				}
			}
			
			nodeSets.addIfVariable(t.getSubject(), 
					r.nodeMaker(TripleRelation.SUBJECT), r.baseRelation().aliases());
			nodeSets.addIfVariable(t.getPredicate(), 
					r.nodeMaker(TripleRelation.PREDICATE), r.baseRelation().aliases());
			nodeSets.addIfVariable(t.getObject(), 
					r.nodeMaker(TripleRelation.OBJECT), r.baseRelation().aliases());
		}
		if (!nodeSets.satisfiable()) {
			return null;
		}
		return new TripleRelationJoiner(nodeSets, newPatterns, newRelations, useAllOptimizations);
	}
	
	public NodeRelation toNodeRelation() {
		return new NodeRelation(
				joinedBaseRelation().select(
						nodeSets.constraint()).project(nodeSets.allProjections()), 
				nodeSets.toMap());
	}
	
	private Relation joinedBaseRelation() {
		List relations = new ArrayList();
		Iterator it = joinedTripleRelations.iterator();
		while (it.hasNext()) {
			TripleRelation tripleRelation = (TripleRelation) it.next();
			relations.add(tripleRelation.baseRelation());
		}
		return joinRelations(relations, Expression.TRUE);
	}
	
	/**
	 * Static convenience function that joins several {@link Relation}s into one. Exposed here
	 * because it can be used in the old FastPath engine.
	 * 
	 * @param relations A set of {@link Relation}s
	 * @param additionalCondition An additional expression, e.g. join condition
	 * @return A relation that is the join of the inputs
	 */
	private Relation joinRelations(Collection relations, Expression additionalCondition) {
		if (relations.isEmpty()) {
			return Relation.TRUE;
		}
		ConnectedDB connectedDB = ((Relation) relations.iterator().next()).database();
		Iterator it = relations.iterator();
		AliasMap joinedAliases = AliasMap.NO_ALIASES;
		Collection expressions = new HashSet();
		expressions.add(additionalCondition);
		Set joins = new HashSet();
		Set projections = new HashSet();
		int limit = Relation.NO_LIMIT;
		int limitInverse = Relation.NO_LIMIT;
		Attribute order = null;
		boolean orderDesc = false;
		
		while (it.hasNext()) {
			Relation relation = (Relation) it.next();
			joinedAliases = joinedAliases.applyTo(relation.aliases());
			expressions.add(relation.condition());
			joins.addAll(relation.joinConditions());
			projections.addAll(relation.projections());
			orderDesc = order==null?relation.orderDesc():orderDesc;
			order = order==null?relation.order():order;
			limit = Relation.combineLimits(limit, relation.limit());
			limitInverse = Relation.combineLimits(limitInverse, relation.limitInverse());
		}
		// TODO: @@@ Figure out uniqueness instead of just false
		// I think the new relation is unique if it is joined only on unique node sets.
		// A node set is unique if it is constrained by only unique node makers.
		
		// In the meantime, copy the uniqueness from the relation if there's just one
		boolean isUnique = useAllOptimizations && relations.size()==1 && ((Relation)relations.iterator().next()).isUnique();
		return new RelationImpl(connectedDB, joinedAliases, Conjunction.create(expressions), 
				joins, projections, isUnique, order, orderDesc, limit, limitInverse);
	}
}
