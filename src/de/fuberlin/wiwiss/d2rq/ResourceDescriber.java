package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.mem.GraphMem;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.find.FindQuery;
import de.fuberlin.wiwiss.d2rq.map.Mapping;

public class ResourceDescriber {
	private final Mapping mapping;
	private final Node node;
	private final boolean onlyOutgoing;
	private final int limit;
	private final Graph result = new GraphMem();
	private boolean executed = false;
	
	public ResourceDescriber(Mapping mapping, Node resource) {
		this(mapping, resource, false, Relation.NO_LIMIT);
	}
	
	public ResourceDescriber(Mapping mapping, Node resource, boolean onlyOutgoing, int limit) {
		this.mapping = mapping;
		this.node = resource;
		this.onlyOutgoing = onlyOutgoing;
		this.limit = limit;
	}
	
	public Graph description() {
		if (executed) return result;
		executed = true;

		FindQuery outgoing = new FindQuery(
				Triple.create(node, Node.ANY, Node.ANY), 
				mapping.compiledPropertyBridges(), limit);
		result.getBulkUpdateHandler().add(outgoing.iterator());
		
		if (!onlyOutgoing) {
			FindQuery incoming = new FindQuery(
					Triple.create(Node.ANY, Node.ANY, node), 
					mapping.compiledPropertyBridges(), limit);
			result.getBulkUpdateHandler().add(incoming.iterator());
	
			FindQuery triples = new FindQuery(
					Triple.create(Node.ANY, node, Node.ANY), 
					mapping.compiledPropertyBridges(), limit);
			result.getBulkUpdateHandler().add(triples.iterator());
		}
		return result;
	}
}
