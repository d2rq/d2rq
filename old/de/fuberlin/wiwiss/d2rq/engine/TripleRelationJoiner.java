package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.d2rq.algebra.AliasMap;
import org.d2rq.algebra.Attribute;
import org.d2rq.algebra.NodeRelation;
import org.d2rq.algebra.Relation;
import org.d2rq.algebra.RelationImpl;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.expr.Conjunction;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.nodes.NodeMaker;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.util.Context;

import de.fuberlin.wiwiss.d2rq.algebra.VariableConstraints;

class TripleRelationJoiner {
	
	public static TripleRelationJoiner create(Context context) {
		return new TripleRelationJoiner(new VariableConstraints(), 
				Collections.<Triple>emptyList(), Collections.<NodeRelation>emptyList(), 
				context);
	}

	private final VariableConstraints nodeSets;
	private final List<Triple> joinedTriplePatterns;
	private final List<NodeRelation> joinedTripleRelations;
	private final Context context;
	
	private TripleRelationJoiner(VariableConstraints nodeSets, 
			List<Triple> patterns, List<NodeRelation> relations, Context context) {
		this.nodeSets = nodeSets;
		this.joinedTriplePatterns = patterns;
		this.joinedTripleRelations = relations;
		this.context = context;
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
	
	private static boolean isUnique(SQLConnection database, DatabaseOp originalName, 
			Set<Identifier> attributeNames) {
		Collection<List<ColumnName>> uniqueKeys = originalName.getUniqueKeys();
		if (uniqueKeys == null) return false;
		for (List<ColumnName> indexColumns: uniqueKeys) {
			for (ColumnName column: indexColumns) {
				if (!attributeNames.contains(column.getColumn())) return false;
			}
			return true;
		}
		return false;
	}
	
	private static class AttributeSet {
		NamedOp relationName = null;
		Set<Identifier> attributeNames = new TreeSet<Identifier>();
		
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
				
				Set<Attribute> reqAttr = projection.getProjectedColumns();
				Iterator<Attribute> j = reqAttr.iterator();
				while (j.hasNext() && !err) {
					Attribute a = (Attribute) j.next();
					if (attributes.relationName == null)
						attributes.relationName = a.relationName();
					else if (!attributes.relationName.equals(a.relationName()))
						err = true;
					
					attributes.attributeNames.add(a.getColumn());
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
	
	private List<NamedOp> getRelationNames(NodeMaker nodeMaker)
	{
		List<NamedOp> result = new ArrayList<NamedOp>();
		Set<ProjectionSpec> projectionSpecs = nodeMaker.projectionSpecs();
		for (ProjectionSpec spec: projectionSpecs) {
			for (Attribute a: spec.getProjectedColumns()) {
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
					NodeMaker n = (NodeMaker) nodeSets.toMap().get(name);
					if (n != null/* && n instanceof TypedNodeMaker*/) {

						AttributeSet attributes = AttributeSet.createFrom(n);
						/*
						 * If we would set an alias to this table...
						 */
						if (attributes != null) {
							DatabaseOp originalName = nodeSets.relationAliases().get(name).originalOf(attributes.relationName.getTableName());
							if (originalName == null) {
								originalName = attributes.relationName;
							}
							if (r.getBaseTabular().aliases().hasAlias(originalName)) { 

								/*
								 * ... and indexes are in place to guarantee uniqueness of the attribute combination...
								 */
								if (isUnique(r.getBaseTabular().database(), originalName, attributes.attributeNames)) {
									
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
				List<NamedOp> relationNames = getRelationNames(
						r.nodeMaker(TripleRelation.OBJECT));
				Set<AliasOp> aliases = new HashSet<AliasOp>();
								
				for (NamedOp rname: relationNames) {
					if (r.getBaseTabular().aliases().isAlias(rname.getTableName()))
						aliases.add(AliasOp.create(r.getBaseTabular().aliases().originalOf(rname.getTableName()), rname.getTableName()));
				}
				nodeSets.add(Var.alloc(t.getObject()), r.nodeMaker(TripleRelation.OBJECT), new AliasMap(aliases));
			}
			
			if (t.getPredicate().isVariable()) {
				List<NamedOp> relationNames = getRelationNames(
						r.nodeMaker(TripleRelation.PREDICATE));
				Set<AliasOp> aliases = new HashSet<AliasOp>();
								
				for (NamedOp rname: relationNames) {
					if (r.getBaseTabular().aliases().isAlias(rname.getTableName()))
						aliases.add(AliasOp.create(r.getBaseTabular().aliases().originalOf(rname.getTableName()), rname.getTableName()));
				}
				nodeSets.add(Var.alloc(t.getPredicate()), r.nodeMaker(TripleRelation.PREDICATE), new AliasMap(aliases));
			}
			
			if (t.getSubject().isVariable()) {
				List<NamedOp> relationNames = getRelationNames(
						r.nodeMaker(TripleRelation.SUBJECT));
				Set<AliasOp> aliases = new HashSet<AliasOp>();
								
				for (NamedOp rname: relationNames) {
					if (r.getBaseTabular().aliases().isAlias(rname.getTableName()))
						aliases.add(AliasOp.create(r.getBaseTabular().aliases().originalOf(rname.getTableName()), rname.getTableName()));
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
			relations.add(tripleRelation.getBaseTabular());
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
		SQLConnection connectedDB = ((Relation) relations.iterator().next()).database();
		Set<NamedOp> tables = new HashSet<NamedOp>();
		AliasMap joinedAliases = AliasMap.NO_ALIASES;
		Collection<Expression> expressions = new HashSet<Expression>();
		expressions.add(additionalCondition);
		Collection<Expression> softConditions = new HashSet<Expression>();
		Set<ColumnListEquality> joins = new HashSet<ColumnListEquality>();
		Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();
		int limit = Relation.NO_LIMIT;
		int limitInverse = Relation.NO_LIMIT;
		List<OrderSpec> orderSpecs = null;
		
		for (Relation relation: relations) {
			tables.addAll(relation.tables());
			// FIXME Is this joinedAliases correct?
			joinedAliases = joinedAliases.getRenamer().applyTo(relation.aliases());
			expressions.add(relation.getCondition());
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
		return new RelationImpl(connectedDB, tables, joinedAliases,
				Conjunction.create(expressions), Conjunction.create(softConditions),
				joins, projections, isUnique, orderSpecs, limit, limitInverse);
	}
}
