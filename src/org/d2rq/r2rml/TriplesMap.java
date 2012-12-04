package org.d2rq.r2rml;

import java.util.HashSet;
import java.util.Set;

import org.d2rq.vocab.RR;

import com.hp.hpl.jena.rdf.model.Resource;


public class TriplesMap extends MappingComponent {
	private Resource logicalTable = null;
	private Resource subjectMap = null;
	private ConstantShortcut subject = null;
	private final Set<Resource> predicateObjectMaps = new HashSet<Resource>();
	
	public ComponentType getType() {
		return ComponentType.TRIPLES_MAP;
	}
	
	public void setLogicalTable(Resource resource) {
		this.logicalTable = resource;
	}
	
	public Resource getLogicalTable() {
		return logicalTable;
	}

	public void setSubjectMap(Resource resource) {
		subjectMap = resource;
	}
	
	public Resource getSubjectMap() {
		return subjectMap;
	}
	
	public void setSubject(ConstantShortcut subject) {
		this.subject = subject;
	}
	
	public ConstantShortcut getSubject() {
		return subject;
	}
	
	/**
	 * May be modified to add or remove predicate-object maps.
	 */
	public Set<Resource> getPredicateObjectMaps() {
		return predicateObjectMaps;
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitComponent(this);
		visitor.visitComponentProperty(RR.logicalTable, logicalTable, ComponentType.LOGICAL_TABLE);
		visitor.visitComponentProperty(RR.subjectMap, subjectMap, ComponentType.SUBJECT_MAP);
		visitor.visitTermProperty(RR.subject, subject);
		for (Resource poMap: predicateObjectMaps) {
			visitor.visitComponentProperty(RR.predicateObjectMap, poMap, ComponentType.PREDICATE_OBJECT_MAP);
		}
	}
}
