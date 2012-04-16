package de.fuberlin.wiwiss.d2rq.parser;

import java.util.Collections;
import java.util.HashSet;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.helpers.MappingHelper;
import de.fuberlin.wiwiss.d2rq.map.DownloadMap;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.SQL;
import de.fuberlin.wiwiss.d2rq.values.Translator;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * Unit tests for {@link MapParser}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
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
		Mapping mapping = MappingHelper.readFromTestFile("parser/alias.ttl");
		MappingHelper.connectToDummyDBs(mapping);
		assertEquals(1, mapping.compiledPropertyBridges().size());
		TripleRelation bridge = (TripleRelation) mapping.compiledPropertyBridges().iterator().next();
		assertTrue(bridge.baseRelation().condition().isTrue());
		AliasMap aliases = bridge.baseRelation().aliases();
		AliasMap expected = new AliasMap(Collections.singleton(SQL.parseAlias("People AS Bosses")));
		assertEquals(expected, aliases);
	}
	
	public void testParseResourceInsteadOfLiteral() {
		try {
			MappingHelper.readFromTestFile("parser/resource-instead-of-literal.ttl");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.MAPPING_RESOURCE_INSTEADOF_LITERAL, ex.errorCode());
		}
	}
	
	public void testParseLiteralInsteadOfResource() {
		try {
			MappingHelper.readFromTestFile("parser/literal-instead-of-resource.ttl");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.MAPPING_LITERAL_INSTEADOF_RESOURCE, ex.errorCode());
		}
	}

	public void testTranslationTableRDFValueCanBeLiteral() {
		Mapping m = MappingHelper.readFromTestFile("parser/translation-table.ttl");
		TranslationTable tt = m.translationTable(ResourceFactory.createResource("http://example.org/tt"));
		assertEquals("http://example.org/foo", tt.translator().toRDFValue("literal"));
	}
	
	public void testTranslationTableRDFValueCanBeURI() {
		Mapping m = MappingHelper.readFromTestFile("parser/translation-table.ttl");
		TranslationTable tt = m.translationTable(ResourceFactory.createResource("http://example.org/tt"));
		assertEquals("http://example.org/foo", tt.translator().toRDFValue("uri"));
	}
	
	public void testTypeConflictClassMapAndBridgeIsDetected() {
		try {
			MappingHelper.readFromTestFile("parser/type-classmap-and-propertybridge.ttl");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.MAPPING_TYPECONFLICT, ex.errorCode());
		}
	}
	
	public void testGenerateDownloadMap() {
		Mapping m = MappingHelper.readFromTestFile("parser/download-map.ttl");
		MappingHelper.connectToDummyDBs(m);
		Resource name = ResourceFactory.createResource("http://example.org/dm");
		assertTrue(m.downloadMapResources().contains(name));
		DownloadMap d = m.downloadMap(name);
		assertNotNull(d);
		assertEquals("image/png", 
				d.getMediaTypeValueMaker().makeValue(
						new ResultRow() {public String get(ProjectionSpec column) {return null;}}));
		assertEquals("People.pic", d.getContentDownloadColumn().qualifiedName());
		assertEquals("URI(Pattern(http://example.org/downloads/@@People.ID@@))", 
				d.nodeMaker().toString());
		assertEquals(
				new HashSet<ProjectionSpec>() {{ 
					add(SQL.parseAttribute("People.ID"));
					add(SQL.parseAttribute("People.pic"));
				}}, 
				d.getRelation().projections());
		assertTrue(d.getRelation().isUnique());
		assertTrue(d.getRelation().condition().isTrue());
		assertTrue(d.getRelation().joinConditions().isEmpty());
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
