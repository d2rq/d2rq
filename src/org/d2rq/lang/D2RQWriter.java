package org.d2rq.lang;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.d2rq.db.op.LimitOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.lang.TranslationTable.Translation;
import org.d2rq.pp.PrettyPrinter;
import org.d2rq.vocab.D2RQ;
import org.d2rq.vocab.JDBC;

import com.hp.hpl.jena.rdf.model.Literal;
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
public class D2RQWriter {
	private final Mapping mapping;
	private final PrefixMapping prefixes = new PrefixMappingImpl();
	private PrintWriter out;
	
	public D2RQWriter(Mapping mapping) {
		this.mapping = mapping;
		this.prefixes.setNsPrefixes(mapping.getPrefixes());
	}
	
	public void write(OutputStream outStream) {
		try {
			write(new OutputStreamWriter(outStream, "utf-8"));
		} catch (UnsupportedEncodingException ex) {
			// can't happen, UTF-8 always supported
		}
	}
	
	public void write(Writer outWriter) {
		out = new PrintWriter(outWriter);
		if (!mapping.getPrefixes().getNsPrefixMap().containsValue(D2RQ.NS)) {
			prefixes.setNsPrefix("d2rq", D2RQ.NS);
		}
		writePrefixes(prefixes);
		writeConfiguration(mapping.configuration());
		for (Database database: mapping.databases()) {
			writeDatabase(database);
		}
		out.println();
		for (Resource r: mapping.classMapResources()) {
			ClassMap classMap = mapping.classMap(r);
			writeClassMap(classMap);
			for (PropertyBridge bridge: classMap.propertyBridges()) {
				writePropertyBridge(bridge);
			}
			out.println();
		}
		for (Resource r: mapping.downloadMapResources()) {
			DownloadMap downloadMap = mapping.downloadMap(r);
			writeDownloadMap(downloadMap);
		}
		out.println();
		for (Resource r: mapping.translationTableResources()) {
			TranslationTable translationTable = mapping.translationTable(r);
			writeTranslationTable(translationTable);
		}
		out.flush();
	}
	
	private void writePrefixes(PrefixMapping prefixes) {
		List<String> p = new ArrayList<String>(prefixes.getNsPrefixMap().keySet());
		Collections.sort(p);
		for (String prefix: p) {
			writePrefix(prefix, prefixes.getNsPrefixURI(prefix));
		}
		out.println();
	}
	
	private void writePrefix(String prefix, String uri) {
		out.println("@prefix " + prefix + ": <" + uri + ">.");
	}
	
	private void writeConfiguration(Configuration configuration) {
		if (configuration.getServeVocabulary() == Configuration.DEFAULT_SERVE_VOCABULARY
				&& configuration.getUseAllOptimizations() == Configuration.DEFAULT_USE_ALL_OPTIMIZATIONS) {
			return;
		}
		writeMapObject(configuration, D2RQ.Configuration);
		writeProperty(
				configuration.getServeVocabulary() != Configuration.DEFAULT_SERVE_VOCABULARY,
				D2RQ.serveVocabulary, 
				configuration.getServeVocabulary());
		writeProperty(
				configuration.getUseAllOptimizations() != Configuration.DEFAULT_USE_ALL_OPTIMIZATIONS,
				D2RQ.useAllOptimizations,
				configuration.getUseAllOptimizations());
		writeResourceEnd();
	}

