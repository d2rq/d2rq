package de.fuberlin.wiwiss.d2rq.csv;

import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable.Translation;

/**
 * Unit tests for {@link TranslationTableParser}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TranslationTableParserTest.java,v 1.1 2006/09/12 12:06:18 cyganiak Exp $
 */
public class TranslationTableParserTest extends TestCase {

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
		String csv = "key1,value1\nkey2,value2";
		Collection translations = new TranslationTableParser(
				new StringReader(csv)).parseTranslations();
		assertEquals(2, translations.size());
		Set expected = new HashSet();
		expected.add(new Translation("key1", "value1"));
		expected.add(new Translation("key2", "value2"));
		assertEquals(expected, new HashSet(translations));
	}
}
