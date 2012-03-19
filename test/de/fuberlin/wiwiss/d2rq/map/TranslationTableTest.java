package de.fuberlin.wiwiss.d2rq.map;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.d2rq.map.TranslationTable.Translation;
import de.fuberlin.wiwiss.d2rq.values.Translator;

/**
 * Tests the TranslationTable functionality
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TranslationTableTest extends TestCase {
	Resource table1 = ResourceFactory.createResource("http://test/table1");

	public void testNewTranslationTableIsEmpty() {
		TranslationTable table = new TranslationTable(table1);
		assertEquals(0, table.size());
	}
	
	public void testTranslationTableIsSizeOneAfterAddingOneTranslation() {
		TranslationTable table = new TranslationTable(table1);
		table.addTranslation("key1", "value1");
		assertEquals(1, table.size());
	}

	public void testTranslationTableTranslator() {
		TranslationTable table = new TranslationTable(table1);
		table.addTranslation("key1", "value1");
		table.addTranslation("key2", "value2");
		table.addTranslation("key3", "value3");
		Translator translator = table.translator();
		assertEquals("value1", translator.toRDFValue("key1"));
		assertEquals("value2", translator.toRDFValue("key2"));
		assertEquals("key1", translator.toDBValue("value1"));
		assertEquals("key2", translator.toDBValue("value2"));
	}

	public void testUndefinedTranslation() {
		TranslationTable table = new TranslationTable(table1);
		table.addTranslation("key1", "value1");
		Translator translator = table.translator();
		assertNull(translator.toRDFValue("unknownKey"));
		assertNull(translator.toDBValue("http://example.org/"));
	}
	
	public void testNullTranslation() {
		TranslationTable table = new TranslationTable(table1);
		table.addTranslation("key1", "value1");
		Translator translator = table.translator();
		assertNull(translator.toRDFValue(null));
		assertNull(translator.toDBValue(null));
	}
	
	public void testTranslationsWithSameValuesAreEqual() {
		Translation t1 = new Translation("foo", "bar");
		Translation t2 = new Translation("foo", "bar");
		assertEquals(t1, t2);
		assertEquals(t1.hashCode(), t2.hashCode());
	}
	
	public void testTranslationsWithDifferentValuesAreNotEqual() {
		Translation t1 = new Translation("foo", "bar");
		Translation t2 = new Translation("foo", "bar2");
		Translation t3 = new Translation("foo2", "bar");
		assertFalse(t1.equals(t2));
		assertFalse(t2.equals(t1));
		assertFalse(t1.hashCode() == t2.hashCode());
		assertFalse(t1.equals(t3));
		assertFalse(t3.equals(t1));
		assertFalse(t1.hashCode() == t3.hashCode());
	}
}