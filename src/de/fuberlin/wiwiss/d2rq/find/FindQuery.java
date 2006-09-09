package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;

import de.fuberlin.wiwiss.d2rq.algebra.JoinOptimizer;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleSelection;
import de.fuberlin.wiwiss.d2rq.algebra.UnionOverSameBase;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;


/**
 * A find query on a list of property bridges. Results are delivered
 * as an iterator. Will combine queries on multiple bridges into one
 * SQL statement where possible.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindQuery.java,v 1.6 2006/09/09 20:51:49 cyganiak Exp $
 */
public class FindQuery {
	private Triple triplePattern;
	private List propertyBridges;
	private Collection compatibleRelations;
	
	public FindQuery(Triple triplePattern, List propertyBridges) {
		this.triplePattern = triplePattern;
		this.propertyBridges = propertyBridges;
	}

	public ExtendedIterator iterator() {
		this.compatibleRelations = new ArrayList();
		findPropertyBridges();
		ExtendedIterator result = new NullIterator();
		Iterator it = this.compatibleRelations.iterator();
		while (it.hasNext()) {
			List relations = (List) it.next();
			result = result.andThen(resultIterator(new UnionOverSameBase(relations)));
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
		Iterator it = this.compatibleRelations.iterator();
		while (it.hasNext()) {
			List queries = (List) it.next();
			if (UnionOverSameBase.isSameBase((RDFRelation) queries.get(0), newQuery)) {
				queries.add(newQuery);
				return;
			}
		}
		List newList = new ArrayList();
		newList.add(newQuery);
		this.compatibleRelations.add(newList);
	}

	private ClosableIterator resultIterator(RDFRelation relation) {
		SelectStatementBuilder sql = new SelectStatementBuilder(relation.getDatabase());
		sql.addAliasMap(relation.getAliases());
		sql.addJoins(relation.getJoins());
		sql.addColumnValues(relation.getColumnValues());
		sql.addCondition(relation.condition());
		sql.addSelectColumns(relation.getSelectColumns());
		sql.setEliminateDuplicates(relation.mightContainDuplicates());
		ClosableIterator sqlResults = sql.isTrivial()
				? (ClosableIterator) new SingletonIterator(new String[]{})
				: new QueryExecutionIterator(sql.getSQLStatement(), sql.getDatabase());
		return new ApplyTripleMakersIterator(
				sqlResults, relation.tripleMaker(sql.getColumnNameNumberMap()));
	}
}
