package de.fuberlin.wiwiss.d2rq.mapgen;

import java.util.Iterator;
import java.util.List;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.values.Pattern;

/**
 * Generates a D2RQ mapping compatible with W3C's Direct Mapping by introspecting a database schema.
 * Result is available as a high-quality Turtle serialization, or
 * as a parsed model.
 *  
 * @author Lu&iacute;s Eufrasio (luis.eufrasio@gmail.com)
 */
public class W3CMappingGenerator extends MappingGenerator {
	private Pattern.EncodeFunction encFunction = new Pattern.EncodeFunction();
	
	public W3CMappingGenerator(ConnectedDB database) {
		super(database);
		setGenerateLabelBridges(false);
		setHandleLinkTables(false);
		setGenerateDefinitionLabels(false);
		setServeVocabularyFalse(true);
	}

	@Override
	protected String uriPattern(RelationName tableName) {
		String result = encodeTableName(this.instanceNamespaceURI + tableName.qualifiedName());
		Iterator it = this.schema.primaryKeyColumns(tableName).iterator();
		int i = 0;
		while (it.hasNext()) {
			result += i == 0 ? "/" : ".";
			i++;
			
			Attribute column = (Attribute) it.next();
			String attributeName = encodeColumnName(column);
			String attributeQName = column.qualifiedName();
			result += attributeName + "-@@" + attributeQName;
			if (DatabaseSchemaInspector.isStringType(this.schema.columnType(column))) {
				result += "|encode";
			}
			result += "@@";
		}
		return result;
	}
	
	@Override
	protected String vocabularyTermQName(RelationName table) {
		return "<" + encodeTableName(table.qualifiedName()) + ">";
	}
	
	@Override
	protected String vocabularyTermQName(Attribute attribute) {
		return "<" + toRelationColumnName(attribute) + ">";
	}
	
	private String toRelationColumnName(Attribute column) {
		return encodeTableName(column.tableName()) + "#"
				+ encodeColumnName(column);
	}
	
	@Override
	protected String vocabularyTermQName(List attributes) {
		StringBuffer result = new StringBuffer();
		result.append("<");
		String tableName = ((Attribute) attributes.get(0)).tableName();
		result.append(encodeTableName(tableName));
		Iterator it = attributes.iterator();
		int i = 1;
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			String attributeName = encodeColumnName(column);
			if (i == 1) {
				result.append("#ref-");
				result.append(attributeName);
			} else {
				result.append("." + attributeName);
			}
			i++;
		}
		result.append(">");
		return result.toString();
	}

	private String encodeTableName(String tableName) {
		return encFunction.encode(tableName).replaceAll("%2E", ".");
	}

	private String encodeColumnName(Attribute column) {
		return encFunction.encode(column.attributeName()).replaceAll("-", "%3D").replaceAll("%2E", ".");
	}
}