package de.fuberlin.wiwiss.d2rq.csv;

import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable.Translation;

/**
 * Unit tests for {@link TranslationTableParser}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TranslationTableParserTest.java,v 1.2 2006/09/16 13:22:29 cyganiak Exp $
 */
public class TranslationTableParserTest extends TestCase {
	private Collection simpleTranslations;
	
	public void setUp() {
		this.simpleTranslations = new HashSet();
		this.simpleTranslations.add(new Translation("db1", "rdf1"));
		this.simpleTranslations.add(new Translation("db2", "rdf2"));
	}
	
	public void testEmpty() {
		Collection translations = new TranslationTableParser(
				new StringReader("")).parseTranslations();
		assertTrue(translations.isEmpty());
	}
	
	public void testSimple() {
		String csv = "key,value";
		Collection translations = new TranslationTableParser(
				new StringReader(csv)).parseTranslations();
		assertEquals(1, translations.size());
		Translation t = (Translation) translations.iterator().next();
		assertEquals("key", t.dbValue());
		assertEquals("value", t.rdfValue());
	}
	
	public void testTwoRows() {
		String csv = "db1,rdf1\ndb2,rdf2";
		Collection translations = new TranslationTableParser(
				new StringReader(csv)).parseTranslations();
		assertEquals(2, translations.size());
		assertEquals(this.simpleTranslations, new HashSet(translations));
	}
	
	public void testParseFromFile() {
		Collection translations = new TranslationTableParser(
				D2RQTestSuite.DIRECTORY + "csv/translationtable.csv").parseTranslations();
		assertEquals(this.simpleTranslations, new HashSet(translations));
	}
	
	public void testParseFromFileWithProtocol() {
		Collection translations = new TranslationTableParser(
				D2RQTestSuite.DIRECTORY_URL + "csv/translationtable.csv").parseTranslations();
		assertEquals(this.simpleTranslations, new HashSet(translations));
	}
}
