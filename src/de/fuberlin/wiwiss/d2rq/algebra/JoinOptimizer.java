package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;


/**
 * <p>Removes unnecessary joins from a {@link TripleRelation} in cases
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
 * TODO: Currently this only is used for TripleRelations in FindQuery but it could be used for NodeRelations in SPARQL queries too
 * TODO: Prune unnecessary aliases after removing joins
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class JoinOptimizer {
	private TripleRelation relation;
	
	/**
	 * Constructs a new JoinOptimizer.
	 * @param relation The TripleRelation to be optimized
	 */
	public JoinOptimizer(TripleRelation relation) {
		this.relation = relation;
	}
	
	public TripleRelation optimize() {
		Map<Attribute,Attribute> replacedColumns = new HashMap<Attribute,Attribute>();
		Set<Attribute> allRequiredColumns = relation.baseRelation().allKnownAttributes();
		Set<Join> requiredJoins = new HashSet<Join>(this.relation.baseRelation().joinConditions());
		for (Join join: relation.baseRelation().joinConditions()) {
			if (!isRemovableJoin(join)) continue;
						
			boolean isRemovable1 = join.joinDirection() == Join.DIRECTION_RIGHT && isRemovableJoinSide(join.table1(), join, allRequiredColumns);
			boolean isRemovable2 = join.joinDirection() == Join.DIRECTION_LEFT && isRemovableJoinSide(join.table2(), join, allRequiredColumns);

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
		NodeMaker s = this.relation.nodeMaker(TripleRelation.SUBJECT);
		NodeMaker p = this.relation.nodeMaker(TripleRelation.PREDICATE);
		NodeMaker o = this.relation.nodeMaker(TripleRelation.OBJECT);
		Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();
		projections.addAll(s.projectionSpecs());
		projections.addAll(p.projectionSpecs());
		projections.addAll(o.projectionSpecs());
		return new TripleRelation(
				new RelationImpl(this.relation.baseRelation().database(),
					this.relation.baseRelation().aliases(),
					this.relation.baseRelation().condition(),
					this.relation.baseRelation().softCondition(),
					requiredJoins, projections,
					this.relation.baseRelation().isUnique(),
					this.relation.baseRelation().orderSpecs(),
					this.relation.baseRelation().limit(),
					this.relation.baseRelation().limitInverse()).renameColumns(renamer),
				s.renameAttributes(renamer),
				p.renameAttributes(renamer),
				o.renameAttributes(renamer));
	}

	private boolean isRemovableJoin(Join join) {
		for (Attribute side1: join.attributes1()) {
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
	private boolean isRemovableJoinSide(RelationName tableName, Join join, Set<Attribute> allRequiredColumns) {
		for (Attribute requiredColumn: allRequiredColumns) {
			if (!requiredColumn.relationName().equals(tableName)) {
				continue;		// requiredColumn is in another table
			}
			if (!join.containsColumn(requiredColumn)) {
				return false;	// requiredColumn is in our table, but not in the join condition
			}
		}
		return true;	// all columns from our table are in the join condition
	}
	
	private Map<Attribute,Attribute> replacementColumns(Collection<Attribute> originalColumns, Join removableJoin) {
		Map<Attribute,Attribute> result = new HashMap<Attribute,Attribute>();
		for (Attribute originalColumn: originalColumns) {
			result.put(originalColumn, removableJoin.equalAttribute(originalColumn));
		}
		return result;
	}
}
