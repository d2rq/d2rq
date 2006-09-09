package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.find.QueryContext;
import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.map.ColumnRenamerMap;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Expression;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.map.RenamingNodeMaker;

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
 * direction of the FK though. The way 1:n and n:m joins are typically used,
 * condition 2 will exclude most cases where the FK is on <em>T2.c_n</em>.
 * However, it will not catch cases that result from a d2rq:join on a
 * d2rq:ClassMap along an 1:1 relation. This should be considered a bug.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: JoinOptimizer.java,v 1.1 2006/09/09 15:40:05 cyganiak Exp $
 */
public class JoinOptimizer implements RDFRelation {
	private RDFRelation bridge;
	private ColumnRenamer columnRenamer;
	private Set joins;
	private Set selectColumns;
	private Map columnValues;
	private Expression condition;
	private NodeMaker subjectMaker;
	private NodeMaker predicateMaker;
	private NodeMaker objectMaker;
	
	/**
	 * Constructs a new JoinOptimizer.
	 * @param base The RDFRelation to be optimized
	 */
	public JoinOptimizer(RDFRelation base) {
		this.bridge = base;
		Map replacedColumns = new HashMap();
		Set allRequiredColumns = allRequiredColumns();
		Set requiredJoins = new HashSet(this.bridge.getJoins());
		Iterator it = this.bridge.getJoins().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			if (!isRemovableJoinSide(join.getFirstTable(), join, allRequiredColumns)) {
				continue;
			}
			requiredJoins.remove(join);
			replacedColumns.putAll(replacementColumns(join.getFirstColumns(), join));
		}
		it = this.bridge.getJoins().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			if (!isRemovableJoinSide(join.getSecondTable(), join, allRequiredColumns)) {
				continue;
			}
			requiredJoins.remove(join);
			replacedColumns.putAll(replacementColumns(join.getSecondColumns(), join));
		}
		if (replacedColumns.isEmpty()) {
			this.columnRenamer = ColumnRenamer.NULL;
			this.subjectMaker = this.bridge.getSubjectMaker();
			this.predicateMaker = this.bridge.getPredicateMaker();
			this.objectMaker = this.bridge.getObjectMaker();
		} else {
			this.columnRenamer = new ColumnRenamerMap(replacedColumns);
			this.subjectMaker = new RenamingNodeMaker(this.bridge.getSubjectMaker(), this.columnRenamer);
			this.predicateMaker = new RenamingNodeMaker(this.bridge.getPredicateMaker(), this.columnRenamer);
			this.objectMaker = new RenamingNodeMaker(this.bridge.getObjectMaker(), this.columnRenamer);
		}
		this.joins = this.columnRenamer.applyToJoinSet(requiredJoins);
		this.selectColumns = this.columnRenamer.applyToColumnSet(this.bridge.getSelectColumns());
		this.columnValues = this.columnRenamer.applyToMapKeys(this.bridge.getColumnValues());
		this.condition = this.columnRenamer.applyTo(this.bridge.condition());
	}

	public boolean couldFit(Triple t, QueryContext context) {
		return this.bridge.couldFit(t, context);
	}

	public int getEvaluationPriority() {
		return this.bridge.getEvaluationPriority();
	}

	public NodeMaker getSubjectMaker() {
		return this.subjectMaker;
	}
	
	public NodeMaker getPredicateMaker() {
		return this.predicateMaker;
	}
	
	public NodeMaker getObjectMaker() {
		return this.objectMaker;
	}
	
	public Set getJoins() {
		return this.joins;
	}
	
	public AliasMap getAliases() {
		return this.bridge.getAliases();
	}
	
	public Expression condition() {
		return this.condition;
	}

	public Map getColumnValues() {
		return this.columnValues;
	}
	
	public Set getSelectColumns() {
		return this.selectColumns;
	}

	public Database getDatabase() {
		return this.bridge.getDatabase();
	}

	public boolean mightContainDuplicates() {
		return this.bridge.mightContainDuplicates();
	}

	private Set allRequiredColumns() {
		Set results = new HashSet();
		results.addAll(this.bridge.getSelectColumns());
		results.addAll(this.bridge.condition().columns());
		results.addAll(this.bridge.getColumnValues().keySet());
		Iterator it = this.bridge.getJoins().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			results.addAll(join.getFirstColumns());
			results.addAll(join.getSecondColumns());
		}
		return results;
	}

	/**
	 * Checks if the table on one side of a join is irrelevant to the result.
	 * @param tableName A table that is on one side of the join
	 * @param join The join whose status we check
	 * @param allRequiredColumns All columns that are involved in the query
	 * @return <tt>true</tt> iff all columns from that table are covered by
	 * 		the join's condition
	 */
	private boolean isRemovableJoinSide(String tableName, Join join, Set allRequiredColumns) {
		Iterator it = allRequiredColumns.iterator();
		while (it.hasNext()) {
			Column requiredColumn = (Column) it.next();
			if (!requiredColumn.getTableName().equals(tableName)) {
				continue;		// requiredColumn is in another table
			}
			if (!join.containsColumn(requiredColumn)) {
				return false;	// requiredColumn is in our table, but not in the join condition
			}
		}
		return true;	// all columns from our table are in the join condition
	}
	
	private Map replacementColumns(Set originalColumns, Join removableJoin) {
		Map result = new HashMap();
		Iterator it = originalColumns.iterator();
		while (it.hasNext()) {
			Column originalColumn = (Column) it.next();
			result.put(originalColumn, removableJoin.getOtherSide(originalColumn));
		}
		return result;
	}

	public String toString() {
		return "JoinOptimizer(" + this.bridge + ")";
	}
}
