package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;

/**
 * A group of {@link Relation}s that can be combined into a single
 * relation without changing the semantics of {@link TripleMaker}s.
 * 
 * Relations can be combined iff they access the same database and 
 * they contain exactly the same joins and WHERE clauses. If they
 * both contain no joins, they must contain only columns from the same 
 * table.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: CompatibleRelationGroup.java,v 1.2 2008/08/12 17:26:22 cyganiak Exp $
 */
public class CompatibleRelationGroup {

	private final List tripleMakers = new ArrayList();
	private final Relation firstBaseRelation;
	private boolean allUnique = true;
	private Set projections = new HashSet();
	
	public CompatibleRelationGroup(Relation first, TripleMaker tripleMaker) {
		firstBaseRelation = first;
		add(first, tripleMaker);
	}

	public boolean isCompatible(Relation otherRelation) {
		if (!firstBaseRelation.database().equals(otherRelation.database())) {
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
		if (!firstBaseRelation.isUnique() || !otherRelation.isUnique()) {
			return false;
		}
		return true;
	}

	public void add(Relation relation, TripleMaker tripleMaker) {
		tripleMakers.add(tripleMaker);
		projections.addAll(relation.projections());
		allUnique = allUnique && relation.isUnique();
	}
	
	public Relation baseRelation() {
		if (tripleMakers.size() == 1) {
			return firstBaseRelation;
		}
		return new RelationImpl(firstBaseRelation.database(), firstBaseRelation.aliases(),
				firstBaseRelation.condition(), firstBaseRelation.joinConditions(), 
				projections, allUnique);
	}
	
	public Collection tripleMakers() {
		return tripleMakers;
	}
}
