/*
 * $Id: ParserTest.java,v 1.2 2006/08/28 19:44:22 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq.parser;

import java.util.Collections;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.MockLogger;
import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.D2RQ;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;

/**
 * Unit tests for {@link MapParser}
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class ParserTest extends TestCase {
	private final static String TABLE_URI = "http://example.org/map#table1";
	private final static String TEST_FILES = "file:test/de/fuberlin/wiwiss/d2rq/parser/";
	
	private Model model;
	private MockLogger logger;

	/**
	 * Constructor for ParserTest.
	 * @param arg0
	 */
	public ParserTest(String arg0) {
		super(arg0);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		this.model = ModelFactory.createDefaultModel();
		this.logger = new MockLogger();
		Logger.setInstance(this.logger);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		this.logger.tally();
		Logger.setInstance(null);
	}

	public void testEmptyTranslationTable() {
		Resource r = createTranslationTableResource();
		MapParser parser = new MapParser(this.model);
		this.logger.expectWarning(null);
		TranslationTable table = parser.getTranslationTable(r.asNode());
		assertNotNull(table);
		assertEquals(0, table.size());
	}

	public void testGetSameTranslationTable() {
		Resource r = createTranslationTableResource();
		addTranslationResource(r, this.model.createLiteral("foo"), this.model.createLiteral("bar"));
		MapParser parser = new MapParser(this.model);
		TranslationTable table1 = parser.getTranslationTable(r.asNode());
		TranslationTable table2 = parser.getTranslationTable(r.asNode());
		assertSame(table1, table2);
	}
	
	public void testParseTranslationTable() {
		Resource r = createTranslationTableResource();
		addTranslationResource(r, this.model.createLiteral("foo"), this.model.createLiteral("bar"));
		addTranslationResource(r, this.model.createLiteral("baz"), this.model.createResource(D2RQ.uri));
		MapParser parser = new MapParser(this.model);
		TranslationTable table = parser.getTranslationTable(r.asNode());
		assertEquals(2, table.size());
		assertEquals("bar", table.toRDFValue("foo"));
		assertEquals(D2RQ.uri, table.toRDFValue("baz"));
	}

	public void testParseAlias() {
		MapParser parser = parse("alias.n3");
		assertEquals(1, parser.getPropertyBridges().size());
		PropertyBridge bridge = (PropertyBridge) parser.getPropertyBridges().iterator().next();
		assertTrue(bridge.getConditions().isEmpty());
		AliasMap aliases = bridge.getAliases();
		AliasMap expected = AliasMap.buildFromSQL(Collections.singleton("People AS Bosses"));
		assertEquals(expected, aliases);
	}
	
	private MapParser parse(String testFileName) {
		Model m = ModelFactory.createDefaultModel();
		m.read(TEST_FILES + testFileName, "N3");
		MapParser result = new MapParser(m);
		result.parse();
		return result;
	}
	
	private Resource createTranslationTableResource() {
		Resource result = this.model.createResource(TABLE_URI);
		result.addProperty(RDF.type, this.model.createResource(D2RQ.TranslationTable.getURI()));
		return result;
	}
	
	private void addTranslationResource(Resource table, RDFNode dbValue, RDFNode rdfValue) {
		Resource translation = this.model.createResource();
		translation.addProperty(this.model.createProperty(D2RQ.databaseValue.getURI()), dbValue);
		translation.addProperty(this.model.createProperty(D2RQ.rdfValue.getURI()), rdfValue);
		table.addProperty(this.model.createProperty(D2RQ.translation.getURI()), translation);
	}
}
