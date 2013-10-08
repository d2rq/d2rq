package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Var;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.VariableConstraints;
import de.fuberlin.wiwiss.d2rq.algebra.NodeRelation;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

class TripleRelationJoiner {
	
	public static TripleRelationJoiner create(boolean allOptimizations) {
		return new TripleRelationJoiner(new VariableConstraints(), 
				Collections.<Triple>emptyList(), Collections.<NodeRelation>emptyList(), 
				allOptimizations);
	}

	private final VariableConstraints nodeSets;
	private final List<Triple> joinedTriplePatterns;
	private final List<NodeRelation> joinedTripleRelations;
	private final boolean useAllOptimizations;
	
	private TripleRelationJoiner(VariableConstraints nodeSets, 
			List<Triple> patterns, List<NodeRelation> relations, boolean useAllOptimizations) {
		this.nodeSets = nodeSets;
		this.joinedTriplePatterns = patterns;
		this.joinedTripleRelations = relations;
		this.useAllOptimizations = useAllOptimizations;
	}
	
	public List<TripleRelationJoiner> joinAll(Triple pattern, 
			List<NodeRelation> candidates) {
		List<TripleRelationJoiner> results = new ArrayList<TripleRelationJoiner>();
		for (NodeRelation tripleRelation: candidates) {
			TripleRelationJoiner nextJoiner = join(pattern, tripleRelation);
			if (nextJoiner != null) {
				results.add(nextJoiner);
			}
		}
		return results;
	}
	
	private static boolean isUnique(ConnectedDB database, RelationName originalName, 
			Set<String> attributeNames)
	{
		Map<String,List<String>> uniqueKeys = database.getUniqueKeyColumns(originalName);
		if (uniqueKeys != null) {
		for (List<String> indexColumns: uniqueKeys.values()) {
				if (attributeNames.containsAll(indexColumns)) {
					return true;
				}
			}
		}
		List<Attribute> primaryKeys = database.schemaInspector().primaryKeyColumns(originalName);
		if (primaryKeys != null) {
			for (Attribute attr: primaryKeys) {
				if (!attributeNames.contains(attr.attributeName()))
					return false;
			}
			return true;
		}
		return false;
	}
	
	private static class AttributeSet {
		RelationName relationName = null;
		Set<String> attributeNames = new TreeSet<String>();
		
		/*
		 * Determine the attributes that make up a nodemakers projection spec
		 * all must stem from the same table 
		 */
		static AttributeSet createFrom(NodeMaker nodeMaker)
		{
			AttributeSet attributes = new AttributeSet();
			Set<ProjectionSpec> projectionSpecs = nodeMaker.projectionSpecs();
			boolean err = projectionSpecs.isEmpty();
			
			Iterator<ProjectionSpec> projectionIterator = projectionSpecs.iterator();
			while (projectionIterator.hasNext() && !err) {
				ProjectionSpec projection = (ProjectionSpec) projectionIterator.next();
				
				Set<Attribute> reqAttr = projection.requiredAttributes();
				Iterator<Attribute> j = reqAttr.iterator();
				while (j.hasNext() && !err) {
					Attribute a = (Attribute) j.next();
					if (attributes.relationName == null)
						attributes.relationName = a.relationName();
					else if (!attributes.relationName.equals(a.relationName()))
						err = true;
					
					attributes.attributeNames.add(a.attributeName());
				}
				
//				if (projection instanceof Attribute) {
//					Attribute a = (Attribute) projection;
//					if (attributes.relationName == null)
//						attributes.relationName = a.relationName();
//					else if (!attributes.relationName.equals(a.relationName()))
//						err = true;
//					
//					attributes.attributeNames.add(a.attributeName());
//				} else {
//					err = true;
//				}
			}
			
			if (!err)
				return attributes;
			
			return null;
		}
	}
	
	private List<RelationName> getRelationNames(NodeMaker nodeMaker)
	{
		List<RelationName> result = new ArrayList<RelationName>();
		Set<ProjectionSpec> projectionSpecs = nodeMaker.projectionSpecs();
		for (ProjectionSpec spec: projectionSpecs) {
			for (Attribute a: spec.requiredAttributes()) {
				result.add(a.relationName());
			}
		}
		return result;
	}
	
