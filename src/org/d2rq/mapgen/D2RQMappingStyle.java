package org.d2rq.mapgen;

import java.util.Iterator;
import java.util.List;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.IdentifierList;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.values.TemplateValueMaker;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Generates an original-style mapping. Unlike the W3C Direct Mapping, this
 * handles N:M link tables, includes label definitions and instance labels,
 * and uses different URI patterns.
 */
public class D2RQMappingStyle implements MappingStyle {
	private final MappingGenerator generator;
	private final Model model = ModelFactory.createDefaultModel();
	private final UniqueLocalNameGenerator stringMaker = new UniqueLocalNameGenerator();
	private final String baseIRI;
	private final String vocabBaseIRI;

	public D2RQMappingStyle(SQLConnection connection, String baseIRI) {
		this.baseIRI = baseIRI;
		this.vocabBaseIRI = baseIRI + "vocab/";
		model.setNsPrefix("rdf", RDF.getURI());
		model.setNsPrefix("rdfs", RDFS.getURI());
		model.setNsPrefix("xsd", XSD.getURI());
		model.setNsPrefix("vocab", vocabBaseIRI);
		generator = new MappingGenerator(this, connection);
		generator.setGenerateLabelBridges(true);
		generator.setHandleLinkTables(true);
		generator.setGenerateDefinitionLabels(true);
		generator.setServeVocabulary(true);
		generator.setSkipForeignKeyTargetColumns(true);
		generator.setUseUniqueKeysAsEntityID(true);
	}
	
	public MappingGenerator getMappingGenerator() {
		return generator;
	}
	
	public String getBaseIRI() {
		return baseIRI;
	}
	
	public PrefixMapping getPrefixes() {
		return model;
	}
	
	public TemplateValueMaker getEntityIRITemplate(TableDef table, IdentifierList columns) {
		TemplateValueMaker.Builder builder = TemplateValueMaker.builder();
		// We don't use the base IRI here, so the template will produce relative IRIs
		if (table.getName().getSchema() != null) {
			builder.add(IRIEncoder.encode(table.getName().getSchema().getName()));
			builder.add("/");
		}
		builder.add(IRIEncoder.encode(table.getName().getTable().getName()));
		if (columns != null) {
			for (Identifier column: columns) {
				builder.add("/");
				if (table.getColumnDef(column).getDataType().isIRISafe()) {
					builder.add(table.getName().qualifyIdentifier(column));
				} else {
					builder.add(table.getName().qualifyIdentifier(column), TemplateValueMaker.URLIFY);
				}
			}
		}
		return builder.build();
	}
	
	public List<Identifier> getEntityPseudoKeyColumns(List<ColumnDef> columns) {
		return null;
	}
	
	public Resource getGeneratedOntologyResource() {
		return model.createResource(MappingGenerator.dropTrailingHash(vocabBaseIRI));
	}
	
	public Resource getTableClass(TableName tableName) {
		return model.createResource(vocabBaseIRI + 
				IRIEncoder.encode(stringMaker.toString(tableName)));
	}
	
	public Property getColumnProperty(TableName tableName, Identifier column) {
		return model.createProperty(vocabBaseIRI + 
				IRIEncoder.encode(stringMaker.toString(tableName, column)));
	}
	
	public Property getForeignKeyProperty(TableName tableName, ForeignKey fk) {
		return model.createProperty(vocabBaseIRI + 
				IRIEncoder.encode(stringMaker.toString(tableName, fk.getLocalColumns())));
	}	

	public Property getLinkProperty(TableName linkTable) {
		return model.createProperty(vocabBaseIRI + 
				IRIEncoder.encode(stringMaker.toString(linkTable)));
	}
	
	public TemplateValueMaker getEntityLabelTemplate(TableName tableName, IdentifierList columns) {
		TemplateValueMaker.Builder builder = TemplateValueMaker.builder();
		builder.add(tableName.getTable().getName());
		builder.add(" #");
		Iterator<Identifier> it = columns.iterator();
		while (it.hasNext()) {
			builder.add(tableName.qualifyIdentifier(it.next()));
			if (it.hasNext()) {
				builder.add("/");
			}
		}
		return builder.build();
	}

}
