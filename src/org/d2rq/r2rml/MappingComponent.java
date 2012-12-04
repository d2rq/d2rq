package org.d2rq.r2rml;

import org.d2rq.vocab.RR;
import org.d2rq.vocab.RRExtra;

import com.hp.hpl.jena.rdf.model.Resource;


public abstract class MappingComponent {

	public enum ComponentType {
		MAPPING(RRExtra.Mapping, "mapping"),
		TRIPLES_MAP(RR.TriplesMap, "triples map"),
		LOGICAL_TABLE(RR.LogicalTable, "logical table"),
		BASE_TABLE_OR_VIEW(RR.BaseTableOrView, "base table or view"),
		R2RML_VIEW(RR.R2RMLView, "R2RML view"),
		TERM_MAP(RRExtra.TermMap, "term map"),
		CONSTANT_VALUED_TERM_MAP(RRExtra.ConstantValuedTermMap, "constant-valued term map"),
		COLUMN_VALUED_TERM_MAP(RRExtra.ColumnValuedTermMap, "column-valued term map"),
		TEMPLATE_VALUED_TERM_MAP(RRExtra.TemplateValuedTermMap, "template-valued term map"),
		SUBJECT_MAP(RR.SubjectMap, "subject map"),
		PREDICATE_MAP(RR.PredicateMap, "predicate map"),
		OBJECT_MAP(RR.ObjectMap, "object map"),
		GRAPH_MAP(RR.GraphMap, "graph map"),
		PREDICATE_OBJECT_MAP(RR.PredicateObjectMap, "predicate-object map"),
		REF_OBJECT_MAP(RR.RefObjectMap, "referencing object map"),
		JOIN(RR.Join, "join");

		private final String label;
		private final Resource resource;
		ComponentType(Resource resource, String label) {
			this.resource = resource;
			this.label = label;
		}
		public Resource asResource() {
			return resource;
		}
		public String getLabel() {
			return label;
		}
	}

	public abstract ComponentType getType();
	
	public abstract void accept(MappingVisitor visitor);
	
	public boolean isValid() {
		MappingValidator validator = new MappingValidator(null);
		accept(validator);
		return !validator.getReport().hasError();
	}
}