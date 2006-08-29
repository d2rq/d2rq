package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;


/**
 * A find query on a list of property bridges. Results are delivered
 * as an iterator. Will combine queries on multiple bridges into one
 * SQL statement where possible.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindQuery.java,v 1.1 2006/08/29 16:12:14 cyganiak Exp $
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
		this.compatibleQueries = new ArrayList(10);
		findPropertyBridges();
		ExtendedIterator result = new NullIterator();
		Iterator it = this.compatibleQueries.iterator();
		while (it.hasNext()) {
			List queryList = (List) it.next();
			SelectStatementBuilder sql = getSQL(queryList);
			result = result.andThen(new ApplyTripleMakersIterator(
					new QueryExecutionIterator(sql.getSQLStatement(), sql.getDatabase()),
					queryList, sql.getColumnNameNumberMap()));
		}
		return result;
	}
	
	private void findPropertyBridges() {
		QueryContext context = new QueryContext();
		Iterator it = this.propertyBridges.iterator();
		while (it.hasNext()) {
			PropertyBridge bridge = (PropertyBridge) it.next();
			if (bridge.couldFit(this.triplePattern, context)) {
				addBridge(bridge);
			}
		}
	}
	
	private void addBridge(PropertyBridge bridge) {
		PropertyBridgeQuery newQuery = new PropertyBridgeQuery(bridge, this.triplePattern);
		Iterator it = this.compatibleQueries.iterator();
		while (it.hasNext()) {
			List queries = (List) it.next();
			if (((PropertyBridgeQuery) queries.get(0)).isCombinable(newQuery)) {
				queries.add(newQuery);
				return;
			}
		}
		List newList = new ArrayList(5);
		newList.add(newQuery);
		this.compatibleQueries.add(newList);
	}

	private SelectStatementBuilder getSQL(List queries) {
		Iterator it = queries.iterator();
		PropertyBridgeQuery first = (PropertyBridgeQuery) it.next();
		SelectStatementBuilder result = new SelectStatementBuilder(first.getDatabase());
		result.addJoins(first.getJoins());
		result.addColumnValues(first.getColumnValues());
		result.addConditions(first.getConditions());
		result.addSelectColumns(first.getSelectColumns());
		result.addColumnRenames(first.getReplacedColumns());
		result.setEliminateDuplicates(first.mightContainDuplicates());
		result.addAliasMap(first.getAliases());
		while (it.hasNext()) {
			PropertyBridgeQuery query = (PropertyBridgeQuery) it.next();
			result.addSelectColumns(query.getSelectColumns());
			result.addColumnRenames(query.getReplacedColumns());
		}
		return result;
	}
}
