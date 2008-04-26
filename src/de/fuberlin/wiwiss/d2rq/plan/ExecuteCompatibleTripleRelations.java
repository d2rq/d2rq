package de.fuberlin.wiwiss.d2rq.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.TripleMaker;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ExecuteCompatibleTripleRelations.java,v 1.3 2008/04/26 21:43:18 cyganiak Exp $
 */
public class ExecuteCompatibleTripleRelations implements ExecutionPlanElement, TripleMaker {

	/**
	 * Checks if two {@link Relation}s can be combined into
	 * a single SQL statement. Relations can be combined iff
	 * they access the same database and they contain exactly the same
	 * joins and WHERE clauses. If they both contain no joins, they
	 * must contain only columns from the same table.
	 * 
	 * @return <tt>true</tt> if both arguments are combinable
	 */
	public static boolean areCompatible(Relation first, Relation second) {
		if (!first.database().equals(second.database())) {
			return false;
		}
		if (!first.joinConditions().equals(second.joinConditions())) {
			return false;
		}
		if (!first.condition().equals(second.condition())) {
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
			if (!first.aliases().originalOf(tableName).equals(
					second.aliases().originalOf(tableName))) {
				return false;
			}
		}
		return true;
	}

	private Relation baseRelation;
	private List tripleMakers;
	
	public ExecuteCompatibleTripleRelations(List baseRelations) {
		this.tripleMakers = baseRelations;
		Relation base = ((TripleRelation) baseRelations.get(0)).baseRelation();
		Set projections = new HashSet();
		boolean allUnique = true;
		Iterator it = baseRelations.iterator();
		while (it.hasNext()) {
			TripleRelation bridge = (TripleRelation) it.next();
			projections.addAll(bridge.baseRelation().projections());
			allUnique = allUnique && bridge.baseRelation().isUnique();
		}
		this.baseRelation = new RelationImpl(base.database(), base.aliases(),
				base.condition(), base.joinConditions(), projections, allUnique);
	}

	public Relation relation() {
		return this.baseRelation;
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
