package de.fuberlin.wiwiss.d2rq.mapgen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
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
		setServeVocabulary(false);
		setSkipForeignKeyTargetColumns(false);
	}

	@Override
	protected String uriPattern(RelationName tableName) {
		String result = encodeTableName(this.instanceNamespaceURI + tableName.qualifiedName());
		Iterator<Attribute> it = filteredPrimaryKeyColumns(tableName).iterator();
		int i = 0;
		while (it.hasNext()) {
			result += i == 0 ? "/" : ";";
			i++;
			
			Attribute column = it.next();
			String attributeName = encodeColumnName(column);
			String attributeQName = column.qualifiedName();
			result += attributeName + "=@@" + attributeQName;
			if (!database.columnType(column).isIRISafe()) {
				result += "|encode";
			}
			result += "@@";
		}
		return result;
	}
	
	@Override
	protected void writeSubjectSpec(RelationName tableName) {
		List<Attribute> usedColumns = new ArrayList<Attribute>();
		for (Attribute column: schema.listColumns(tableName)) {
			DataType type = schema.columnType(column);
			if (type == null) {
				writeWarning(new String[]{
						"Ignoring " + column + " in d2rq:bNodeIdColumns because its datatype is unknown."
					}, "");
				continue;
			}
			if (type.isUnsupported()) {
				writeWarning(new String[]{
						"Ignoring " + column + " in d2rq:bNodeIdColumns because its datatype cannot be mapped to RDF."
					}, "");
				continue;
			}
			if (!type.supportsDistinct()) {
				writeWarning(new String[]{
						"Ignoring " + column + " in d2rq:bNodeIdColumns because it doesn't support DISTINCT."
					}, "");
				continue;
			}
			usedColumns.add(column);
		}
		out.print("\td2rq:bNodeIdColumns \"");
		Iterator<Attribute> it = usedColumns.iterator();
		while (it.hasNext()) {
			Attribute column = it.next();
			out.print(column.qualifiedName());
			if (it.hasNext()) {
				out.print(",");
			}
		}
		out.println("\";");
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
	protected String vocabularyTermQName(List<Attribute> attributes) {
		StringBuffer result = new StringBuffer();
		result.append("<");
		String tableName = ((Attribute) attributes.get(0)).tableName();
		result.append(encodeTableName(tableName));
		int i = 1;
		for (Attribute column: attributes) {
			String attributeName = encodeColumnName(column);
			if (i == 1) {
				result.append("#ref-");
				result.append(attributeName);
			} else {
				result.append(";" + attributeName);
			}
			i++;
		}
		result.append(">");
		return result.toString();
	}

	private String encodeTableName(String tableName) {
		return encFunction.encode(tableName);
	}

	private String encodeColumnName(Attribute column) {
		return encFunction.encode(column.attributeName());
	}
}