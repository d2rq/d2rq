package de.fuberlin.wiwiss.d2rq.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIter;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterNullIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterSingleton;

import de.fuberlin.wiwiss.d2rq.algebra.NodeRelation;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/**
 * A {@link QueryIterator} over the bindings produced by a
 * {@link Relation}. Works by running the underlying SQL
 * query using a {@link QueryExecutionIterator}.
 *  
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class QueryIterTableSQL extends QueryIter {

	/**
	 * Creates an instance, or a simpler QueryIterator
	 * if optimization is possible (e.g., the relation is empty).
	 * @return A query iterator over the contents of the relation
	 */
	public static QueryIterator create(Relation relation, 
			Collection<BindingMaker> bindingMakers, ExecutionContext execCxt) {
		if (relation.condition().isFalse() || bindingMakers.isEmpty()) {
			return new QueryIterNullIterator(execCxt);
		}
		return new QueryIterTableSQL(relation, bindingMakers, execCxt);
	}
	
	/**
	 * Creates an instance, or a simpler QueryIterator
	 * if optimization is possible (e.g., the relation is empty).
	 * @return A query iterator over the contents of the node relation
	 */
	public static QueryIterator create(NodeRelation table, ExecutionContext execCxt) {
		if (table.baseRelation().condition().isFalse()) {
			return new QueryIterNullIterator(execCxt);
		}
		if (table.baseRelation().isTrivial()) {
			return QueryIterSingleton.create(
					BindingMaker.createFor(table).makeBinding(ResultRow.NO_ATTRIBUTES), 
					execCxt);
		}
		return new QueryIterTableSQL(table.baseRelation(), 
				Collections.singleton(BindingMaker.createFor(table)), execCxt);
	}
	
	private final QueryExecutionIterator wrapped;
	private final Collection<BindingMaker> bindingMakers;
	private final LinkedList<Binding> queue = new LinkedList<Binding>();

	public QueryIterTableSQL(Relation relation, 
			Collection<BindingMaker> bindingMakers, ExecutionContext execCxt) {
		super(execCxt);
		this.bindingMakers = bindingMakers;
		SelectStatementBuilder builder = new SelectStatementBuilder(relation);
		wrapped = new QueryExecutionIterator(
				builder.getSQLStatement(), builder.getColumnSpecs(), relation.database());
	}
	
	@Override
	protected boolean hasNextBinding() {
		while (queue.isEmpty() && wrapped.hasNext()) {
			enqueueBindings(wrapped.next());
		}
		return !queue.isEmpty();
	}

	@Override
	protected Binding moveToNextBinding() {
		return queue.removeFirst();
	}

	@Override
	protected void closeIterator() {
		wrapped.close();
	}

	@Override
	protected void requestCancel() {
		wrapped.close();
	}

	/**
	 * Create bindings from one database result row and put
	 * them onto the queue
	 */
	private void enqueueBindings(ResultRow row) {
		for (BindingMaker bindingMaker: bindingMakers) {
			Binding binding = bindingMaker.makeBinding(row);
			if (binding == null) continue; 
			queue.add(binding);
		}
	}
}