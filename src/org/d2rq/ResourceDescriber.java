package org.d2rq;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.d2rq.db.op.LimitOp;
import org.d2rq.find.FindQuery;
import org.d2rq.find.TripleQueryIter;
import org.openjena.atlas.lib.AlarmClock;
import org.openjena.atlas.lib.Callback;
import org.openjena.atlas.lib.Pingback;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterConcat;


public class ResourceDescriber {
	private static final Log log = LogFactory.getLog(ResourceDescriber.class);

	private final CompiledMapping mapping;
	private final Node node;
	private final boolean onlyOutgoing;
	private final int limit;
	private final long timeout;
	private final Graph result = new GraphMem();
	private boolean executed = false;
	
	public ResourceDescriber(CompiledMapping mapping, Node resource) {
		this(mapping, resource, false, LimitOp.NO_LIMIT, -1);
	}
	
	public ResourceDescriber(CompiledMapping mapping, Node resource, boolean onlyOutgoing, int limit, long timeout) {
		this.mapping = mapping;
		this.node = resource;
		this.onlyOutgoing = onlyOutgoing;
		this.limit = limit;
		this.timeout = timeout;
	}
	
	public Graph description() {
		if (executed) return result;
		executed = true;

		log.info("Describing resource: " + node);

		ExecutionContext context = new ExecutionContext(
				mapping.getContext(), null, null, null);
		final QueryIterConcat qIter = new QueryIterConcat(context);
		Pingback<?> pingback = null;
		if (timeout > 0) {
			pingback = AlarmClock.get().add(new Callback<Object>() {
				public void proc(Object ignore) {
					qIter.cancel();
				}
			}, timeout);
		}
		
		FindQuery outgoing = new FindQuery(
				Triple.create(node, Node.ANY, Node.ANY), 
				mapping.getTripleRelations(), limit, context);
		qIter.add(outgoing.iterator());
		
		if (!onlyOutgoing) {
			FindQuery incoming = new FindQuery(
					Triple.create(Node.ANY, Node.ANY, node), 
					mapping.getTripleRelations(), limit, context);
			qIter.add(incoming.iterator());
	
			FindQuery triples = new FindQuery(
					Triple.create(Node.ANY, node, Node.ANY), 
					mapping.getTripleRelations(), limit, context);
			qIter.add(triples.iterator());
		}
		Iterator<Triple> it = TripleQueryIter.create(qIter);
		while (it.hasNext()) {
			result.add(it.next());
		}
		
		if (pingback != null) {
			AlarmClock.get().cancel(pingback);
		}
		
		return result;
	}
}
