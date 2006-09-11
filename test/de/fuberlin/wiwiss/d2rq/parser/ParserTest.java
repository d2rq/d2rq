package de.fuberlin.wiwiss.d2rq.parser;

import java.util.Collections;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * Unit tests for {@link MapParser}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ParserTest.java,v 1.10 2006/09/11 22:29:20 cyganiak Exp $
 */
public class ParserTest extends TestCase {
	private final static String TABLE_URI = "http://example.org/map#table1";
	
	private Model model;

	protected void setUp() throws Exception {
		this.model = ModelFactory.createDefaultModel();
	}

	public void testEmptyTranslationTable() {
		Resource r = createTranslationTableResource();
		MapParser parser = new MapParser(this.model, null);
		Level save = Logger.getLogger(MapParser.class).getLevel();
		Logger.getLogger(MapParser.class).setLevel(Level.OFF);
		TranslationTable table = parser.getTranslationTable(r);
		Logger.getLogger(MapParser.class).setLevel(save);
		assertNotNull(table);
		assertEquals(0, table.size());
	}

	public void testGetSameTranslationTable() {
		Resource r = createTranslationTableResource();
		addTranslationResource(r, this.model.createLiteral("foo"), this.model.createLiteral("bar"));
		MapParser parser = new MapParser(this.model, null);
		TranslationTable table1 = parser.getTranslationTable(r);
		TranslationTable table2 = parser.getTranslationTable(r);
		assertSame(table1, table2);
	}
	
	public void testParseTranslationTable() {
		Resource r = createTranslationTableResource();
		addTranslationResource(r, this.model.createLiteral("foo"), this.model.createLiteral("bar"));
		addTranslationResource(r, this.model.createLiteral("baz"), this.model.createResource(D2RQ.NS));
		MapParser parser = new MapParser(this.model, null);
		TranslationTable table = parser.getTranslationTable(r);
		assertEquals(2, table.size());
		assertEquals("bar", table.toRDFValue("foo"));
		assertEquals(D2RQ.NS, table.toRDFValue("baz"));
	}

	public void testParseAlias() {
		Mapping mapping = parse("parser/alias.n3").parse();
		assertEquals(1, mapping.compiledPropertyBridges().size());
		RDFRelation bridge = (RDFRelation) mapping.compiledPropertyBridges().iterator().next();
		assertTrue(bridge.baseRelation().condition().isTrue());
		AliasMap aliases = bridge.baseRelation().aliases();
		AliasMap expected = AliasMap.buildFromSQL(Collections.singleton("People AS Bosses"));
		assertEquals(expected, aliases);
	}
	
	private MapParser parse(String testFileName) {
		Model m = ModelFactory.createDefaultModel();
		m.read(D2RQTestSuite.DIRECTORY + testFileName, "N3");
		MapParser result = new MapParser(m, null);
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
