package org.d2rq.mapgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.lang.AliasDeclaration;
import org.d2rq.lang.ClassMap;
import org.d2rq.lang.Configuration;
import org.d2rq.lang.Database;
import org.d2rq.lang.Join;
import org.d2rq.lang.Mapping;
import org.d2rq.lang.Microsyntax;
import org.d2rq.lang.PropertyBridge;
import org.d2rq.values.TemplateValueMaker;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class D2RQTarget implements Target {
	private final static Logger log = Logger.getLogger(D2RQTarget.class);

	private final Model model = ModelFactory.createDefaultModel();
	private final Map<TableName,ClassMap> classMaps = 
			new HashMap<TableName,ClassMap>();
	private final UniqueLocalNameGenerator stringMaker = 
			new UniqueLocalNameGenerator();
	private final Mapping mapping;
	private final Database database;
	private String mapNamespaceURI = "#";
	private boolean generateDefinitionLabels = true;

	public D2RQTarget() {
		mapping = new Mapping();
		database = new Database(
				model.createResource(mapNamespaceURI + "database"));
		mapping.addDatabase(database);
	}
	
	public Mapping getMapping() {
		return mapping;
	}

	public void init(String baseIRI, Resource generatedOntology, 
			boolean serveVocabulary, boolean generateDefinitionLabels) {
		mapping.setBaseIRI(baseIRI);
		mapNamespaceURI = baseIRI + "#";
		model.setNsPrefix("map", mapNamespaceURI);
		log.info("Generating d2rq:Configuration instance");
		Configuration configuration = new Configuration(
				model.createResource(mapNamespaceURI + "configuration"));
		configuration.setServeVocabulary(serveVocabulary);
		mapping.setConfiguration(configuration);
		this.generateDefinitionLabels = generateDefinitionLabels;
	}
	
	public void addPrefix(String prefix, String uri) {
		model.setNsPrefix(prefix, uri);
	}

	public void generateDatabase(SQLConnection connection, String startupSQLScript) {
		log.info("Generating d2rq:Database instance");
		database.setJDBCDriver(connection.getJdbcDriverClass());
		database.setJdbcURL(connection.getJdbcURL());
		database.setUsername(connection.getUsername());
		database.setPassword(connection.getPassword());
		if (startupSQLScript != null) {
			database.setStartupSQLScript(model.createResource(startupSQLScript));
		}
		Properties props = connection.vendor().getDefaultConnectionProperties();
		for (String property: props.stringPropertyNames()) {
			database.setConnectionProperty(property, props.getProperty(property));
		}
	}
	
	public void generateEntities(Resource class_, TableName table,
			TemplateValueMaker iriTemplate, List<Identifier> blankNodeColumns) {
		log.info("Generating d2rq:ClassMap instance for table " + Microsyntax.toString(table));
		ClassMap result = getClassMap(table);
		result.setComment("Table " + Microsyntax.toString(table));
		if (iriTemplate == null) {
			result.setBNodeIdColumns(table.qualifyIdentifiers(blankNodeColumns));
		} else {
			if (iriTemplate.columns().length == 0) {
				result.setComment(result.getComment() + 
						"\nNOTE: Sorry, I don't know which columns to put into the d2rq:uriPattern\n" +
						"because the table doesn't have a primary key. Please specify it manually.");
			}
			result.setURIPattern(Microsyntax.toString(iriTemplate));
		}
		if (class_ != null) {
			result.addClass(class_);
			if (generateDefinitionLabels) {
				result.addDefinitionLabel(model.createLiteral(
						Microsyntax.toString(table)));
			}
		}
	}

	public void generateEntityLabels(TemplateValueMaker labelTemplate, TableName table) {
		PropertyBridge bridge = new PropertyBridge(getLabelBridgeResource(table));
		bridge.setBelongsToClassMap(getClassMap(table));
		bridge.addProperty(RDFS.label);
		bridge.setPattern(Microsyntax.toString(labelTemplate));
	}
	
	public void generateColumnProperty(Property property,
			TableName table, Identifier column, DataType datatype) {
		PropertyBridge bridge = new PropertyBridge(getPropertyBridgeResource(table, column));
		bridge.setBelongsToClassMap(getClassMap(table));
		bridge.addProperty(property);
		if (generateDefinitionLabels) {
			bridge.addDefinitionLabel(model.createLiteral(
					table.getTable().getName() + " " + column.getName()));
		}
		bridge.setColumn(table.qualifyIdentifier(column));
		String datatypeURI = datatype.rdfType();
		if (datatypeURI != null && !XSD.xstring.getURI().equals(datatypeURI)) {
			// We use plain literals instead of xsd:strings, so skip
			// this if it's an xsd:string
			bridge.setDatatype(datatypeURI);
		}
		if (datatype.valueRegex() != null) {
			bridge.addValueRegex(datatype.valueRegex());
		}
	}

	public void generateRefProperty(Property property, 
			TableName table, ForeignKey foreignKey) {
		Key localColumns = foreignKey.getLocalColumns();
		TableName referencedTable = foreignKey.getReferencedTable();
		PropertyBridge bridge = new PropertyBridge(
				getPropertyBridgeResource(table, localColumns));
		bridge.setBelongsToClassMap(getClassMap(table));
		bridge.addProperty(property);
		bridge.setRefersToClassMap(getClassMap(referencedTable));
		TableName target = referencedTable;
		if (referencedTable.equals(table)) {
			// Same-table join? Then we need to set up an alias for the table and join to that
			target = TableName.create(null, null, 
					Identifier.createDelimited( 
							Microsyntax.toString(referencedTable).replace('.', '_') + "__alias"));
			bridge.addAlias(new AliasDeclaration(referencedTable, target));
			foreignKey = new ForeignKey(foreignKey.getLocalColumns(), foreignKey.getReferencedColumns(), target);
		}
		for (Join join: Join.createFrom(table, foreignKey)) {
			bridge.addJoin(join);
		}
	}

	public void generateLinkProperty(Property property, 
			TableName table, ForeignKey fk1, ForeignKey fk2) {
		log.info("Generating d2rq:PropertyBridge instance for table " + Microsyntax.toString(table));
		TableName referencedTable1 = fk1.getReferencedTable();
		TableName referencedTable2 = fk2.getReferencedTable();
		PropertyBridge bridge = new PropertyBridge(getLinkPropertyBridgeResource(table));
		boolean isSelfJoin = referencedTable1.equals(referencedTable2);
		bridge.setComment("Table " + Microsyntax.toString(table) + (isSelfJoin ? " (n:m self-join)" : " (n:m)"));
		bridge.setBelongsToClassMap(getClassMap(referencedTable1));
		bridge.addProperty(property);
		bridge.setRefersToClassMap(getClassMap(referencedTable2));
		TableName target = referencedTable2;
		if (isSelfJoin) {
			target = TableName.create(null, null, 
					Identifier.createDelimited( 
							Microsyntax.toString(referencedTable2).replace('.', '_') +
							Microsyntax.toString(table).replace('.', '_') +
							"__alias"));
			bridge.addAlias(new AliasDeclaration(referencedTable2, target)); 
			fk2 = new ForeignKey(fk2.getLocalColumns(), fk2.getReferencedColumns(), target);
		}
		for (Join join: Join.createFrom(table, fk1)) {
			bridge.addJoin(join);
		}
		for (Join join: Join.createFrom(table, fk2)) {
			bridge.addJoin(join);
		}
	}
	
	public void skipColumn(TableName table, Identifier column, String reason) {
		ClassMap map = getClassMap(table);
		map.setComment(map.getComment() + "\nSkipping column " + 
				table + "." + column + ". " + reason);
	}
	
	public void close() {
		mapping.getPrefixes().setNsPrefixes(model);
		log.info("Done!");
	}
	
	private ClassMap getClassMap(TableName table) {
		if (classMaps.containsKey(table)) {
			return classMaps.get(table);
		}
		ClassMap result = new ClassMap(getClassMapResource(table));
		result.setDatabase(database);
		mapping.addClassMap(result);
		classMaps.put(table, result);
		return result;
	}
	
	private Resource getClassMapResource(TableName table) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table)));
	}
	
	private Resource getLabelBridgeResource(TableName table) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table) + "__label"));
	}
	
	private Resource getPropertyBridgeResource(TableName table, Identifier column) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table, column)));
	}
	
	private Resource getPropertyBridgeResource(TableName table, Key columns) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table, columns) + "__ref"));
	}
	
	private Resource getLinkPropertyBridgeResource(TableName table) {
		return model.createResource(mapNamespaceURI + 
				IRIEncoder.encode(stringMaker.toString(table) + "__link"));
	}
}
