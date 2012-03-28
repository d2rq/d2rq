package de.fuberlin.wiwiss.d2rq.optimizer.iterators;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.openjena.atlas.io.IndentedWriter;

import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingHashMap;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIter;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterNullIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.util.Utils;

import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.engine.BindingMaker;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/**
 * Iterator that calculates the result-bindings of an OpD2RQ.
 *
 * @author Herwig Leimer
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class RelationToBindingsD2RQIterator extends QueryIterRepeatApply {

	/**
	 * Creates a new RelationToBindingsD2RQIterator
	 * @param relation - contains information for getting the data used for the bindings from the database
	 * @param bindingMakers - contains information for creating the bindings from the the databasedata
	 * @param input - Input-Iterator
	 * @param context - execution-context
	 * @return QueryIterator - a QueryIterator
	 */
	public static QueryIterator create(Relation relation, Collection<BindingMaker> bindingMakers, 
			QueryIterator input, ExecutionContext context) {
		if (relation.condition().isFalse()) {
			input.close();
			return new QueryIterNullIterator(context);
		}
		if (relation.isTrivial()) {
			return input;
		}
		return new RelationToBindingsD2RQIterator(relation, bindingMakers, input, context);
	}

	private final Collection<BindingMaker> bindingMakers;
	private final Relation relation;

	/**
	 * Constructor
	 * @param relation - contains information for getting the data used for the bindings from the database
	 * @param bindingMakers - contains information for creating the bindings from the the databasedata
	 * @param input - Input-Iterator
	 * @param context - execution-context
	 */
	protected RelationToBindingsD2RQIterator(Relation relation, Collection<BindingMaker> bindingMakers, 
			QueryIterator input, ExecutionContext context) {
		super(input, context) ;
		this.bindingMakers = bindingMakers;
		this.relation = relation;
	}

	/**
	 * Method for printing
	 */
	@Override
	public void output(IndentedWriter out, SerializationContext cxt) {
		out.print(Utils.className(this));
	}

	@Override
	protected QueryIterator nextStage(Binding binding) {	
		return new StagePattern(binding, relation, bindingMakers, getExecContext());
	}

	/**
	 * Class that fetches additional values for every parent-binding.
	 * Especially necessary for Joins and LeftJoins.
	 */
	static class StagePattern extends QueryIter {
		private final Binding parentBinding ;
		private final Collection<BindingMaker> bindingMakers;
		private final LinkedList<Binding> queue = new LinkedList<Binding>();
		private final Set<Var> vars = new HashSet<Var>();
		private final QueryExecutionIterator wrapped;

		/**
		 * Construtor
		 * @param parentBinding - Parentbinding - for example join/leftjoin - the parentbinding will be a resultbinding from the left-OpD2RQ
		 * @param relation - relation for getting the needed data from the database
		 * @param bindingMakers - bindingMakers for creating the bindings
		 * @param qCxt - execution-context
		 */
		public StagePattern(Binding parentBinding, Relation relation, 
				Collection<BindingMaker> bindingMakers, ExecutionContext qCxt) {
			super(qCxt);
			this.parentBinding = parentBinding ;
			this.bindingMakers = bindingMakers;

			MutableRelation mutableRelation = new MutableRelation(relation);
			// BindingMakers contain all information for making the bindings of the current relation
			for (BindingMaker bindingMaker: bindingMakers) {
				// collect all vars of all bindingmakers
				for (String name: bindingMaker.variableNames()) {
					vars.add(Var.alloc(name));
				}

				// TODO: The logic seems incomplete. If we have multiple BindingMakers,
				// just selecting on all of them isn't going to work. Also, the
				// BindingMaker should probably be modified to use the new NodeMaker
				// that is returned by the selectNode operation. At the moment this
				// seems to work anyways because ARQ only ever seems to invoke the
				// iterator with the root binding as input, so there are no variables
				// in the parent binding.

				// now check if every variable of the parent binding is part of
				// the current relation and can be put into the current relation
				for (Iterator<Var> bindingVars = parentBinding.vars(); bindingVars.hasNext();) {
					// get first binding-var
					Var var = bindingVars.next();

					// is the binding var also used in this relation ?
					if (!bindingMaker.variableNames().contains(var.getName())) continue;

					// put the value of the binding-var to the relation
					bindingMaker.nodeMaker(var.getName()).selectNode(
							parentBinding.get(var), mutableRelation);
				}
			}
			relation = mutableRelation.immutableSnapshot(); 
			if (relation.equals(Relation.EMPTY)) {
				wrapped = null;
			} else {
				SelectStatementBuilder sql = new SelectStatementBuilder(relation);
				wrapped = new QueryExecutionIterator(
						sql.getSQLStatement(), sql.getColumnSpecs(), relation.database());
			}
		}

		public boolean hasNextBinding() {
			while (queue.isEmpty() && wrapped != null && wrapped.hasNext()) {
				enqueueBindings(wrapped.next());
			}
			return !queue.isEmpty();
		}

		protected void closeIterator() {
			if (wrapped != null) wrapped.close();
		}

		public Binding moveToNextBinding() {
			BindingMap next = new BindingHashMap(parentBinding);
			Binding b = queue.removeFirst();
			for (Var var: vars) {
				if (next.contains(var) || b.get(var) == null) continue;
				next.add(var, b.get(var));
			}
			return next;
		}

		/**
		 * Makes the binding from a result-row of the database
		 * and puts it into a queue
		 * @param row
		 */
		protected void enqueueBindings(ResultRow row) {
			for (BindingMaker bindingMaker: bindingMakers) {
				Binding binding = bindingMaker.makeBinding(row);
				if (binding == null) continue; 
				queue.add(binding);
			}
		}

		@Override
		protected void requestCancel() { }
	}
}
