/*
 * $Id: QueryCombiner.java,v 1.4 2006/08/29 15:13:12 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;


/**
 * Container for {@link TripleQuery} instances. TripleQueries can
 * be added using the {@link #add} method. The QueryCombiner will
 * combine multiple queries into a single SQL statement, if
 * possible. After adding all queries, a {@link D2RQResultIterator}
 * can be obtained from the {@link #getResultIterator} method.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class QueryCombiner {
	private Collection compatibleQueries = new ArrayList(10);

	public void add(TripleQuery newQuery) {
		Iterator it = this.compatibleQueries.iterator();
		while (it.hasNext()) {
			List queries = (List) it.next();
			if (((TripleQuery) queries.get(0)).isCombinable(newQuery)) {
				queries.add(newQuery);
				return;
			}
		}
		List newList = new ArrayList(5);
		newList.add(newQuery);
		this.compatibleQueries.add(newList);
	}

	public ExtendedIterator tripleIterator() {
		ExtendedIterator resultIterator = new NullIterator();
		Iterator it = this.compatibleQueries.iterator();
		while (it.hasNext()) {
			List queryList = (List) it.next();
			SelectStatementBuilder sql = getSQL(queryList);
			resultIterator = resultIterator.andThen(new ApplyTripleMakersIterator(
					new QueryExecutionIterator(sql.getSQLStatement(), sql.getDatabase()),
					queryList, sql.getColumnNameNumberMap()));
		}
		return resultIterator;
	}

	private SelectStatementBuilder getSQL(List queries) {
		Iterator it = queries.iterator();
		TripleQuery first = (TripleQuery) it.next();
		SelectStatementBuilder result = new SelectStatementBuilder(first.getDatabase());
		result.addJoins(first.getJoins());
		result.addColumnValues(first.getColumnValues());
		result.addConditions(first.getConditions());
		result.addSelectColumns(first.getSelectColumns());
		result.addColumnRenames(first.getReplacedColumns());
		result.setEliminateDuplicates(first.mightContainDuplicates());
		result.addAliasMap(first.getAliases());
		while (it.hasNext()) {
			TripleQuery query = (TripleQuery) it.next();
			result.addSelectColumns(query.getSelectColumns());
			result.addColumnRenames(query.getReplacedColumns());
		}
		return result;
	}
}
