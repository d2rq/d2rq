package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;

import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;


/**
 * A find query on a list of property bridges. Results are delivered
 * as an iterator. Will combine queries on multiple bridges into one
 * SQL statement where possible.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindQuery.java,v 1.4 2006/09/03 17:59:08 cyganiak Exp $
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
			result = result.andThen(resultIterator(queryList));
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

	private ClosableIterator resultIterator(List queryList) {
		Iterator it = queryList.iterator();
		PropertyBridgeQuery first = (PropertyBridgeQuery) it.next();
		SelectStatementBuilder sql = new SelectStatementBuilder(first.getDatabase());
		sql.addAliasMap(first.getAliases());
		sql.addJoins(first.getJoins());
		sql.addColumnValues(first.getColumnValues());
		sql.addCondition(first.condition());
		sql.addSelectColumns(first.getSelectColumns());
		sql.addColumnRenames(first.getReplacedColumns());
		sql.setEliminateDuplicates(first.mightContainDuplicates());
		while (it.hasNext()) {
			PropertyBridgeQuery query = (PropertyBridgeQuery) it.next();
			sql.addSelectColumns(query.getSelectColumns());
			sql.addColumnRenames(query.getReplacedColumns());
		}
		if (sql.isTrivial()) {
			return new ApplyTripleMakersIterator(
					new SingletonIterator(new String[]{}), queryList, Collections.EMPTY_MAP);
		}
		return new ApplyTripleMakersIterator(
				new QueryExecutionIterator(sql.getSQLStatement(), sql.getDatabase()),
				queryList, sql.getColumnNameNumberMap());				
	}
}
