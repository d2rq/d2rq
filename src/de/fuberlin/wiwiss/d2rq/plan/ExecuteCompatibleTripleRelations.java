package de.fuberlin.wiwiss.d2rq.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.TripleMaker;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ExecuteCompatibleTripleRelations.java,v 1.1 2008/04/25 11:25:05 cyganiak Exp $
 */
public class ExecuteCompatibleTripleRelations implements ExecutionPlanElement, TripleMaker {

	/**
	 * Checks if two {@link TripleRelation}s can be combined into
	 * a single SQL statement. Relations can be combined iff
	 * they access the same database and they contain exactly the same
	 * joins and WHERE clauses. If they both contain no joins, they
	 * must contain only columns from the same table.
	 * 
	 * TODO This should be done via Relation.equals?
	 * 
	 * @return <tt>true</tt> if both arguments are combinable
	 */
	public static boolean areCompatible(TripleRelation first, TripleRelation second) {
		if (!first.baseRelation().database().equals(second.baseRelation().database())) {
			return false;
		}
		if (!first.isUnique() || !second.isUnique()) {
			return false;
		}
		if (!first.baseRelation().joinConditions().equals(second.baseRelation().joinConditions())) {
			return false;
		}
		if (!first.baseRelation().condition().equals(second.baseRelation().condition())) {
			return false;
		}
		Set firstTables = first.tables();
		Set secondTables = second.tables();
		if (!firstTables.equals(secondTables)) {
			return false;
		}
		Iterator it = firstTables.iterator();
		while (it.hasNext()) {
			RelationName tableName = (RelationName) it.next();
			if (!first.baseRelation().aliases().originalOf(tableName).equals(
					second.baseRelation().aliases().originalOf(tableName))) {
				return false;
			}
		}
		return true;
	}

	private Relation baseRelation;
	private List tripleMakers;
	private Set projectionSpecs = new HashSet();
	
	public ExecuteCompatibleTripleRelations(List baseRelations) {
		this.baseRelation = ((TripleRelation) baseRelations.get(0)).baseRelation();
		this.tripleMakers = baseRelations;
		Iterator it = baseRelations.iterator();
		while (it.hasNext()) {
			TripleRelation relation = (TripleRelation) it.next();
			this.projectionSpecs.addAll(relation.projectionSpecs());
		}
	}

	public Relation baseRelation() {
		return this.baseRelation;
	}
	
	public Set projectionSpecs() {
		return this.projectionSpecs;
	}
	
	public Collection makeTriples(ResultRow row) {
		List result = new ArrayList();
		Iterator it = this.tripleMakers.iterator();
		while (it.hasNext()) {
			TripleMaker relation = (TripleMaker) it.next();
			result.addAll(relation.makeTriples(row));
		}
		return result;
	}
	
	public void visit(ExecutionPlanVisitor visitor) {
		visitor.visit(this);
	}
}
