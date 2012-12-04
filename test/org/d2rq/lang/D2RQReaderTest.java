package org.d2rq.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.d2rq.D2RQException;
import org.d2rq.D2RQTestSuite;
import org.d2rq.db.schema.TableName;
import org.d2rq.lang.AliasDeclaration;
import org.d2rq.lang.D2RQReader;
import org.d2rq.lang.DownloadMap;
import org.d2rq.lang.Mapping;
import org.d2rq.lang.Microsyntax;
import org.d2rq.lang.TranslationTable;
import org.d2rq.values.Translator;
import org.d2rq.vocab.D2RQ;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


/**
 * Unit tests for {@link D2RQReader}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class D2RQReaderTest {
	private final static String TABLE_URI = "http://example.org/map#table1";
	
	private Model model;
	
	@Before
	public void setUp() {
		model = ModelFactory.createDefaultModel();
	}

	@Test
	public void testAbsoluteHTTPURIIsNotChanged() {
		assertEquals("http://example.org/foo", D2RQReader.absolutizeURI("http://example.org/foo"));
	}
	
	@Test
	public void testAbsoluteFileURIIsNotChanged() {
		assertEquals("file://C:/autoexec.bat", D2RQReader.absolutizeURI("file://C:/autoexec.bat"));
	}
	
	@Test
	public void testRelativeFileURIIsAbsolutized() {
		String uri = D2RQReader.absolutizeURI("foo/bar");
		assertTrue(uri.startsWith("file://"));
		assertTrue(uri.endsWith("/foo/bar"));
	}
	
	@Test
	public void testRootlessFileURIIsAbsolutized() {
		String uri = D2RQReader.absolutizeURI("file:foo/bar");
		assertTrue(uri.startsWith("file://"));
		assertTrue(uri.endsWith("/foo/bar"));
	}	@Test
	public void testEmptyTranslationTable() {
		Resource r = addTranslationTableResource();
		Mapping mapping = new D2RQReader(this.model, null).getMapping();
		TranslationTable table = mapping.translationTable(r);
		assertNotNull(table);
		assertEquals(0, table.size());
	}

	@Test
	public void testGetSameTranslationTable() {
		Resource r = addTranslationTableResource();
		addTranslationResource(r, "foo", "bar");
		Mapping mapping = new D2RQReader(this.model, null).getMapping();
		TranslationTable table1 = mapping.translationTable(r);
		TranslationTable table2 = mapping.translationTable(r);
		assertSame(table1, table2);
	}
	
	@Test
	public void testParseTranslationTable() {
		Resource r = addTranslationTableResource();
		addTranslationResource(r, "foo", "bar");
		Mapping mapping = new D2RQReader(this.model, null).getMapping();
		TranslationTable table = mapping.translationTable(r);
		assertEquals(1, table.size());
		Translator translator = table.translator();
		assertEquals("bar", translator.toRDFValue("foo"));
	}

	@Test
	public void testParseAlias() {
		Set<AliasDeclaration> aliases = D2RQTestSuite
				.loadMapping("lang/alias.ttl")
				.classMap(ResourceFactory.createResource("http://example.org/classmap"))
				.propertyBridges().iterator().next().getAliases();
		assertEquals(1, aliases.size());
		AliasDeclaration alias = aliases.iterator().next();
		assertEquals(TableName.parse("\"People\""), alias.getOriginal());
		assertEquals(TableName.parse("\"Bosses\""), alias.getAlias());
	}
	
	@Test
	public void testParseResourceInsteadOfLiteral() {
		try {
			D2RQTestSuite.loadMapping("lang/resource-instead-of-literal.ttl");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.MAPPING_RESOURCE_INSTEADOF_LITERAL, ex.errorCode());
		}
	}
	
	@Test
	public void testParseLiteralInsteadOfResource() {
		try {
			D2RQTestSuite.loadMapping("lang/literal-instead-of-resource.ttl");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.MAPPING_LITERAL_INSTEADOF_RESOURCE, ex.errorCode());
		}
	}

	@Test
	public void testTranslationTableRDFValueCanBeLiteral() {
		Mapping m = D2RQTestSuite.loadMapping("lang/translation-table.ttl");
		TranslationTable tt = m.translationTable(ResourceFactory.createResource("http://example.org/tt"));
		assertEquals("http://example.org/foo", tt.translator().toRDFValue("literal"));
	}
	
	@Test
	public void testTranslationTableRDFValueCanBeURI() {
		Mapping m = D2RQTestSuite.loadMapping("lang/translation-table.ttl");
		TranslationTable tt = m.translationTable(ResourceFactory.createResource("http://example.org/tt"));
		assertEquals("http://example.org/foo", tt.translator().toRDFValue("uri"));
	}
	
	@Test
	public void testTypeConflictClassMapAndBridgeIsDetected() {
		try {
			D2RQTestSuite.loadMapping("lang/type-classmap-and-propertybridge.ttl");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.MAPPING_TYPECONFLICT, ex.errorCode());
		}
	}
	
	@Test
	public void testGenerateDownloadMap() {
		Mapping m = D2RQTestSuite.loadMapping("lang/download-map.ttl");
		Resource name = ResourceFactory.createResource("http://example.org/dm");
		assertTrue(m.downloadMapResources().contains(name));
		DownloadMap dl = m.downloadMap(name);
		assertEquals("http://example.org/database", dl.getDatabase().resource().getURI());
		assertEquals("http://example.org/downloads/@@People.ID@@", dl.getURIPattern());
		assertEquals(Microsyntax.parseColumn("People.pic"), dl.getContentDownloadColumn());
		assertEquals("image/png", dl.getMediaType());
	}
	
	private Resource addTranslationTableResource() {
		return model.createResource(TABLE_URI, D2RQ.TranslationTable);
	}
	
	private Resource addTranslationResource(Resource table, String dbValue, String rdfValue) {
		Resource translation = model.createResource();
		translation.addProperty(D2RQ.databaseValue, dbValue);
		translation.addProperty(D2RQ.rdfValue, rdfValue);
		table.addProperty(D2RQ.translation, translation);
		return translation;
	}
}