	private void writeDatabase(Database db) {
		writeMapObject(db, D2RQ.Database);
		writeProperty(D2RQ.jdbcURL, db.getJdbcURL());
		writeProperty(D2RQ.jdbcDriver, db.getJDBCDriver());
		writeProperty(D2RQ.username, db.getUsername());
		writeProperty(D2RQ.password, db.getPassword());
		for (String jdbcProperty: db.getConnectionProperties().stringPropertyNames()) {
			writeProperty(
					JDBC.getProperty(jdbcProperty), 
					db.getConnectionProperties().getProperty(jdbcProperty));
		}
		writeProperty(db.getResultSizeLimit() != Database.NO_LIMIT,
				D2RQ.resultSizeLimit, db.getResultSizeLimit());
		writeProperty(db.getFetchSize() != Database.NO_FETCH_SIZE, 
				D2RQ.fetchSize, db.getFetchSize());
		writeURIProperty(D2RQ.startupSQLScript, db.getStartupSQLScript());
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
			writeProperty(property, Microsyntax.toString(column));
		}
		writeResourceEnd();
	}

	private void writeClassMap(ClassMap classMap) {
		writeMapObject(classMap, D2RQ.ClassMap);
		writeProperty(D2RQ.dataStorage, classMap.getDatabase());
		writeRDFNodeProperties(D2RQ.class_, classMap.getClasses());
		writeResourceMapProperties(classMap);
		writeRDFNodeProperties(D2RQ.classDefinitionLabel, classMap.getDefinitionLabels());
		writeRDFNodeProperties(D2RQ.classDefinitionComment, classMap.getDefinitionComments());
		writeProperty(classMap.getContainsDuplicates(),
				D2RQ.containsDuplicates, classMap.getContainsDuplicates());
		writeResourceEnd();
	}
	
	private void writePropertyBridge(PropertyBridge bridge) {
		writeMapObject(bridge, D2RQ.PropertyBridge);
		writeProperty(D2RQ.belongsToClassMap, bridge.getBelongsToClassMap());
		writeRDFNodeProperties(D2RQ.property, bridge.getProperties());
		writeStringProperties(D2RQ.dynamicProperty, bridge.getDynamicPropertyPatterns());
		writeProperty(D2RQ.column, Microsyntax.toString(bridge.getColumn()));
		writeProperty(D2RQ.pattern, bridge.getPattern());
		writeProperty(D2RQ.sqlExpression, bridge.getSQLExpression());
		writeProperty(D2RQ.refersToClassMap, bridge.getRefersToClassMap());
		writeProperty(D2RQ.lang, bridge.getLang());
		writeURIProperty(D2RQ.datatype, bridge.getDatatype());
		writeResourceMapProperties(bridge);
		writeRDFNodeProperties(D2RQ.propertyDefinitionLabel, bridge.getDefinitionLabels());
		writeRDFNodeProperties(D2RQ.propertyDefinitionComment, bridge.getDefinitionComments());
		writeProperty(bridge.getLimit() != LimitOp.NO_LIMIT,
				D2RQ.limit, bridge.getLimit());
		writeProperty(bridge.getLimitInverse() != LimitOp.NO_LIMIT,
				D2RQ.limitInverse, bridge.getLimitInverse());
		writeProperty(bridge.getOrder() != null && !bridge.getOrderDesc(), 
				D2RQ.orderAsc, bridge.getOrder());
		writeProperty(bridge.getOrder() != null && bridge.getOrderDesc(),
				D2RQ.orderDesc, bridge.getOrder());
		writeProperty(!bridge.getContainsDuplicates(),
				D2RQ.containsDuplicates, bridge.getContainsDuplicates());
		writeResourceEnd();
	}
	
	private void writeDownloadMap(DownloadMap map) {
		writeMapObject(map, D2RQ.DownloadMap);
		writeProperty(D2RQ.dataStorage, map.getDatabase());
		writeProperty(D2RQ.belongsToClassMap, map.getBelongsToClassMap());
		writeResourceMapProperties(map);
		writeProperty(D2RQ.contentDownloadColumn, Microsyntax.toString(map.getContentDownloadColumn()));
		writeProperty(D2RQ.mediaType, map.getMediaType());
		writeResourceEnd();
	}
	
	private void writeResourceMapProperties(ResourceMap map) {
		writeProperty(D2RQ.bNodeIdColumns, map.getBNodeIdColumns());
		writeProperty(D2RQ.uriColumn, Microsyntax.toString(map.getURIColumn()));
		writeProperty(D2RQ.uriPattern, map.getURIPattern());
		writeProperty(D2RQ.uriSqlExpression, map.getUriSQLExpression());
		writeProperty(D2RQ.constantValue, map.getConstantValue());
		writeStringProperties(D2RQ.valueRegex, map.getValueRegexes());
		writeStringProperties(D2RQ.valueContains, map.getValueContainses());
		writeProperty(map.getValueMaxLength() != Integer.MAX_VALUE,
				D2RQ.valueMaxLength, map.getValueMaxLength());
		for (Join join: map.getJoins()) {
			writeProperty(D2RQ.join, Microsyntax.toString(join));
		}
		writeStringProperties(D2RQ.condition, map.getConditions());
		for (AliasDeclaration alias: map.getAliases()) {
			writeProperty(D2RQ.alias, Microsyntax.toString(alias));
		}
		writeProperty(D2RQ.translateWith, map.getTranslateWith());
	}
	
	private void writeTranslationTable(TranslationTable table) {
		writeMapObject(table, D2RQ.TranslationTable);
		writeURIProperty(D2RQ.href, table.getHref());
		writeProperty(D2RQ.javaClass, table.getJavaClass());
		Iterator<Translation> it = table.getTranslations().iterator();
		if (it.hasNext()) {
			out.print("    ");
			out.println(toTurtle(D2RQ.translation));
			while (it.hasNext()) {
				Translation translation = it.next();
				out.print("        [ ");
				out.print(toTurtle(D2RQ.databaseValue));
				out.print(" ");
				out.print(quote(translation.dbValue()));
				out.print("; ");
				out.print(toTurtle(D2RQ.rdfValue));
				out.println(quote(translation.rdfValue()));
				out.print(" ]");
				out.println(it.hasNext() ? "," : ";");
			}
		}
	}
	
	private void writeMapObject(MapObject object, Resource class_) {
		if (object.getComment() != null) {
			for (String commentLine: object.getComment().split("[\r\n]+")) {
				out.print("# ");
				out.println(commentLine);
			}
		}
		writeResourceStart(object.resource(), class_);
	}

	private void writeProperty(Property property, MapObject object) {
		if (object == null) return;
		writeProperty(property, object.resource());
	}
	
	private void writeResourceStart(Resource resource, Resource class_) {
		out.print(toTurtle(resource));
		out.print(" a ");
		out.print(toTurtle(class_));
		out.println(";");
	}
	
	private void writeResourceEnd() {
		out.println("    .");
	}
	
	private void writeProperty(boolean writeIt, Property property, boolean value) {
		if (!writeIt) return;
		writePropertyTurtle(writeIt, property, value ? "true" : "false");
	}
	
	private void writeProperty(boolean writeIt, Property property, int value) {
		if (!writeIt) return;
		writePropertyTurtle(writeIt, property, Integer.toString(value));
	}
	
	private void writeProperty(boolean writeIt, Property property, String value) {
		if (!writeIt) return;
		writePropertyTurtle(writeIt, property, quote(value));
	}
	
	private void writeProperty(Property property, String value) {
		writeProperty(value != null, property, value);
	}
	
	private void writeURIProperty(Property property, String uri) {
		writePropertyTurtle(uri != null, property, "<" + uri + ">");
	}
	
	private void writeProperty(Property property, RDFNode term) {
		if (term == null) return;
		if (term.isResource()) {
			writePropertyTurtle(term != null, property, toTurtle(term.asResource()));
		} else {
			writePropertyTurtle(term != null, property, toTurtle(term.asLiteral()));
		}
	}
	
	private void writeRDFNodeProperties(Property property, Collection<? extends RDFNode> terms) {
		for (RDFNode term: terms) {
			writeProperty(property, term);
		}
	}
	
	private void writeStringProperties(Property property, Collection<String> values) {
		for (String value: values) {
			writeProperty(property, value);
		}
	}
	
	private void writePropertyTurtle(boolean writeIt, Property property, String turtleSnippet) {
		if (!writeIt) return;
		out.print("    ");
		out.print(toTurtle(property));
		out.print(" ");
		out.print(turtleSnippet);
		out.println(";");
	}
	
	private String quote(String s) {
		if (s.contains("\n") || s.contains("\r")) {
			return quoteLong(s);
		}
		return "\"" + s.replaceAll("\"", "\\\"") + "\"";
	}
	
	private String quoteLong(String s) {
		return "\"\"\"" + s.replaceAll("\"", "\\\"") + "\"\"\"";
	}
	
	private String toTurtle(Resource r) {
		return PrettyPrinter.toString(r.asNode(), prefixes);
	}
	
	private String toTurtle(Literal l) {
		StringBuffer result = new StringBuffer(quote(l.getLexicalForm()));
		if (!"".equals(l.getLanguage())) {
			result.append("@");
			result.append(l.getLanguage());
		} else if (l.getDatatype() != null) {
			result.append("^^");
			result.append(toTurtle(ResourceFactory.createResource(l.getDatatypeURI())));
		}
		return result.toString();
	}
}
