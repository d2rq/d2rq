package de.fuberlin.wiwiss.d2rq.mapgen;

import java.util.Iterator;
import java.util.List;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * Generates a D2RQ mapping compatible with W3C's Direct Mapping by introspecting a database schema.
 * Result is available as a high-quality Turtle serialization, or
 * as a parsed model.
 *  
 * @author Lu&iacute;s Eufrasio (luis.eufrasio@gmail.com)
 */
public class W3CMappingGenerator extends MappingGenerator {
	
	public W3CMappingGenerator(ConnectedDB database) {
		super(database);
		setGenerateLabelBridges(false);
		setHandleLinkTables(false);
		setGenerateDefinitionLabels(false);
		setServeVocabulary(false);
		setSkipForeignKeyTargetColumns(false);
	}

	@Override
	protected void writeEntityIdentifier(RelationName tableName, List<Attribute> identifierColumns) {
		String uriPattern = this.instanceNamespaceURI + encodeTableName(tableName);
		Iterator<Attribute> it = identifierColumns.iterator();
		int i = 0;
		while (it.hasNext()) {
			uriPattern += i == 0 ? "/" : ";";
			i++;
			
			Attribute column = it.next();
			uriPattern += encodeColumnName(column) + "=@@" + column.qualifiedName();
			if (!database.columnType(column).isIRISafe()) {
				uriPattern += "|encode";
			}
			uriPattern += "@@";
		}
		this.out.println("\td2rq:uriPattern \"" + uriPattern + "\";");
	}
	
	@Override
	protected void writePseudoEntityIdentifier(RelationName tableName) {
		List<Attribute> usedColumns = filter(schema.listColumns(tableName), true, "pseudo identifier column");
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
	protected String vocabularyIRITurtle(RelationName table) {
		return "<" + encodeTableName(table) + ">";
	}
	
	@Override
	protected String vocabularyIRITurtle(Attribute attribute) {
		return "<" + encodeTableName(attribute.relationName()) + "#"
		+ encodeColumnName(attribute) + ">";
	}
	
	@Override
	protected String vocabularyIRITurtle(List<Attribute> attributes) {
		StringBuffer result = new StringBuffer();
		result.append("<");
		result.append(encodeTableName(attributes.get(0).relationName()));
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

	private String encodeTableName(RelationName tableName) {
		return (tableName.schemaName() == null ? "" : IRIEncoder.encode(tableName.schemaName()) + '/')
			+ IRIEncoder.encode(tableName.tableName());
	}

	private String encodeColumnName(Attribute column) {
		return IRIEncoder.encode(column.attributeName());
	}
}