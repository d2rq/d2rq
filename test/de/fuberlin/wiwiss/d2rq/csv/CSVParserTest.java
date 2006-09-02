package de.fuberlin.wiwiss.d2rq.csv;

import java.io.StringReader;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.csv.CSVParser;

import junit.framework.TestCase;

/**
 * Unit tests for {@link CSVParser}
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: CSVParserTest.java,v 1.1 2006/09/02 22:49:17 cyganiak Exp $
 */
public class CSVParserTest extends TestCase {

	public void testEmpty() {
		Map map = new CSVParser(new StringReader("")).parse();
		assertTrue(map.isEmpty());
	}
	
	public void testSimple() {
		String csv = "key,value";
		Map map = new CSVParser(new StringReader(csv)).parse();
		assertEquals(1, map.size());
		assertEquals("value", map.get("key"));
	}
	
	public void testTwoRows() {
		String csv = "key1,value1\nkey2,value2";
		Map map = new CSVParser(new StringReader(csv)).parse();
		assertEquals(2, map.size());
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
	}
}
