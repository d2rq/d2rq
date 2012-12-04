package org.d2rq.r2rml;

import org.d2rq.r2rml.MappingComponent.ComponentType;
import org.d2rq.r2rml.TermMap.Position;
import org.d2rq.vocab.RR;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;


public interface MappingVisitor {

	void visitComponentProperty(Property property, Resource resource, ComponentType... types);
	void visitTermProperty(Property property, MappingTerm term);
	void visitSimpleProperty(Property property, Object value);
	
	void visitComponent(Mapping mapping);
	void visitComponent(TriplesMap triplesMap);
	void visitComponent(LogicalTable logicalTable);
	void visitComponent(LogicalTable.BaseTableOrView baseTableOrView);	
	void visitComponent(LogicalTable.R2RMLView r2rmlView);
	void visitComponent(TermMap termMap, Position position);
	void visitComponent(TermMap.ConstantValuedTermMap termMap, Position position);
	void visitComponent(TermMap.ColumnOrTemplateValuedTermMap termMap, Position position);
	void visitComponent(TermMap.ColumnValuedTermMap termMap, Position position);
	void visitComponent(TermMap.TemplateValuedTermMap termMap, Position position);
	void visitComponent(PredicateObjectMap predicateObjectMap);
	void visitComponent(ReferencingObjectMap referencingObjectMap);
	void visitComponent(Join join);
	
	void visitTerm(TableOrViewName tableName);
	void visitTerm(SQLQuery sqlQuery);
	void visitTerm(ConstantShortcut shortcut, Position position);
	void visitTerm(ColumnNameR2RML columnName);
	void visitTerm(StringTemplate stringTemplate);
	void visitTerm(ConstantIRI iri);
	void visitTerm(LanguageTag languageTag);
	
	public class DoNothingImplementation implements MappingVisitor {
		public void visitComponentProperty(Property property, Resource resource, 
				ComponentType... types) {}
		public void visitTermProperty(Property property, MappingTerm term) {}
		public void visitSimpleProperty(Property property, Object value) {}
		public void visitComponent(Mapping mapping) {}
		public void visitComponent(TriplesMap triplesMap) {}
		public void visitComponent(LogicalTable logicalTable) {}
		public void visitComponent(LogicalTable.BaseTableOrView logicalTable) {}
		public void visitComponent(LogicalTable.R2RMLView logicalTable) {}
		public void visitComponent(TermMap termMap, Position position) {}
		public void visitComponent(TermMap.ConstantValuedTermMap termMap, Position position) {}
		public void visitComponent(TermMap.ColumnOrTemplateValuedTermMap termMap, Position position) {}
		public void visitComponent(TermMap.ColumnValuedTermMap termMap, Position position) {}
		public void visitComponent(TermMap.TemplateValuedTermMap termMap, Position position) {}
		public void visitComponent(PredicateObjectMap predicateObjectMap) {}
		public void visitComponent(ReferencingObjectMap referencingObjectMap) {}
		public void visitComponent(Join join) {}
		public void visitTerm(TableOrViewName tableName) {}
		public void visitTerm(SQLQuery sqlQuery) {}
		public void visitTerm(ConstantShortcut shortcut, Position position) {}
		public void visitTerm(ColumnNameR2RML columnName) {}
		public void visitTerm(StringTemplate stringTemplate) {}
		public void visitTerm(ConstantIRI iri) {}
		public void visitTerm(LanguageTag languageTag) {}
	}
	
	public class TreeWalkerImplementation extends DoNothingImplementation {
		private final Mapping mapping;
		public TreeWalkerImplementation(Mapping mapping) {
			this.mapping = mapping;
		}
		public void visitComponentProperty(Property property, Resource resource, 
				ComponentType... types) { 
			// Do not recurse into parentTriplesMap to avoid cycles
			if (resource == null || RR.parentTriplesMap.equals(property)) return;
			for (ComponentType type: types) {
				MappingComponent component = mapping.getMappingComponent(resource, type);
				if (component == null) continue;
				switch (type) {
				case SUBJECT_MAP: ((TermMap) component).acceptAs(this, Position.SUBJECT_MAP); continue;
				case PREDICATE_MAP: ((TermMap) component).acceptAs(this, Position.PREDICATE_MAP); continue;
				case OBJECT_MAP: ((TermMap) component).acceptAs(this, Position.OBJECT_MAP); continue;
				case GRAPH_MAP: ((TermMap) component).acceptAs(this, Position.GRAPH_MAP); continue;
				default: component.accept(this);
				}
			}
		}
		public void visitTermProperty(Property property, MappingTerm term) {
			if (term == null) return;
			if (RR.subject.equals(property)) {
				((ConstantShortcut) term).acceptAs(this, Position.SUBJECT_MAP);
			} else if (RR.predicate.equals(property)) {
				((ConstantShortcut) term).acceptAs(this, Position.PREDICATE_MAP);
			} else if (RR.object.equals(property)) {
				((ConstantShortcut) term).acceptAs(this, Position.OBJECT_MAP);
			} else if (RR.graph.equals(property)) {
				((ConstantShortcut) term).acceptAs(this, Position.GRAPH_MAP);
			} else {
				term.accept(this);
			}
		}
	}
}
