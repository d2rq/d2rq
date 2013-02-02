package org.d2rq.mapgen;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.IdentifierList;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.lang.Microsyntax;
import org.d2rq.values.TemplateValueMaker;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.PrefixMapping.IllegalPrefixException;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;


/**
 * Generates a D2RQ mapping by introspecting a database schema.
 * Result is available as a high-quality Turtle serialization, or
 * as a parsed model.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MappingGenerator {
	private final static Logger log = Logger.getLogger(MappingGenerator.class);

	private final MappingStyle style;
	private final SQLConnection sqlConnection;
	private final List<TableName> tablesWithoutUniqueKey = 
			new ArrayList<TableName>();
	private final PrefixMapping prefixes = new PrefixMappingImpl();
	private Filter filter = Filter.ALL;
	private boolean finished = false;
	private boolean generateClasses = true;
	private boolean generateLabelBridges = true;
	private boolean generateDefinitionLabels = true;
	private boolean handleLinkTables = true;
	private boolean serveVocabulary = true;
	private boolean skipForeignKeyTargetColumns = true;
	private boolean useUniqueKeysAsEntityID = true;
	private boolean suppressWarnings = false;
	private URI startupSQLScript;
	private Target target;
	
	public MappingGenerator(MappingStyle style, SQLConnection sqlConnection) {
		this.style = style;
		this.sqlConnection = sqlConnection;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}
	
	public void setStartupSQLScript(URI uri) {
		startupSQLScript = uri;
	}
	
	/**
	 * @param flag Generate an rdfs:label property bridge based on the PK?
	 */
	public void setGenerateLabelBridges(boolean flag) {
		this.generateLabelBridges = flag;
	}

	/**
	 * @param flag Add <code>rdfs:label</code>s to auto-generated classes and properties?
	 */
	public void setGenerateDefinitionLabels(boolean flag) {
		this.generateDefinitionLabels = flag;
	}
	
	/**
	 * @param flag Generate a d2rq:class for every class map?
	 */
	public void setGenerateClasses(boolean flag) {
		this.generateClasses = flag;
	}
		

	/**
	 * @param flag Handle Link Tables as properties (true) or normal tables (false)
	 */
	public void setHandleLinkTables(boolean flag) {
		this.handleLinkTables = flag;
	}

	/**
	 * @param flag Value for d2rq:serveVocabulary in map:Configuration
	 */
	public void setServeVocabulary(boolean flag) {
		this.serveVocabulary = flag;
	}

	public void setSkipForeignKeyTargetColumns(boolean flag) {
		skipForeignKeyTargetColumns = flag;
	}
	
	public void setUseUniqueKeysAsEntityID(boolean flag) {
		useUniqueKeysAsEntityID = flag;
	}
	
	public void setSuppressWarnings(boolean flag) {
		suppressWarnings = flag;
	}
	
	public void generate(Target generationTarget) {
		if (finished) return;
		finished = true;
		target = generationTarget;
		target.init(style.getBaseIRI(), style.getGeneratedOntologyResource(),
				serveVocabulary, generateDefinitionLabels);
		for (String prefix: style.getPrefixes().getNsPrefixMap().keySet()) {
			target.addPrefix(prefix, style.getPrefixes().getNsPrefixMap().get(prefix));
			prefixes.setNsPrefix(prefix, style.getPrefixes().getNsPrefixMap().get(prefix));
		}
		target.generateDatabase(sqlConnection, 
				startupSQLScript == null ? null : startupSQLScript.toString());
		List<TableName> tableNames = new ArrayList<TableName>();
		for (TableName tableName: sqlConnection.getTableNames(filter.getSingleSchema())) {
			if (!filter.matches(tableName)) {
				log.info("Skipping table " + tableName);
				continue;
			}
			tableNames.add(tableName);
		}
		log.info("Filter '" + filter + "' matches " + tableNames.size() + " total tables");
		for (TableName tableName: tableNames) {
			TableDef table = sqlConnection.getTable(tableName).getTableDefinition();
			if (handleLinkTables && isLinkTable(table)) {
				Iterator<ForeignKey> it = table.getForeignKeys().iterator();
				ForeignKey fk1 = it.next();
				ForeignKey fk2 = it.next();
				TableName referencedTable1 = fk1.getReferencedTable();
				TableName referencedTable2 = fk2.getReferencedTable();
				if (!filter.matches(referencedTable1) || 
						!filter.matchesAll(referencedTable1, fk1.getLocalColumns()) || 
						!filter.matchesAll(referencedTable1, fk1.getReferencedColumns()) ||
						!filter.matches(referencedTable2) || 
						!filter.matchesAll(referencedTable2, fk2.getLocalColumns()) || 
						!filter.matchesAll(referencedTable2, fk2.getReferencedColumns())) {
					log.info("Skipping link table " + tableName);
					continue;
				}
				target.generateLinkProperty(
						style.getLinkProperty(tableName), tableName, fk1, fk2);
			} else {
				Resource class_ = generateClasses ? 
						style.getTableClass(tableName) : null;
				TemplateValueMaker iriTemplate = null;
				List<Identifier> blankNodeColumns = null;
				IdentifierList key = findBestKey(table);
				if (key == null) {
					List<ColumnDef> filteredColumns = new ArrayList<ColumnDef>();
					for (ColumnDef columnDef: table.getColumns()) {
						if (filter.matches(tableName, columnDef.getName())) {
							filteredColumns.add(columnDef);
						}
					}
					if (style.getEntityPseudoKeyColumns(filteredColumns) == null) {
						tablesWithoutUniqueKey.add(tableName);
						iriTemplate = style.getEntityIRITemplate(table, null);
					} else {
						blankNodeColumns = style.getEntityPseudoKeyColumns(filteredColumns);
					}
				} else {
					iriTemplate = style.getEntityIRITemplate(table, key);
				}
				target.generateEntities(class_, tableName, 
						iriTemplate, blankNodeColumns);
				if (class_ != null) {
					if (tableName.getSchema() != null) {
						tryRegisterPrefix(tableName.getSchema().getName().toLowerCase(),
								class_.getNameSpace());
					}
					if (tableName.getCatalog() != null) {
						tryRegisterPrefix(tableName.getCatalog().getName().toLowerCase(),
								class_.getNameSpace());
					}
				}
				if (generateLabelBridges && key != null) {
					target.generateEntityLabels(
							style.getEntityLabelTemplate(tableName, key), tableName);
				}
				for (Identifier column: table.getColumnNames()) {
					if (skipForeignKeyTargetColumns && isInForeignKey(column, table)) continue;
					if (!filter.matches(tableName, column)) {
						log.info("Skipping filtered column " + column);
						continue;
					}
					DataType type = table.getColumnDef(column).getDataType();
					if (type == null) {
						String message = "The datatype is unknown to D2RQ.\n";
						message += "You can override the column's datatype using d2rq:xxxColumn and add a property bridge.";
						if (!suppressWarnings) {
							log.warn(message);
						}
						target.skipColumn(tableName, column, message);
						continue;
					}
					if (type.isUnsupported()) {
						String message = "The datatype " + type + " cannot be mapped to RDF.";
						if (!suppressWarnings) {
							log.warn(message);
						}
						target.skipColumn(tableName, column, message);
						continue;
					}
					Property property = style.getColumnProperty(tableName, column);
					target.generateColumnProperty(property, tableName, column, type);
					tryRegisterPrefix(
							tableName.getTable().getName().toLowerCase(), 
							property.getNameSpace());
				}
				for (ForeignKey fk: table.getForeignKeys()) {
					if (!filter.matches(fk.getReferencedTable()) || 
							!filter.matchesAll(tableName, fk.getLocalColumns()) || 
							!filter.matchesAll(fk.getReferencedTable(), fk.getReferencedColumns())) {
						log.info("Skipping foreign key: " + fk);
						continue;
					}
					target.generateRefProperty(
							style.getForeignKeyProperty(tableName, fk), 
							tableName, fk);
				}
			}
		}
		if (!tablesWithoutUniqueKey.isEmpty()) {
			StringBuilder s = new StringBuilder();
			s.append("Sorry, I don't know which columns to put into the ");
			s.append("d2rq:uriPattern of tables that don't have a ");
			s.append("primary or unique key. Please specify them manually: ");
			Iterator<TableName> it = tablesWithoutUniqueKey.iterator();
			while (it.hasNext()) {
				s.append(Microsyntax.toString(it.next()));
				if (it.hasNext()) {
					s.append(", ");
				}
			}
			if (!suppressWarnings) {
				log.warn(s.toString());
			}
		}
		target.close();
	}

	private void tryRegisterPrefix(String prefix, String uri) {
		if (prefixes.getNsPrefixMap().containsKey(prefix)) return;
		if (prefixes.getNsPrefixMap().containsValue(uri)) return;
		try {
			prefixes.setNsPrefix(prefix, uri);
			target.addPrefix(prefix, uri);
		} catch (IllegalPrefixException ex) {
			// Oh well, no prefix then.
		}
	}

	private IdentifierList findBestKey(TableDef table) {
		if (table.getPrimaryKey() != null) {
			if (!isExcluded(table, table.getPrimaryKey(), true)) {
				return table.getPrimaryKey();
			}
		}
		if (!useUniqueKeysAsEntityID) return null;
		for (IdentifierList uniqueKey: table.getUniqueKeys()) {
			if (!isExcluded(table, uniqueKey, true)) {
				return uniqueKey;
			}
		}
		return null;
	}
	
	private boolean isExcluded(TableDef table, IdentifierList columns, boolean requireDistinct) {
		for (Identifier column: columns) {
			if (isExcluded(table, column, requireDistinct)) return true;
		}
		return false;
	}
	
	private boolean isExcluded(TableDef table, Identifier column, boolean requireDistinct) {
		if (!filter.matches(table.getName(), column)) return true;
		DataType type = table.getColumnDef(column).getDataType();
		return type == null || type.isUnsupported() || (requireDistinct && !type.supportsDistinct());
	}

	/**
	 * A table T is considered to be a link table if it has exactly two
	 * foreign key constraints, and the constraints reference other
	 * tables (not T), and the constraints cover all columns of T,
	 * and there are no foreign keys from other tables pointing to this table
	 */
	private boolean isLinkTable(TableDef table) {
		if (table.getForeignKeys().size() != 2) return false;
		if (sqlConnection.isReferencedByForeignKey(table.getName())) return false;
		List<Identifier> columns = new ArrayList<Identifier>();
		for (ColumnDef qualified: table.getColumns()) {
			columns.add(qualified.getName());
		}
		for (ForeignKey foreignKey: table.getForeignKeys()) {
			if (foreignKey.getReferencedTable().equals(table.getName())) return false;
			columns.removeAll(foreignKey.getLocalColumns().getColumns());
		}
		return columns.isEmpty();
	}

	/**
	 * @return <code>true</code> iff the table contains this column as a local column in a foreign key
	 */
	private boolean isInForeignKey(Identifier column, TableDef table) {
		for (ForeignKey fk: table.getForeignKeys()) {
			if (fk.getLocalColumns().contains(column)) return true;
		}
		return false;
	}
	
	public static String dropTrailingHash(String uri) {
		if (!uri.endsWith("#")) {
			return uri;
		}
		return uri.substring(0, uri.length() - 1);
	}
}
