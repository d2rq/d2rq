package org.d2rq.mapgen;

import java.util.ArrayList;
import java.util.List;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.lang.ClassMap;
import org.d2rq.values.TemplateValueMaker;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * Generates a D2RQ mapping compatible with W3C's Direct Mapping by introspecting a database schema.
 * Result is available as a high-quality Turtle serialization, or
 * as a parsed model.
 *  
 * @author Lu&iacute;s Eufrasio (luis.eufrasio@gmail.com)
 */
public class W3CMappingGenerator extends MappingGenerator {
	
	public W3CMappingGenerator(SQLConnection database) {
		super(database);
		setGenerateLabelBridges(false);
		setHandleLinkTables(false);
		setGenerateDefinitionLabels(false);
		setServeVocabulary(false);
		setSkipForeignKeyTargetColumns(false);
		setUseUniqueKeysAsEntityID(false);
		setVocabNamespaceURI("");
	}

	@Override
	protected TemplateValueMaker getEntityIdentifierPattern(
			TableDef table, Key columns) {
		TemplateValueMaker.Builder builder = TemplateValueMaker.builder();
		builder.add(instanceNamespaceURI);
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
	
	@Override
	protected void definePseudoEntityIdentifier(ClassMap result, TableDef table) {
		List<ColumnName> columns = new ArrayList<ColumnName>();
		for (ColumnDef column: table.getColumns()) {
			if (!isFiltered(table, column.getName(), true)) {
				columns.add(table.getName().qualifyIdentifier(column.getName()));
			}
		}
		result.setBNodeIdColumns(columns);
	}
	
	@Override
	protected Resource getTableClass(TableName tableName) {
		return mappingResources.createResource(
				vocabNamespaceURI + encodeTableName(tableName));
	}
	
	@Override
	protected Property getColumnProperty(TableName tableName, Identifier column) {
		return mappingResources.createProperty(
				vocabNamespaceURI + encodeTableName(tableName) + 
				"#" + encodeColumnName(column));
	}
	
	@Override
	protected Property getForeignKeyProperty(TableName tableName, Key columns) {
		StringBuffer result = new StringBuffer(vocabNamespaceURI);
		result.append(encodeTableName(tableName));
		int i = 1;
		for (Identifier column: columns.getColumns()) {
			String encoded = encodeColumnName(column);
			if (i == 1) {
				result.append("#ref-");
				result.append(encoded);
			} else {
				result.append(";" + encoded);
			}
			i++;
		}
		return mappingResources.createProperty(result.toString());
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