	public TripleRelationJoiner join(Triple pattern, NodeRelation relation) {
		List<Triple> newPatterns = new ArrayList<Triple>(joinedTriplePatterns);
		newPatterns.add(pattern);
		List<NodeRelation> newRelations = new ArrayList<NodeRelation>(joinedTripleRelations);
		newRelations.add(relation);

		VariableConstraints nodeSets = new VariableConstraints();
		for (int i = 0; i < newPatterns.size(); i++) {
			Triple t = (Triple) newPatterns.get(i);
			NodeRelation r = newRelations.get(i);

			if (useAllOptimizations) {
				/*
				 * Before adding a NodeMaker, try to adopt aliases from existing NodeMakers
				 * in order to prevent unnecessary self-joins (#2798308) 
				 */
				// This failed when originalName had more than 1 alias r.baseRelation (fixed, GM)
				List<String> names = new ArrayList<String>();
	
				if (t.getSubject().isVariable())
					names.add(t.getSubject().getName());
				if (t.getPredicate().isVariable())
					names.add(t.getPredicate().getName());
				if (t.getObject().isVariable())
					names.add(t.getObject().getName());
	
				for (String name: names) {
					Var nameVar = Var.alloc(name);
					NodeMaker n = (NodeMaker) nodeSets.toMap().get(nameVar);
					if (n != null/* && n instanceof TypedNodeMaker*/) {

						AttributeSet attributes = AttributeSet.createFrom(n);
						/*
						 * If we would set an alias to this table...
						 */
						if (attributes != null) {
							AliasMap amap = (AliasMap)(nodeSets.relationAliases().get(nameVar));
							RelationName originalName = amap.originalOf(attributes.relationName);
							if (r.baseRelation().aliases().hasAlias(originalName)) { 

								/*
								 * ... and indexes are in place to guarantee uniqueness of the attribute combination...
								 */
								if (isUnique(r.baseRelation().database(), originalName, attributes.attributeNames)) {
									
									if (t.getSubject().isVariable() && t.getSubject().getName().equals(name)) {
										// ... then first find the right relation name...
										AttributeSet existing = AttributeSet.createFrom(r.nodeMaker(TripleRelation.SUBJECT));
										if (existing != null && existing.attributeNames.equals(attributes.attributeNames)) {
											// ... then apply it
											r = r.renameSingleRelation(existing.relationName, attributes.relationName);
											newRelations.set(i, r);
										}
									}
									
									if (t.getPredicate().isVariable() && t.getPredicate().getName().equals(name)) {
										// ... then first find the right relation name...
										AttributeSet existing = AttributeSet.createFrom(r.nodeMaker(TripleRelation.PREDICATE));
										if (existing != null && existing.attributeNames.equals(attributes.attributeNames)) {
											// ... then apply it
											r = r.renameSingleRelation(existing.relationName, attributes.relationName);
											newRelations.set(i, r);
										}
									}
									
									if (t.getObject().isVariable() && t.getObject().getName().equals(name)) {
										// ... then first find the right relation name...
										AttributeSet existing = AttributeSet.createFrom(r.nodeMaker(TripleRelation.OBJECT));
										if (existing != null && existing.attributeNames.equals(attributes.attributeNames)) {
											// ... then apply it
											r = r.renameSingleRelation(existing.relationName, attributes.relationName);
											newRelations.set(i, r);
										}
									}
								}							
							}
						}
					}
				}
			}
			
			if (t.getObject().isVariable()) {
				List<RelationName> relationNames = getRelationNames(
						r.nodeMaker(TripleRelation.OBJECT));
				Set<Alias> aliases = new HashSet<Alias>();
								
				for (RelationName rname: relationNames) {
					if (r.baseRelation().aliases().isAlias(rname))
						aliases.add(new AliasMap.Alias(r.baseRelation().aliases().originalOf(rname), rname));
				}
				nodeSets.add(Var.alloc(t.getObject()), r.nodeMaker(TripleRelation.OBJECT), new AliasMap(aliases));
			}
			
			if (t.getPredicate().isVariable()) {
				List<RelationName> relationNames = getRelationNames(
						r.nodeMaker(TripleRelation.PREDICATE));
				Set<Alias> aliases = new HashSet<Alias>();
								
				for (RelationName rname: relationNames) {
					if (r.baseRelation().aliases().isAlias(rname))
						aliases.add(new AliasMap.Alias(r.baseRelation().aliases().originalOf(rname), rname));
				}
				nodeSets.add(Var.alloc(t.getPredicate()), r.nodeMaker(TripleRelation.PREDICATE), new AliasMap(aliases));
			}
			
			if (t.getSubject().isVariable()) {
				List<RelationName> relationNames = getRelationNames(
						r.nodeMaker(TripleRelation.SUBJECT));
				Set<Alias> aliases = new HashSet<Alias>();
								
				for (RelationName rname: relationNames) {
					if (r.baseRelation().aliases().isAlias(rname))
						aliases.add(new AliasMap.Alias(r.baseRelation().aliases().originalOf(rname), rname));
				}
				nodeSets.add(Var.alloc(t.getSubject()), r.nodeMaker(TripleRelation.SUBJECT), new AliasMap(aliases));
			}

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
		List<Relation> relations = new ArrayList<Relation>();
		for (NodeRelation tripleRelation: joinedTripleRelations) {
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
	private Relation joinRelations(Collection<Relation> relations, Expression additionalCondition) {
		if (relations.isEmpty()) {
			return Relation.TRUE;
		}
		ConnectedDB connectedDB = ((Relation) relations.iterator().next()).database();
		AliasMap joinedAliases = AliasMap.NO_ALIASES;
		Collection<Expression> expressions = new HashSet<Expression>();
		expressions.add(additionalCondition);
		Collection<Expression> softConditions = new HashSet<Expression>();
		Set<Join> joins = new HashSet<Join>();
		Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();
		int limit = Relation.NO_LIMIT;
		int limitInverse = Relation.NO_LIMIT;
		List<OrderSpec> orderSpecs = null;
		
		for (Relation relation: relations) {
			joinedAliases = joinedAliases.applyTo(relation.aliases());
			expressions.add(relation.condition());
			softConditions.add(relation.softCondition());
			joins.addAll(relation.joinConditions());
			projections.addAll(relation.projections());
			orderSpecs = orderSpecs == null ? relation.orderSpecs() : orderSpecs;
			limit = Relation.combineLimits(limit, relation.limit());
			limitInverse = Relation.combineLimits(limitInverse, relation.limitInverse());
		}
		// TODO: Determine correct uniqueness instead of just false.
		// The new relation is unique if it is joined only on unique node sets.
		// A node set is unique if it is constrained by only unique node makers.
		
		// In the meantime, copy the uniqueness from the relation if there's just one
		boolean isUnique = relations.size() == 1 && (relations.iterator().next()).isUnique();
		return new RelationImpl(connectedDB, joinedAliases, Conjunction.create(expressions), 
				Conjunction.create(softConditions),
				joins, projections, isUnique, orderSpecs, limit, limitInverse);
	}
}
