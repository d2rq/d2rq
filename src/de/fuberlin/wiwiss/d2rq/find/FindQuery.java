package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;

import de.fuberlin.wiwiss.d2rq.algebra.JoinOptimizer;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleSelection;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.map.TripleMaker;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;


/**
 * A find query on a list of property bridges. Results are delivered
 * as an iterator. Will combine queries on multiple bridges into one
 * SQL statement where possible.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindQuery.java,v 1.5 2006/09/09 15:40:05 cyganiak Exp $
 */
public class FindQuery {
	private Triple triplePattern;
	private List propertyBridges;
	private Collection compatibleQueries;
	
	public FindQuery(Triple triplePattern, List propertyBridges) {
		this.triplePattern = triplePattern;
		this.propertyBridges = propertyBridges;
	}

	public ExtendedIterator iterator() {
		this.compatibleQueries = new ArrayList();
		findPropertyBridges();
		ExtendedIterator result = new NullIterator();
		Iterator it = this.compatibleQueries.iterator();
		while (it.hasNext()) {
			List queryList = (List) it.next();
			result = result.andThen(resultIterator(queryList));
		}
		return result;
	}
	
	private void findPropertyBridges() {
		QueryContext context = new QueryContext();
		Iterator it = this.propertyBridges.iterator();
		while (it.hasNext()) {
			RDFRelation bridge = (RDFRelation) it.next();
			if (bridge.couldFit(this.triplePattern, context)) {
				addBridge(bridge);
			}
		}
	}
	
	private void addBridge(RDFRelation bridge) {
		RDFRelation newQuery = new JoinOptimizer(new TripleSelection(bridge, this.triplePattern));
		Iterator it = this.compatibleQueries.iterator();
		while (it.hasNext()) {
			List queries = (List) it.next();
			if (isCombinable((RDFRelation) queries.get(0), newQuery)) {
				queries.add(newQuery);
				return;
			}
		}
		List newList = new ArrayList();
		newList.add(newQuery);
		this.compatibleQueries.add(newList);
	}

	private ClosableIterator resultIterator(List queryList) {
		Iterator it = queryList.iterator();
		RDFRelation first = (RDFRelation) it.next();
		SelectStatementBuilder sql = new SelectStatementBuilder(first.getDatabase());
		sql.addAliasMap(first.getAliases());
		sql.addJoins(first.getJoins());
		sql.addColumnValues(first.getColumnValues());
		sql.addCondition(first.condition());
		sql.addSelectColumns(first.getSelectColumns());
		sql.setEliminateDuplicates(first.mightContainDuplicates());
		while (it.hasNext()) {
			RDFRelation query = (RDFRelation) it.next();
			sql.addSelectColumns(query.getSelectColumns());
		}
		if (sql.isTrivial()) {
			return new ApplyTripleMakersIterator(
					new SingletonIterator(new String[]{}), queryList);
		}
		Collection tripleMakers = new ArrayList();
		it = queryList.iterator();
		while (it.hasNext()) {
			RDFRelation relation = (RDFRelation) it.next();
			tripleMakers.add(new TripleMaker(relation, sql.getColumnNameNumberMap()));
		}
		return new ApplyTripleMakersIterator(
				new QueryExecutionIterator(sql.getSQLStatement(), sql.getDatabase()),
				tripleMakers);
	}
	
	/**
	 * Checks if two {@link RDFRelation}s can be combined into
	 * a single SQL statement. Relations can be combined iff
	 * they access the same database and they contain exactly the same
	 * joins and WHERE clauses. If they both contain no joins, they
	 * must contain only columns from the same table.
	 * @return <tt>true</tt> if both arguments are combinable
	 */
	private static boolean isCombinable(RDFRelation first, RDFRelation second) {
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
}
