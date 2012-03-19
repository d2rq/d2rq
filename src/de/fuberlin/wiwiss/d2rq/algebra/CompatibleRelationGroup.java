package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.engine.NodeRelation;
import de.fuberlin.wiwiss.d2rq.find.TripleMaker;

/**
 * A group of {@link Relation}s that can be combined into a single
 * relation without changing the semantics of {@link de.fuberlin.wiwiss.d2rq.nodes.NodeMaker}s.
 * 
 * Relations can be combined iff they access the same database and 
 * they contain exactly the same joins and WHERE clauses. If they
 * both contain no joins, they must contain only columns from the same 
 * table.
 * 
 * TODO: groupNodeRelations and groupTripleRelations are virtually identical
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class CompatibleRelationGroup {

	public static Collection groupTripleRelations(Collection tripleRelations) {
		Collection result = new ArrayList();
		Iterator it = tripleRelations.iterator();
		while (it.hasNext()) {
			TripleRelation tripleRelation = (TripleRelation) it.next();
			addTripleRelation(tripleRelation, result);
		}
		return result;
	}
	
	private static void addTripleRelation(TripleRelation tripleRelation, Collection groups) {
		Iterator it = groups.iterator();
		while (it.hasNext()) {
			CompatibleRelationGroup group = (CompatibleRelationGroup) it.next();
			if (group.isCompatible(tripleRelation.baseRelation())) {
				group.addRelation(tripleRelation.baseRelation());
				group.addTripleMaker(new TripleMaker(tripleRelation));
				return;
			}
		}
		CompatibleRelationGroup newGroup = new CompatibleRelationGroup(
				tripleRelation.baseRelation());
		newGroup.addTripleMaker(new TripleMaker(tripleRelation));
		groups.add(newGroup);
	}
	
	public static Collection groupNodeRelations(Collection nodeRelations) {
		Collection result = new ArrayList();
		Iterator it = nodeRelations.iterator();
		while (it.hasNext()) {
			NodeRelation nodeRelation = (NodeRelation) it.next();
			addNodeRelation(nodeRelation, result);
		}
		return result;
	}
	
	private static void addNodeRelation(NodeRelation nodeRelation, Collection groups) {
		Iterator it = groups.iterator();
		while (it.hasNext()) {
			CompatibleRelationGroup group = (CompatibleRelationGroup) it.next();
			if (group.isCompatible(nodeRelation.baseRelation())) {
				group.addRelation(nodeRelation.baseRelation());
				group.addBindingMaker(new BindingMaker(nodeRelation));
				return;
			}
		}
		CompatibleRelationGroup newGroup = new CompatibleRelationGroup(
				nodeRelation.baseRelation());
		newGroup.addBindingMaker(new BindingMaker(nodeRelation));
		groups.add(newGroup);
	}
	
	private final List tripleMakers = new ArrayList();
	private final List bindingMakers = new ArrayList();
	private final Relation firstBaseRelation;
	private boolean allUnique = true;
	private int relationCounter = 0;
	private Set projections = new HashSet();
	
	public CompatibleRelationGroup(Relation first) {
		firstBaseRelation = first;
		addRelation(first);
	}

	public boolean isCompatible(Relation otherRelation) {
		if (firstBaseRelation.database()==null || !firstBaseRelation.database().equals(otherRelation.database())) {
			return false;
		}
		if (!firstBaseRelation.joinConditions().equals(otherRelation.joinConditions())) {
			return false;
		}
		if (!firstBaseRelation.condition().equals(otherRelation.condition())) {
			return false;
		}
		Set firstTables = firstBaseRelation.tables();
		Set secondTables = otherRelation.tables();
		if (!firstTables.equals(secondTables)) {
			return false;
		}
		Iterator it = firstTables.iterator();
		while (it.hasNext()) {
			RelationName tableName = (RelationName) it.next();
			if (!firstBaseRelation.aliases().originalOf(tableName).equals(
					otherRelation.aliases().originalOf(tableName))) {
				return false;
			}
		}
		if (firstBaseRelation.projections().equals(otherRelation.projections())) {
			return true;
		}
		if (firstBaseRelation.isUnique() && otherRelation.isUnique()) {
			return true;
		}
		return false;
	}

	public void addRelation(Relation relation) {
		projections.addAll(relation.projections());
		allUnique = allUnique && relation.isUnique();
		relationCounter++;
	}

	public void addTripleMaker(TripleMaker tripleMaker) {
		tripleMakers.add(tripleMaker);
	}
	
	public void addBindingMaker(BindingMaker bindingMaker) {
		bindingMakers.add(bindingMaker);
	}
	
	public Relation baseRelation() {
		if (relationCounter == 1) {
			return firstBaseRelation;
		}
		return new RelationImpl(firstBaseRelation.database(), firstBaseRelation.aliases(),
				firstBaseRelation.condition(), firstBaseRelation.joinConditions(), 
				projections, allUnique, firstBaseRelation.order(), firstBaseRelation.orderDesc(), firstBaseRelation.limit(), firstBaseRelation.limitInverse());
	}
	
	public Collection tripleMakers() {
		return tripleMakers;
	}
	
	public Collection bindingMakers() {
		return bindingMakers;
	}
}
