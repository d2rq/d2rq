package de.fuberlin.wiwiss.d2rq.engine;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.Binding0;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterNullIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterSingleton;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIteratorBase;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.util.IndentedWriter;
import com.hp.hpl.jena.sparql.util.Utils;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

public class RelationToBindingsIterator extends QueryIteratorBase {
	
	public static QueryIterator create(Relation relation, 
			Collection bindingMakers, ExecutionContext context) {
		if (relation.condition().isFalse()) {
			return new QueryIterNullIterator(context);
		}
		if (relation.isTrivial()) {
			return new QueryIterSingleton(new Binding0(), context);
		}
		return new RelationToBindingsIterator(relation, bindingMakers);
	}
	
	private final Collection bindingMakers;
	private final QueryExecutionIterator wrapped;
	private final LinkedList queue = new LinkedList();
	
	private RelationToBindingsIterator(Relation relation, Collection bindingMakers) {
		this.bindingMakers = bindingMakers;
		SelectStatementBuilder sql = new SelectStatementBuilder(relation);
		wrapped = new QueryExecutionIterator(
				sql.getSQLStatement(), sql.getColumnSpecs(), relation.database());
	}
	
	public boolean hasNextBinding() {
		while (queue.isEmpty() && wrapped.hasNext()) {
			enqueueBindings(wrapped.nextRow());
		}
		return !queue.isEmpty();
	}
	
	public Binding moveToNextBinding() {
		return (Binding) queue.removeFirst();
	}

	private void enqueueBindings(ResultRow row) {
		Iterator it = bindingMakers.iterator();
		while (it.hasNext()) {
			BindingMaker bindingMaker = (BindingMaker) it.next();
			Binding binding = bindingMaker.makeBinding(row);
			if (binding != null) {
				queue.add(binding);
			}
		}
	}
	
	public void closeIterator() {
		wrapped.close();
	}
	
	public void output(IndentedWriter out, SerializationContext cxt) {
		out.print(Utils.className(this));
	}
}
