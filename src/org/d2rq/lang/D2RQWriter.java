package org.d2rq.lang;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.d2rq.db.op.LimitOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.lang.TranslationTable.Translation;
import org.d2rq.vocab.D2RQ;
import org.d2rq.vocab.JDBC;
import org.d2rq.writer.MappingWriter;
import org.d2rq.writer.PrettyTurtleWriter;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;

/**
 * Writes a D2RQ {@link Mapping} instance as a Turtle file to an output stream.
 * 
 * TODO: D2RQWriter needs much more testing.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class D2RQWriter implements MappingWriter {
	private final Mapping mapping;
	private final PrefixMapping prefixes = new PrefixMappingImpl();
	private PrettyTurtleWriter out;
	
	public D2RQWriter(Mapping mapping) {
		this.mapping = mapping;
		this.prefixes.setNsPrefixes(mapping.getPrefixes());
		if (!mapping.getPrefixes().getNsPrefixMap().containsValue(D2RQ.NS)) {
			prefixes.setNsPrefix("d2rq", D2RQ.NS);
		}
		if (!mapping.getPrefixes().getNsPrefixMap().containsValue(JDBC.NS)) {
			prefixes.setNsPrefix("d2rq", JDBC.NS);
		}
	}
	
	public void write(OutputStream outStream) {
		try {
			write(new OutputStreamWriter(outStream, "utf-8"));
		} catch (UnsupportedEncodingException ex) {
			// can't happen, UTF-8 always supported
		}
	}
	
	public void write(Writer outWriter) {
		out = new PrettyTurtleWriter(mapping.getBaseIRI(), prefixes, outWriter);
		printConfiguration(mapping.configuration());
		for (Database database: mapping.databases()) {
			printDatabase(database);
		}
		out.println();
		for (Resource r: mapping.classMapResources()) {
			ClassMap classMap = mapping.classMap(r);
			printClassMap(classMap);
			for (PropertyBridge bridge: classMap.propertyBridges()) {
				printPropertyBridge(bridge);
			}
			out.println();
		}
		for (Resource r: mapping.downloadMapResources()) {
			DownloadMap downloadMap = mapping.downloadMap(r);
			printDownloadMap(downloadMap);
		}
		out.println();
		for (Resource r: mapping.translationTableResources()) {
			TranslationTable translationTable = mapping.translationTable(r);
			printTranslationTable(translationTable);
		}
		out.flush();
	}
	
	private void printConfiguration(Configuration configuration) {
		if (configuration.getServeVocabulary() == Configuration.DEFAULT_SERVE_VOCABULARY
				&& configuration.getUseAllOptimizations() == Configuration.DEFAULT_USE_ALL_OPTIMIZATIONS) {
			return;
		}
		printMapObject(configuration, D2RQ.Configuration);
		out.printProperty(
				configuration.getServeVocabulary() != Configuration.DEFAULT_SERVE_VOCABULARY,
				D2RQ.serveVocabulary, 
				configuration.getServeVocabulary());
		out.printProperty(
				configuration.getUseAllOptimizations() != Configuration.DEFAULT_USE_ALL_OPTIMIZATIONS,
				D2RQ.useAllOptimizations,
				configuration.getUseAllOptimizations());
		out.printResourceEnd();
	}

	private void printDatabase(Database db) {
		printMapObject(db, D2RQ.Database);
		out.printProperty(D2RQ.jdbcURL, db.getJdbcURL());
		out.printProperty(D2RQ.jdbcDriver, db.getJDBCDriver());
		out.printProperty(D2RQ.username, db.getUsername());
		out.printProperty(D2RQ.password, db.getPassword());
		for (String jdbcProperty: db.getConnectionProperties().stringPropertyNames()) {
			out.printProperty(
					JDBC.getProperty(jdbcProperty), 
					db.getConnectionProperties().getProperty(jdbcProperty));
		}
		out.printProperty(db.getResultSizeLimit() != Database.NO_LIMIT,
				D2RQ.resultSizeLimit, db.getResultSizeLimit());
		out.printProperty(db.getFetchSize() != Database.NO_FETCH_SIZE, 
				D2RQ.fetchSize, db.getFetchSize());
		out.printURIProperty(D2RQ.startupSQLScript, db.getStartupSQLScript());
		List<ColumnName> overriddenColumns = new ArrayList<ColumnName>(db.getColumnTypes().keySet());
		Collections.sort(overriddenColumns);
		for (ColumnName column: overriddenColumns) {
			Property property = null;
			switch (db.getColumnTypes().get(column)) {
				case CHARACTER: property = D2RQ.textColumn; break;
				case NUMERIC: property = D2RQ.numericColumn; break;
				case BOOLEAN: property = D2RQ.booleanColumn; break;
				case DATE: property = D2RQ.dateColumn; break;
				case TIMESTAMP: property = D2RQ.timestampColumn; break;
				case TIME: property = D2RQ.timeColumn; break;
				case BINARY: property = D2RQ.binaryColumn; break;
				case BIT: property = D2RQ.bitColumn; break;
				case INTERVAL: property = D2RQ.intervalColumn; break;
				default: continue;
			}
			out.printProperty(property, Microsyntax.toString(column));
		}
		out.printResourceEnd();
	}

	private void printClassMap(ClassMap classMap) {
		printMapObject(classMap, D2RQ.ClassMap);
		out.printProperty(D2RQ.dataStorage, classMap.getDatabase());
		out.printRDFNodeProperties(D2RQ.class_, classMap.getClasses());
		printResourceMapProperties(classMap);
		out.printRDFNodeProperties(D2RQ.classDefinitionLabel, classMap.getDefinitionLabels());
		out.printRDFNodeProperties(D2RQ.classDefinitionComment, classMap.getDefinitionComments());
		out.printProperty(classMap.getContainsDuplicates(),
				D2RQ.containsDuplicates, classMap.getContainsDuplicates());
		out.printResourceEnd();
	}
	
	private void printPropertyBridge(PropertyBridge bridge) {
		printMapObject(bridge, D2RQ.PropertyBridge);
		out.printProperty(D2RQ.belongsToClassMap, bridge.getBelongsToClassMap());
		out.printRDFNodeProperties(D2RQ.property, bridge.getProperties());
		out.printStringProperties(D2RQ.dynamicProperty, bridge.getDynamicPropertyPatterns());
		out.printProperty(D2RQ.column, Microsyntax.toString(bridge.getColumn()));
		out.printProperty(D2RQ.pattern, bridge.getPattern());
		out.printProperty(D2RQ.sqlExpression, bridge.getSQLExpression());
		out.printProperty(D2RQ.refersToClassMap, bridge.getRefersToClassMap());
		out.printProperty(D2RQ.lang, bridge.getLang());
		out.printURIProperty(D2RQ.datatype, bridge.getDatatype());
		printResourceMapProperties(bridge);
		out.printRDFNodeProperties(D2RQ.propertyDefinitionLabel, bridge.getDefinitionLabels());
		out.printRDFNodeProperties(D2RQ.propertyDefinitionComment, bridge.getDefinitionComments());
		out.printProperty(bridge.getLimit() != LimitOp.NO_LIMIT,
				D2RQ.limit, bridge.getLimit());
		out.printProperty(bridge.getLimitInverse() != LimitOp.NO_LIMIT,
				D2RQ.limitInverse, bridge.getLimitInverse());
		out.printProperty(bridge.getOrder() != null && !bridge.getOrderDesc(), 
				D2RQ.orderAsc, bridge.getOrder());
		out.printProperty(bridge.getOrder() != null && bridge.getOrderDesc(),
				D2RQ.orderDesc, bridge.getOrder());
		out.printProperty(!bridge.getContainsDuplicates(),
				D2RQ.containsDuplicates, bridge.getContainsDuplicates());
		out.printResourceEnd();
	}
	
	private void printDownloadMap(DownloadMap map) {
		printMapObject(map, D2RQ.DownloadMap);
		out.printProperty(D2RQ.dataStorage, map.getDatabase());
		out.printProperty(D2RQ.belongsToClassMap, map.getBelongsToClassMap());
		printResourceMapProperties(map);
		out.printProperty(D2RQ.contentDownloadColumn, Microsyntax.toString(map.getContentDownloadColumn()));
		out.printProperty(D2RQ.mediaType, map.getMediaType());
		out.printResourceEnd();
	}
	
	private void printResourceMapProperties(ResourceMap map) {
		out.printProperty(D2RQ.bNodeIdColumns, map.getBNodeIdColumns());
		out.printProperty(D2RQ.uriColumn, Microsyntax.toString(map.getURIColumn()));
		out.printProperty(D2RQ.uriPattern, map.getURIPattern());
		out.printProperty(D2RQ.uriSqlExpression, map.getUriSQLExpression());
		out.printProperty(D2RQ.constantValue, map.getConstantValue());
		out.printStringProperties(D2RQ.valueRegex, map.getValueRegexes());
		out.printStringProperties(D2RQ.valueContains, map.getValueContainses());
		out.printProperty(map.getValueMaxLength() != Integer.MAX_VALUE,
				D2RQ.valueMaxLength, map.getValueMaxLength());
		for (Join join: map.getJoins()) {
			out.printProperty(D2RQ.join, Microsyntax.toString(join));
		}
		out.printStringProperties(D2RQ.condition, map.getConditions());
		for (AliasDeclaration alias: map.getAliases()) {
			out.printProperty(D2RQ.alias, Microsyntax.toString(alias));
		}
		out.printProperty(D2RQ.translateWith, map.getTranslateWith());
	}
	
	private void printTranslationTable(TranslationTable table) {
		printMapObject(table, D2RQ.TranslationTable);
		out.printURIProperty(D2RQ.href, table.getHref());
		out.printProperty(D2RQ.javaClass, table.getJavaClass());
		Iterator<Translation> it = table.getTranslations().iterator();
		List<Map<Property,RDFNode>> values = new ArrayList<Map<Property,RDFNode>>();
		while (it.hasNext()) {
			Translation translation = it.next();
			Map<Property,RDFNode> r = new LinkedHashMap<Property,RDFNode>();
			r.put(D2RQ.databaseValue, 
					ResourceFactory.createPlainLiteral(translation.dbValue()));
			r.put(D2RQ.rdfValue, 
					ResourceFactory.createPlainLiteral(translation.rdfValue()));
			values.add(r);
		}
		out.printCompactBlankNodeProperties(D2RQ.translation, values);
	}
	
	private void printMapObject(MapObject object, Resource class_) {
		if (object.getComment() != null) {
			for (String commentLine: object.getComment().split("[\r\n]+")) {
				out.printComment(commentLine);
			}
		}
		out.printResourceStart(object.resource(), class_);
	}
}
