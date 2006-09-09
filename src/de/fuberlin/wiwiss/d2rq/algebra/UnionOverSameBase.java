package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.find.QueryContext;
import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Expression;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

public class UnionOverSameBase implements RDFRelation {

	/**
	 * Checks if two {@link RDFRelation}s can be combined into
	 * a single SQL statement. Relations can be combined iff
	 * they access the same database and they contain exactly the same
	 * joins and WHERE clauses. If they both contain no joins, they
	 * must contain only columns from the same table.
	 * @return <tt>true</tt> if both arguments are combinable
	 */
	public static boolean isSameBase(RDFRelation first, RDFRelation second) {
		if (!first.getDatabase().equals(second.getDatabase())) {
			return false;
		}
		if (first.mightContainDuplicates() || second.mightContainDuplicates()) {
			return false;
		}
		if (!first.getJoins().equals(second.getJoins())) {
			return false;
		}
		if (!first.condition().equals(second.condition())) {
			return false;
		}
		if (!first.getColumnValues().equals(second.getColumnValues())) {
			return false;
		}
		if (!first.getAliases().equals(second.getAliases())) {
			return false;
		}
		if (!tables(first).equals(tables(second))) {
			return false;
		}
		return true;
	}

	/**
	 * @return All table names used in the argument
	 */
	private static Set tables(RDFRelation query) {
		Set results = new HashSet();
		Iterator it = query.getColumnValues().keySet().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			results.add(column.getTableName());
		}
		it = query.getSelectColumns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			results.add(column.getTableName());
		}
		it = query.condition().columns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			results.add(column.getTableName());
		}
		it = query.getJoins().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			results.add(join.getFirstTable());
			results.add(join.getSecondTable());
		}
		return results;
	}

	private RDFRelation firstBase;
	private List baseRelations;
	private Set selectColumns = new HashSet();
	
	public UnionOverSameBase(List baseRelations) {
		this.firstBase = (RDFRelation) baseRelations.get(0);
		this.baseRelations = baseRelations;
		Iterator it = baseRelations.iterator();
		while (it.hasNext()) {
			RDFRelation relation = (RDFRelation) it.next();
			this.selectColumns.addAll(relation.getSelectColumns());
		}
	}
	
	public Expression condition() {
		return this.firstBase.condition();
	}

	public boolean couldFit(Triple t, QueryContext context) {
		return this.firstBase.couldFit(t, context);
	}

	public AliasMap getAliases() {
		return this.firstBase.getAliases();
	}

	public Map getColumnValues() {
		return this.firstBase.getColumnValues();
	}

	public Database getDatabase() {
		return this.firstBase.getDatabase();
	}

	public int getEvaluationPriority() {
		return this.firstBase.getEvaluationPriority();
	}

	public Set getJoins() {
		return this.firstBase.getJoins();
	}

	public Set getSelectColumns() {
		return this.selectColumns;
	}
	
	public boolean mightContainDuplicates() {
		return this.firstBase.mightContainDuplicates();
	}
	
	public Collection makeTriples(ResultRow row) {
		List result = new ArrayList();
		Iterator it = this.baseRelations.iterator();
		while (it.hasNext()) {
			RDFRelation relation = (RDFRelation) it.next();
			result.addAll(relation.makeTriples(row));
		}
		return result;
	}
	
	public NodeMaker getSubjectMaker() {
		throw new UnsupportedOperationException();
	}
	
	public NodeMaker getPredicateMaker() {
		throw new UnsupportedOperationException();
	}

	public NodeMaker getObjectMaker() {
		throw new UnsupportedOperationException();
	}
}
