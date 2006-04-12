/*
 * $Id: ParserTest.java,v 1.1 2006/04/12 09:56:16 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.map.D2RQ;
import de.fuberlin.wiwiss.d2rq.map.MapParser;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;

import junit.framework.TestCase;

/**
 * Unit tests for {@link MapParser}
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class ParserTest extends TestCase {
	private final static String TABLE_URI = "http://example.org/map#table1";

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
		TranslationTable table = parser.getTranslationTable(r.getNode());
		assertNotNull(table);
		assertEquals(0, table.size());
	}

	public void testGetSameTranslationTable() {
		Resource r = createTranslationTableResource();
		addTranslationResource(r, this.model.createLiteral("foo"), this.model.createLiteral("bar"));
		MapParser parser = new MapParser(this.model);
		TranslationTable table1 = parser.getTranslationTable(r.getNode());
		TranslationTable table2 = parser.getTranslationTable(r.getNode());
		assertSame(table1, table2);
	}
	
	public void testParseTranslationTable() {
		Resource r = createTranslationTableResource();
		addTranslationResource(r, this.model.createLiteral("foo"), this.model.createLiteral("bar"));
		addTranslationResource(r, this.model.createLiteral("baz"), this.model.createResource(D2RQ.uri));
		MapParser parser = new MapParser(this.model);
		TranslationTable table = parser.getTranslationTable(r.getNode());
		assertEquals(2, table.size());
		assertEquals("bar", table.toRDFValue("foo"));
		assertEquals(D2RQ.uri, table.toRDFValue("baz"));
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
