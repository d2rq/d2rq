package org.d2rq.r2rml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.d2rq.db.DummyDB;
import org.junit.Test;


public class StringTemplateTest {

	@Test public void testSimpleTemplate() {
		StringTemplate template = StringTemplate.create("person/{ID}");
		assertEquals("person/{ID}", template.toString());
		assertTrue(template.isValid());
		assertEquals(2, template.getLiteralParts().length);
		assertEquals(1, template.getColumnNames().length);
		assertEquals("person/", template.getLiteralParts()[0]);
		assertEquals("", template.getLiteralParts()[1]);
		assertEquals("ID", template.getColumnNames()[0]);
	}
	
	@Test public void testUnescapedBackslash() {
		assertFalse(StringTemplate.create("foo\\bar").isValid());
	}
	
	@Test public void testUnmatchedOpenBrace() {
		assertFalse(StringTemplate.create("foo{bar").isValid());
	}
	
	@Test public void testUnmatchedCloseBrace() {
		assertFalse(StringTemplate.create("foo}bar").isValid());
	}
	
	@Test public void testDoubleOpenBrace() {
		assertFalse(StringTemplate.create("foo{bar{baz").isValid());
	}
	
	@Test public void testEscapedOpenBraces() {
		assertTrue(StringTemplate.create("foo\\{bar\\{baz").isValid());
	}
	
	@Test public void testEscapedBackslash() {
		assertFalse(StringTemplate.create("foo\\\\{bar").isValid());
	}
	
	@Test public void testComplexBackslashEscape() {
		assertTrue(StringTemplate.create("foo\\\\\\\\\\{bar").isValid());
	}
	
	@Test public void testInvalidColumnName() {
		assertFalse(StringTemplate.create("foo{a b}bar").isValid(new DummyDB()));
	}
	
	@Test public void testQualifiedColumnName() {
		assertFalse(StringTemplate.create("foo{a.b}bar").isValid(new DummyDB()));
	}
}
