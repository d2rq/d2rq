package org.d2rq.r2rml;

import java.util.HashSet;
import java.util.Set;

import org.d2rq.vocab.RR;

import com.hp.hpl.jena.rdf.model.Resource;


public class PredicateObjectMap extends MappingComponent {
	private final Set<Resource> predicateMaps = new HashSet<Resource>();
	private final Set<ConstantShortcut> predicates = new HashSet<ConstantShortcut>();
	private final Set<Resource> objectMaps = new HashSet<Resource>();
	private final Set<ConstantShortcut> objects = new HashSet<ConstantShortcut>();
	private final Set<Resource> graphMaps = new HashSet<Resource>();
	private final Set<ConstantShortcut> graphs = new HashSet<ConstantShortcut>();
	
	public ComponentType getType() {
		return ComponentType.PREDICATE_OBJECT_MAP;
	}

	public Set<Resource> getPredicateMaps() {
		return predicateMaps;
	}
	
	public Set<ConstantShortcut> getPredicates() {
		return predicates;
	}
	
	/**
	 * These can reference {@link TermMap}s or {@link ReferencingObjectMap}s.
	 */
	public Set<Resource> getObjectMaps() {
		return objectMaps;
	}
	
	public Set<ConstantShortcut> getObjects() {
		return objects;
	}
	
	public Set<Resource> getGraphMaps() {
		return graphMaps;
	}
	
	public Set<ConstantShortcut> getGraphs() {
		return graphs;
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitComponent(this);
		for (Resource predicateMap: predicateMaps) {
			visitor.visitComponentProperty(RR.predicateMap, predicateMap, ComponentType.PREDICATE_MAP);
		}
		for (ConstantShortcut predicate: predicates) {
			visitor.visitTermProperty(RR.predicate, predicate);
		}
		for (Resource objectMap: objectMaps) {
			visitor.visitComponentProperty(RR.objectMap, objectMap, 
					ComponentType.OBJECT_MAP, ComponentType.REF_OBJECT_MAP);
		}
		for (ConstantShortcut object: objects) {
			visitor.visitTermProperty(RR.object, object);
		}
		for (Resource graphMap: graphMaps) {
			visitor.visitComponentProperty(RR.graphMap, graphMap, ComponentType.GRAPH_MAP);
		}
		for (ConstantShortcut graph: graphs) {
			visitor.visitTermProperty(RR.graph, graph);
		}
	}
}
