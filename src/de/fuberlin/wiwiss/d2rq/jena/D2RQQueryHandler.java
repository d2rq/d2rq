package de.fuberlin.wiwiss.d2rq.jena;

import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.BindingQueryPlan;
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.Query;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;
import com.hp.hpl.jena.graph.query.TreeQueryPlan;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.Plan;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Map1;
import com.hp.hpl.jena.util.iterator.Map1Iterator;

import de.fuberlin.wiwiss.d2rq.engine.QueryEngineD2RQ;

/**
 * An implementation of Jena's {@link QueryHandler} interface
 * that answers BGP queries on behalf of a {@link GraphD2RQ}.
 * This is here for Jena compatibility, and is not involved
 * in answering SPARQL queries through ARQ. It uses the
 * {@link QueryEngineD2RQ} to answer BGP queries and wraps 
 * it into the expected interface.
 * 
 * @author Richard Cyganiak
 */
public class D2RQQueryHandler extends SimpleQueryHandler {
	private final DatasetGraph dataset;
	private Node[] variables;
	private Map<Node,Integer> indexes;

	public D2RQQueryHandler(GraphD2RQ graph) {
		super(graph);
		dataset = DatasetGraphFactory.createOneGraph(graph);
	}     

	public TreeQueryPlan prepareTree(Graph pattern) {
		throw new RuntimeException("prepareTree - Andy says Chris says this will not be called");
	}

	public BindingQueryPlan prepareBindings(Query q, Node[] variables) {   
		this.variables = variables;
		this.indexes = new HashMap<Node,Integer>();
		for (int i = 0; i < variables.length; i++) {
			indexes.put(variables[i], new Integer(i));
		}
		BasicPattern pattern = new BasicPattern();
		for (Triple t: q.getPattern()) {
			pattern.add(t);
		}
		Plan plan = QueryEngineD2RQ.getFactory().create(new OpBGP(pattern), dataset, null, null);
		final ExtendedIterator<Domain> queryIterator = new Map1Iterator<Binding,Domain>(new BindingToDomain(), plan.iterator());
		return new BindingQueryPlan() {
			public ExtendedIterator<Domain> executeBindings() {
				return queryIterator;
			}
		};
	}

	private class BindingToDomain implements Map1<Binding,Domain> {
		public Domain map1(Binding binding) {
			Domain d = new Domain(variables.length);
			for (int i = 0; i < variables.length; i++) {
				Var v = Var.alloc(variables[i]);
				Node value = binding.get(v);
				int index = ((Integer) indexes.get(v)).intValue();
				d.setElement(index, value);
			}
			return d;
		}
	}
}
