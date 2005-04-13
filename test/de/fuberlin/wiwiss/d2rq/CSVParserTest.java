/*
 * $Id: CSVParserTest.java,v 1.2 2005/04/13 16:56:08 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import java.io.StringReader;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.helpers.CSVParser;

import junit.framework.TestCase;

/**
 * Unit tests for CSVParser
 *
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class CSVParserTest extends TestCase {

	/**
	 * Constructor for CSVParserTest.
	 * @param arg0
	 */
	public CSVParserTest(String arg0) {
		super(arg0);
	}

	public void testSimple() {
		String csv = "key,value";
		Map map = new CSVParser(new StringReader(csv)).parse();
		assertEquals(1, map.size());
		assertEquals("value", map.get("key"));
	}
}
