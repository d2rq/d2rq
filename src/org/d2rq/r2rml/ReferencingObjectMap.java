package org.d2rq.r2rml;

import java.util.HashSet;
import java.util.Set;

import org.d2rq.vocab.RR;

import com.hp.hpl.jena.rdf.model.Resource;


public class ReferencingObjectMap extends MappingComponent {
	private Resource parentTriplesMap = null;
	private Set<Resource> joinConditions = new HashSet<Resource>();
	
	public ComponentType getType() {
		return ComponentType.REF_OBJECT_MAP;
	}
	
	public void setParentTriplesMap(Resource parentTriplesMap) {
		this.parentTriplesMap = parentTriplesMap;
	}
	
	public Resource getParentTriplesMap() {
		return parentTriplesMap;
	}
	
	public Set<Resource> getJoinConditions() {
		return joinConditions;
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitComponent(this);
		visitor.visitComponentProperty(RR.parentTriplesMap, parentTriplesMap, ComponentType.TRIPLES_MAP);
		for (Resource join: joinConditions) {
			visitor.visitComponentProperty(RR.joinCondition, join, ComponentType.JOIN);
		}
	}
}
