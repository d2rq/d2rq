package de.fuberlin.wiwiss.d2rq.parser;

import java.util.Collections;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.sql.SQL;
import de.fuberlin.wiwiss.d2rq.values.Translator;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * Unit tests for {@link MapParser}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ParserTest.java,v 1.13 2006/09/15 15:31:23 cyganiak Exp $
 */
public class ParserTest extends TestCase {
	private final static String TABLE_URI = "http://example.org/map#table1";
	
	private Model model;

	protected void setUp() throws Exception {
		this.model = ModelFactory.createDefaultModel();
	}

	public void testEmptyTranslationTable() {
		Resource r = addTranslationTableResource();
		Mapping mapping = new MapParser(this.model, null).parse();
		TranslationTable table = mapping.translationTable(r);
		assertNotNull(table);
		assertEquals(0, table.size());
	}

	public void testGetSameTranslationTable() {
		Resource r = addTranslationTableResource();
		addTranslationResource(r, "foo", "bar");
		Mapping mapping = new MapParser(this.model, null).parse();
		TranslationTable table1 = mapping.translationTable(r);
		TranslationTable table2 = mapping.translationTable(r);
		assertSame(table1, table2);
	}
	
	public void testParseTranslationTable() {
		Resource r = addTranslationTableResource();
		addTranslationResource(r, "foo", "bar");
		Mapping mapping = new MapParser(this.model, null).parse();
		TranslationTable table = mapping.translationTable(r);
		assertEquals(1, table.size());
		Translator translator = table.translator();
		assertEquals("bar", translator.toRDFValue("foo"));
	}

	public void testParseAlias() {
		Mapping mapping = parse("parser/alias.n3").parse();
		assertEquals(1, mapping.compiledPropertyBridges().size());
		RDFRelation bridge = (RDFRelation) mapping.compiledPropertyBridges().iterator().next();
		assertTrue(bridge.baseRelation().condition().isTrue());
		AliasMap aliases = bridge.baseRelation().aliases();
		AliasMap expected = new AliasMap(Collections.singleton(SQL.parseAlias("People AS Bosses")));
		assertEquals(expected, aliases);
	}
	
	private MapParser parse(String testFileName) {
		Model m = ModelFactory.createDefaultModel();
		m.read(D2RQTestSuite.DIRECTORY + testFileName, "N3");
		MapParser result = new MapParser(m, null);
		result.parse();
		return result;
	}
	
	private Resource addTranslationTableResource() {
		return this.model.createResource(TABLE_URI, D2RQ.TranslationTable);
	}
	
	private Resource addTranslationResource(Resource table, String dbValue, String rdfValue) {
		Resource translation = this.model.createResource();
		translation.addProperty(D2RQ.databaseValue, dbValue);
		translation.addProperty(D2RQ.rdfValue, rdfValue);
		table.addProperty(D2RQ.translation, translation);
		return translation;
	}
}
