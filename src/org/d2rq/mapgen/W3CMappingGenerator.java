package org.d2rq.mapgen;

import java.util.Iterator;
import java.util.List;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.lang.Microsyntax;


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
	}

	@Override
	protected void writeEntityIdentifier(TableDef table, List<Identifier> identifierColumns) {
		String uriPattern = this.instanceNamespaceURI + encodeTableName(table.getName());
		Iterator<Identifier> it = identifierColumns.iterator();
		int i = 0;
		while (it.hasNext()) {
			uriPattern += i == 0 ? "/" : ";";
			i++;
			
			Identifier column = it.next();
			uriPattern += encodeColumnName(column) + "=@@" + Microsyntax.toString(table.getName(), column);
			if (!table.getColumnDef(column).getDataType().isIRISafe()) {
				uriPattern += "|encode";
			}
			uriPattern += "@@";
		}
		this.out.println("\td2rq:uriPattern \"" + uriPattern + "\";");
	}
	
	@Override
	protected void writePseudoEntityIdentifier(TableDef table) {
		List<Identifier> usedColumns = filter(table, table.getColumnNames(), true, "pseudo identifier column");
		out.print("\td2rq:bNodeIdColumns \"");
		Iterator<Identifier> it = usedColumns.iterator();
		while (it.hasNext()) {
			out.print(Microsyntax.toString(table.getName(), it.next()));
			if (it.hasNext()) {
				out.print(",");
			}
		}
		out.println("\";");
	}
	
	@Override
	protected String vocabularyIRITurtle(TableName tableName) {
		return "<" + encodeTableName(tableName) + ">";
	}
	
	@Override
	protected String vocabularyIRITurtle(TableName tableName, Identifier column) {
		return "<" + encodeTableName(tableName) + "#" + encodeColumnName(column) + ">";
	}
	
	@Override
	protected String vocabularyIRITurtle(TableName tableName, Key columns) {
		StringBuffer result = new StringBuffer();
		result.append("<");
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
		result.append(">");
		return result.toString();
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