package org.d2rq.r2rml;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.d2rq.pp.PrettyTurtleWriter;
import org.d2rq.r2rml.MappingComponent.ComponentType;
import org.d2rq.r2rml.TermMap.TermType;
import org.d2rq.vocab.RR;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;

public class R2RMLWriter extends MappingVisitor.TreeWalkerImplementation {
	private final static Set<Property> COMPACT_PROPERTIES = new HashSet<Property>(
			Arrays.asList(new Property[]{
					RR.logicalTable, RR.joinCondition, 
					RR.subjectMap, RR.predicateMap, RR.objectMap, RR.graphMap
			}));
	
	private final Mapping mapping;
	private final PrefixMapping prefixes = new PrefixMappingImpl();
	private PrettyTurtleWriter out;

	public R2RMLWriter(Mapping mapping) {
		super(mapping);
		this.mapping = mapping;
		prefixes.setNsPrefixes(mapping.getPrefixes());
		if (!prefixes.getNsPrefixMap().containsValue(RR.NS)) {
			prefixes.setNsPrefix("rr", RR.NS);
		}
	}
	
	public void write(OutputStream outStream) {
		try {
			write(new OutputStreamWriter(outStream, "utf-8"));
		} catch (UnsupportedEncodingException ex) {
			// can't happen, UTF-8 always supported
		}
	}
	
	public void write(Writer outWriter) {
		out = new PrettyTurtleWriter(prefixes, outWriter);
		mapping.accept(this);
		out.flush();
	}

	@Override
	public void visitComponent(Mapping mapping) {
		for (Resource r: mapping.logicalTables().resources()) {
			if (r.isAnon()) continue;
			visitComponentProperty(null, r, ComponentType.LOGICAL_TABLE);
		}
		for (Resource r: mapping.termMaps().resources()) {
			if (r.isAnon()) continue;
			visitComponentProperty(null, r, ComponentType.TERM_MAP);
		}
		for (Resource r: mapping.predicateObjectMaps().resources()) {
			if (r.isAnon()) continue;
			visitComponentProperty(null, r, ComponentType.PREDICATE_OBJECT_MAP);
		}
		for (Resource r: mapping.referencingObjectMaps().resources()) {
			if (r.isAnon()) continue;
			visitComponentProperty(null, r, ComponentType.REF_OBJECT_MAP);
		}
		for (Resource r: mapping.joins().resources()) {
			if (r.isAnon()) continue;
			visitComponentProperty(null, r, ComponentType.JOIN);
		}
		super.visitComponent(mapping);
	}
	
	@Override
	public void visitComponentProperty(Property property, Resource resource,
			ComponentType... types) {
		if (resource == null) return;
		if (property == null) {
			out.printResourceStart(resource);
			super.visitComponentProperty(property, resource, types);
			out.printResourceEnd();
		} else if (resource.isAnon()) {
			out.printPropertyStart(property, COMPACT_PROPERTIES.contains(property));
			super.visitComponentProperty(property, resource, types);
			out.printPropertyEnd();
		} else {
			out.printProperty(property, resource);
		}
	}

	@Override
	public void visitTermProperty(Property property, MappingTerm term) {
		if (term == null) return;
		if (term instanceof ConstantShortcut) {
			out.printProperty(property, ((ConstantShortcut) term).asRDFNode()); 
		} else if (term instanceof ConstantIRI) {
			out.printProperty(property, ((ConstantIRI) term).asResource()); 
		} else if (term instanceof SQLQuery) {
			out.printLongStringProperty(property, term.toString()); 
		} else {
			out.printProperty(property, term.toString());
		}
	}

	@Override
	public void visitSimpleProperty(Property property, Object value) {
		if (value instanceof TermType) {
			out.printProperty(property, ((TermType) value).asResource());
		} else if (value instanceof RDFNode) {
			out.printProperty(property, (RDFNode) value);
		}
	}
}
