package de.fuberlin.wiwiss.d2rq;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.BindingQueryPlan;
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.Query;
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;
import com.hp.hpl.jena.graph.query.TreeQueryPlan;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.Plan;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Map1;
import com.hp.hpl.jena.util.iterator.Map1Iterator;

import de.fuberlin.wiwiss.d2rq.engine.D2RQDatasetGraph;
import de.fuberlin.wiwiss.d2rq.engine.QueryEngineD2RQ;

/**
 * A D2RQQueryHandler handles queries on behalf of a {@link GraphD2RQ}.
 * 
 * @author Richard Cyganiak
 * @version $Id: D2RQQueryHandler.java,v 1.5 2008/08/12 13:40:19 cyganiak Exp $
 */
public class D2RQQueryHandler extends SimpleQueryHandler {
	private D2RQDatasetGraph dataset;
	private Node[] variables;
	private Map indexes;

	public D2RQQueryHandler(GraphD2RQ graph, D2RQDatasetGraph dataset) {
		super(graph);
		this.dataset = dataset;
	}     

	public TreeQueryPlan prepareTree(Graph pattern) {
		throw new RuntimeException("prepareTree - Andy says Chris says this will not be called");
	}

	public BindingQueryPlan prepareBindings(Query q, Node[] variables) {   
		this.variables = variables;
		this.indexes = new HashMap();
		for (int i = 0; i < variables.length; i++) {
			indexes.put(variables[i], new Integer(i));
		}
		BasicPattern pattern = new BasicPattern();
		Iterator it = q.getPattern().iterator();
		while (it.hasNext()) {
			Triple t = (Triple) it.next();
			pattern.add(t);
		}
		Plan plan = QueryEngineD2RQ.getFactory().create(new OpBGP(pattern), dataset, null, null);
		final ExtendedIterator queryIterator = new Map1Iterator(new BindingToDomain(), plan.iterator());
		return new BindingQueryPlan() {
			public ExtendedIterator executeBindings() {
				return queryIterator;
			}
		};
	}

	private class BindingToDomain implements Map1 {
		public Object map1(Object o) {
			Binding binding = (Binding) o;
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
