package de.fuberlin.wiwiss.d2rq.engine;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.algebra.AlgebraGenerator;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.Plan;
import com.hp.hpl.jena.sparql.engine.QueryEngineBase;
import com.hp.hpl.jena.sparql.engine.QueryEngineFactory;
import com.hp.hpl.jena.sparql.engine.QueryEngineRegistry;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingRoot;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRoot;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIteratorCheck;
import com.hp.hpl.jena.sparql.engine.main.QC;
import com.hp.hpl.jena.sparql.util.Context;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.optimizer.D2RQTreeOptimizer;

/**
 * TODO: @@@ QueryEngineD2RQ and the whole package is work in progress
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: QueryEngineD2RQ.java,v 1.4 2009/02/06 13:59:02 fatorange Exp $
 */
public class QueryEngineD2RQ extends QueryEngineBase {
	private GraphD2RQ graph;
	
	public QueryEngineD2RQ(GraphD2RQ graph, Query query) {
		this(graph, query, null);
	}

	public QueryEngineD2RQ(GraphD2RQ graph, Query query, Context context) {
		super(query, new D2RQDatasetGraph(graph), new AlgebraGenerator(context), BindingRoot.create(), context);
		this.graph = graph;
	}

	public QueryEngineD2RQ(GraphD2RQ graph, Op op, Context context) {
		super(op, new D2RQDatasetGraph(graph), BindingRoot.create(), context);
		this.graph = graph;
	}

	protected Op modifyOp(Op op) {
	    Op optimizedOp;
	     
	    optimizedOp = D2RQTreeOptimizer.optimize(op, graph);
		return optimizedOp;
	}
	
	public QueryIterator eval(Op op, DatasetGraph dataset, Binding input, Context context) {
		ExecutionContext execCxt = new ExecutionContext(context, dataset.getDefaultGraph(), dataset) ;
        QueryIterator qIter1 = QueryIterRoot.create(input, execCxt) ;
		QueryIterator qIter = QC.compile(op, qIter1, execCxt);
		return QueryIteratorCheck.check(qIter, execCxt);
	}

	// Factory stuff
	private static QueryEngineFactory factory = new QueryEngineFactoryD2RQ();
	public static QueryEngineFactory getFactory() {
		return factory;
	} 
	public static void register() {
		QueryEngineRegistry.addFactory(factory);
	}
	public static void unregister() {
		QueryEngineRegistry.removeFactory(factory);
	}
	private static class QueryEngineFactoryD2RQ implements QueryEngineFactory {
		public boolean accept(Query query, DatasetGraph dataset, Context context) {
			return dataset instanceof D2RQDatasetGraph 
			|| dataset.getDefaultGraph() instanceof GraphD2RQ;
		}
		public Plan create(Query query, DatasetGraph dataset, 
				Binding inputBinding, Context context) {
			return new QueryEngineD2RQ((GraphD2RQ) dataset.getDefaultGraph(), 
					query, context).getPlan();
		}
		public boolean accept(Op op, DatasetGraph dataset, Context context) {
			return dataset instanceof D2RQDatasetGraph
			|| dataset.getDefaultGraph() instanceof GraphD2RQ;
		}
		public Plan create(Op op, DatasetGraph dataset, 
				Binding inputBinding, Context context) {
			return new QueryEngineD2RQ((GraphD2RQ) dataset.getDefaultGraph(), 
					op, context).getPlan();
		}
	}
}