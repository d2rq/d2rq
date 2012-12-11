package org.d2rq.mapgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.values.TemplateValueMaker;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class OntologyTarget implements Target {
	private final static String CREATOR = "D2RQ Mapping Generator";

	private final Model model = ModelFactory.createDefaultModel();
	private final Map<Property,TableName> domains = 
			new HashMap<Property,TableName>();
	private final Map<Property,TableName> ranges = 
			new HashMap<Property,TableName>();
	private final Map<TableName,Resource> classes = 
			new HashMap<TableName,Resource>();
	
	private Resource generatedOntology = null;

	public OntologyTarget() {
		model.setNsPrefix("rdf", RDF.getURI());
		model.setNsPrefix("rdfs", RDFS.getURI());
		model.setNsPrefix("owl", OWL.getURI());
		model.setNsPrefix("dc", DC.getURI());
		model.setNsPrefix("xsd", XSD.getURI());
	}
	
	public Model getOntologyModel() {
		return model;
	}
	
	public 	void init(String baseIRI, Resource generatedOntology, 
			boolean serveVocabulary, boolean generateDefinitionLabels) {
		Resource ont = generatedOntology.inModel(model);
		ont.addProperty(RDF.type, OWL.Ontology);
		ont.addProperty(OWL.imports, 
				model.getResource(MappingGenerator.dropTrailingHash(DC.getURI())));
		ont.addProperty(DC.creator, CREATOR);
		this.generatedOntology = ont;
	}

	public void addPrefix(String prefix, String uri) {
		model.setNsPrefix(prefix, uri);
	}

	public void generateDatabase(SQLConnection connection, String startupSQLScript) {
		// Do nothing
	}
	
	public void generateEntities(Resource class_, TableName table,
			TemplateValueMaker iriTemplate, List<Identifier> blankNodeColumns) {
		if (class_ == null) return;
		Resource c = class_.inModel(model);
		c.addProperty(RDF.type, RDFS.Class);
		c.addProperty(RDF.type, OWL.Class);
		c.addProperty(RDFS.label, getLabel(table));
		if (generatedOntology != null) {
			c.addProperty(RDFS.isDefinedBy, generatedOntology);
		}
		classes.put(table, c);
	}

	public void generateEntityLabels(TemplateValueMaker labelTemplate,
			TableName table) {
		model.add(RDFS.label, RDF.type, RDF.Property);
	}

	public void generateColumnProperty(Property property, TableName table,
			Identifier column, DataType datatype) {
		Property p = property.inModel(model);
		p.addProperty(RDF.type, RDF.Property);
		p.addProperty(RDF.type, OWL.DatatypeProperty);
		p.addProperty(RDFS.label, getLabel(table, column));
		domains.put(p, table);
		if (datatype.rdfType() != null && XSD.xstring.getURI().equals(datatype.rdfType())) {
			p.addProperty(RDFS.range, model.getResource(datatype.rdfType()));
		}
		if (generatedOntology != null) {
			p.addProperty(RDFS.isDefinedBy, generatedOntology);
		}
	}

	public void generateRefProperty(Property property, TableName table,
			ForeignKey foreignKey) {
		Property p = property.inModel(model);
		p.addProperty(RDF.type, RDF.Property);
		p.addProperty(RDF.type, OWL.ObjectProperty);
		p.addProperty(RDFS.label, getLabel(table, foreignKey.getLocalColumns()));
		domains.put(p, table);
		ranges.put(p, foreignKey.getReferencedTable());
		if (generatedOntology != null) {
			p.addProperty(RDFS.isDefinedBy, generatedOntology);
		}
	}

	public void generateLinkProperty(Property property, TableName table,
			ForeignKey fk1, ForeignKey fk2) {
		Property p = property.inModel(model);
		p.addProperty(RDF.type, RDF.Property);
		p.addProperty(RDF.type, OWL.ObjectProperty);
		p.addProperty(RDFS.label, getLabel(table));
		domains.put(p, fk1.getReferencedTable());
		ranges.put(p, fk2.getReferencedTable());
		if (generatedOntology != null) {
			p.addProperty(RDFS.isDefinedBy, generatedOntology);
		}
	}

	public void skipColumn(TableName table, Identifier column, String reason) {
		// Do nothing
	}

	public void close() {
		for (Property p: domains.keySet()) {
			if (classes.containsKey(domains.get(p))) {
				p.addProperty(RDFS.domain, classes.get(domains.get(p)));
			} else {
				p.addProperty(RDFS.domain, RDFS.Resource);
			}
		}
		for (Property p: ranges.keySet()) {
			if (classes.containsKey(ranges.get(p))) {
				p.addProperty(RDFS.range, classes.get(ranges.get(p)));
			} else {
				p.addProperty(RDFS.range, RDFS.Resource);
			}
		}
	}

	private String getLabel(TableName table) {
		return table.getTable().getName();
	}
	
	private String getLabel(TableName table, Identifier column) {
		return table.getTable().getName() + " " + column.getName();		
	}
	
	private String getLabel(TableName table, Key columns) {
		StringBuilder s = new StringBuilder(table.getTable().getName());
		for (Identifier column: columns) {
			s.append(" ");
			s.append(column.getName());
		}
		return s.toString();
	}
}
