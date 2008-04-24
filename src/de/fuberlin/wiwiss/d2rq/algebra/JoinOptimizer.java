package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * <p>Removes unnecessary joins from an {@link RDFRelation} in cases
 * where this is possible without affecting the result. This is an
 * optimization.</p>
 * 
 * <p>A join J from table T1 to table T2 with join condition 
 * <em>T1.c_1 = T2.c_1 && T1.c_2 = T2.c_2 && ...</em>
 * can be removed if these conditions hold:</p>
 * 
 * <ol>
 *   <li>The only join mentioning T2 is J.</li>
 *   <li>All columns of T2 that are selected or constrained or used in
 *     an expression occur in J's join condition.</li>
 *   <li>All values of <em>T1.c_n</em> are guaranteed to occur
 *     in <em>T2.c_n</em>, that is, there is a foreign key constraint
 *     on <em>T1.c_n</em>.</li>
 * </ol>
 * 
 * <p>In this case, J can be dropped, and all mentions of <em>T2.c_n</em>
 * can be replaced with <em>T1.c_n</em>.</p>
 * 
 * TODO: <strong>Note:</strong> The third condition is currently not enforced.
 * This is not a problem in most situations, because d2rq:join is typically
 * used along an FK constraint. At this point in the code, we don't know the
 * direction of the FK though. If the FK is on <em>T2.c_n</em>, then removing
 * the join may result in incorrect results.
 * 
 * The way 1:n and n:m joins are typically used, condition 2 will exclude most
 * cases where the FK is on <em>T2.c_n</em>.
 * 
 * Another heuristic catches many 1:1 cases: If a join looks removable, but
 * switching the order of the two tables around also results in a removable
 * join, then we don't remove it, because we have no indication in which
 * direction the FK is pointing and might pick the wrong one, so the safe
 * choice is not to optimize. 
 *  
 * However, there are still cases where a join will be incorrectly removed,
 * e.g. a 1:1 join with a condition on a column not involved in the join.
 * 
 * This is a bug. The only way to be sure if a join can be removed is to check
 * the database schema (or ask the user) if there is indeed a FK constraint on
 * <em>T1.c_n</em>.
 * 
 * TODO: Prune unnecessary aliases after removing joins
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: JoinOptimizer.java,v 1.16 2008/04/24 17:48:52 cyganiak Exp $
 */
public class JoinOptimizer {
	private RDFRelation relation;
	
	/**
	 * Constructs a new JoinOptimizer.
	 * @param base The RDFRelation to be optimized
	 */
	public JoinOptimizer(RDFRelation base) {
		this.relation = base;
	}
	
	public RDFRelation optimize() {
		Map replacedColumns = new HashMap();
		Set allRequiredColumns = relation.allKnownAttributes();
		Set requiredJoins = new HashSet(this.relation.baseRelation().joinConditions());
		Iterator it = this.relation.baseRelation().joinConditions().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			if (!isRemovableJoin(join)) continue;
			
			boolean isRemovable1 = isRemovableJoinSide(join.table1(), join, allRequiredColumns);
			boolean isRemovable2 = isRemovableJoinSide(join.table2(), join, allRequiredColumns);

			// Ugly heuristic
			if (isRemovable1 && isRemovable2) continue;

			if (isRemovable1) {
				requiredJoins.remove(join);
				replacedColumns.putAll(replacementColumns(join.attributes1(), join));
			}
			if (isRemovable2) {
				requiredJoins.remove(join);
				replacedColumns.putAll(replacementColumns(join.attributes2(), join));
			}
		}
		if (replacedColumns.isEmpty()) {
			return this.relation;
		}
		ColumnRenamer renamer = new ColumnRenamerMap(replacedColumns);
		return new TripleRelation(
				new RelationImpl(this.relation.baseRelation().database(),
					this.relation.baseRelation().aliases(),
					this.relation.baseRelation().condition(),
					requiredJoins).renameColumns(renamer),
				this.relation.nodeMaker(0).renameColumns(renamer, MutableRelation.DUMMY),
				this.relation.nodeMaker(1).renameColumns(renamer, MutableRelation.DUMMY),
				this.relation.nodeMaker(2).renameColumns(renamer, MutableRelation.DUMMY));
	}

	private boolean isRemovableJoin(Join join) {
		Iterator it = join.attributes1().iterator();
		while (it.hasNext()) {
			Attribute side1 = (Attribute) it.next();
			Attribute side2 = join.equalAttribute(side1);
			if (!relation.baseRelation().database().areCompatibleFormats(
					relation.baseRelation().aliases().originalOf(side1), 
					relation.baseRelation().aliases().originalOf(side2))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if the table on one side of a join is irrelevant to the result.
	 * @param tableName A table that is on one side of the join
	 * @param join The join whose status we check
	 * @param allRequiredColumns All columns that are involved in the query
	 * @return <tt>true</tt> iff all columns from that table are covered by
	 * 		the join's condition
	 */
	private boolean isRemovableJoinSide(RelationName tableName, Join join, Set allRequiredColumns) {
		Iterator it = allRequiredColumns.iterator();
		while (it.hasNext()) {
			Attribute requiredColumn = (Attribute) it.next();
			if (!requiredColumn.relationName().equals(tableName)) {
				continue;		// requiredColumn is in another table
			}
			if (!join.containsColumn(requiredColumn)) {
				return false;	// requiredColumn is in our table, but not in the join condition
			}
		}
		return true;	// all columns from our table are in the join condition
	}
	
	private Map replacementColumns(Collection originalColumns, Join removableJoin) {
		Map result = new HashMap();
		Iterator it = originalColumns.iterator();
		while (it.hasNext()) {
			Attribute originalColumn = (Attribute) it.next();
			result.put(originalColumn, removableJoin.equalAttribute(originalColumn));
		}
		return result;
	}
}
