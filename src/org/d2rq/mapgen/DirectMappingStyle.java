package org.d2rq.mapgen;

import java.util.ArrayList;
import java.util.List;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.values.TemplateValueMaker;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;


/**
 * Generates a D2RQ mapping compatible with W3C's Direct Mapping.
 *  
 * @author Lu&iacute;s Eufrasio (luis.eufrasio@gmail.com)
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class DirectMappingStyle implements MappingStyle {
	private final MappingGenerator generator;
	private final Model model = ModelFactory.createDefaultModel();
	private final String baseIRI;
	
	public DirectMappingStyle(SQLConnection connection, String baseIRI) {
		this.generator = new MappingGenerator(this, connection);
		this.baseIRI = baseIRI;
		model.setNsPrefix("rdf", RDF.getURI());
		model.setNsPrefix("xsd", XSD.getURI());
		generator.setGenerateLabelBridges(false);
		generator.setHandleLinkTables(false);
		generator.setGenerateDefinitionLabels(false);
		generator.setServeVocabulary(false);
		generator.setSkipForeignKeyTargetColumns(false);
		generator.setUseUniqueKeysAsEntityID(false);
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
	
	public TemplateValueMaker getEntityIRITemplate(TableDef table, Key columns) {
		TemplateValueMaker.Builder builder = TemplateValueMaker.builder();
		// We don't use baseIRI here, so the template will produce relative IRIs.
		builder.add(encodeTableName(table.getName()));
		if (columns != null) {
			int i = 0;
			for (Identifier column: columns) {
				builder.add(i == 0 ? "/" : ";");
				i++;
				builder.add(encodeColumnName(column));
				builder.add("=");
				if (table.getColumnDef(column).getDataType().isIRISafe()) {
					builder.add(table.getName().qualifyIdentifier(column));
				} else {
					builder.add(table.getName().qualifyIdentifier(column), TemplateValueMaker.ENCODE);
				}
			}
		}
		return builder.build();
	}
	
	public List<Identifier> getEntityPseudoKeyColumns(List<ColumnDef> columns) {
		List<Identifier> result = new ArrayList<Identifier>();
		for (ColumnDef def: columns) {
			DataType type = def.getDataType();
			if (type == null || type.isUnsupported() || !type.supportsDistinct()) {
				continue;
			}
			result.add(def.getName());
		}
		return result;
	}
	
	public TemplateValueMaker getEntityLabelTemplate(TableName tableName, Key columns) {
		// Direct Mapping doesn't do label templates, so we don't need this
		return null;
	}
	
	public Resource getGeneratedOntologyResource() {
		return model.createResource(MappingGenerator.dropTrailingHash(baseIRI));
	}
	
	public Resource getTableClass(TableName tableName) {
		return model.createResource(
				baseIRI + encodeTableName(tableName));
	}
	
	public Property getColumnProperty(TableName tableName, Identifier column) {
		return model.createProperty(
				baseIRI + encodeTableName(tableName) + 
				"#" + encodeColumnName(column));
	}
	
	public Property getForeignKeyProperty(TableName tableName, ForeignKey foreignKey) {
		StringBuffer result = new StringBuffer(baseIRI);
		result.append(encodeTableName(tableName));
		int i = 1;
		for (Identifier column: foreignKey.getLocalColumns().getColumns()) {
			String encoded = encodeColumnName(column);
			if (i == 1) {
				result.append("#ref-");
				result.append(encoded);
			} else {
				result.append(";" + encoded);
			}
			i++;
		}
		return model.createProperty(result.toString());
	}

	public Property getLinkProperty(TableName table) {
		// We don't need this as the Direct Mapping doesn't do link tables
		return null;
	}
	
	private String encodeTableName(TableName tableName) {
		StringBuilder s = new StringBuilder();
		if (tableName.getCatalog() != null) {
			s.append(IRIEncoder.encode(tableName.getCatalog().getName()));
			s.append('/');
		}
		if (tableName.getSchema() != null) {
			s.append(IRIEncoder.encode(tableName.getSchema().getName()));
			s.append('/');
		}
		s.append(IRIEncoder.encode(tableName.getTable().getName()));
		return s.toString();
	}

	private String encodeColumnName(Identifier column) {
		return IRIEncoder.encode(column.getName());
	}
}