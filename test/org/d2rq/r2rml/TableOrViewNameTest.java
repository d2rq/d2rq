package org.d2rq.r2rml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.d2rq.r2rml.Mapping;
import org.d2rq.r2rml.MappingValidator;
import org.d2rq.r2rml.TableOrViewName;
import org.d2rq.validation.Report;
import org.d2rq.validation.Message.Problem;
import org.junit.Before;
import org.junit.Test;


public class TableOrViewNameTest {
	private MappingValidator validator;
	private Report report;
	
	@Before public void setUp() {
		validator = new MappingValidator(new Mapping("http://example.com/"));
		report = validator.getReport();
		report.setIgnoreInfo(true);
	}
	
	@Test public void testUnqualifiedUndelimited() {
		TableOrViewName t = TableOrViewName.create("test");
		assertTrue(t.isValid());
		assertEquals("test", t.toString());
		assertNull(t.asQualifiedTableName().getCatalog());
		assertNull(t.asQualifiedTableName().getSchema());
		assertEquals("test", t.asQualifiedTableName().getTable().toString());
		assertTrue(t.equals(t));
	}
	
	@Test public void testQualifiedUndelimited() {
		TableOrViewName t = TableOrViewName.create("foo.bar.quux");
		assertTrue(t.isValid());
		assertEquals("foo.bar.quux", t.toString());
		assertEquals("foo", t.asQualifiedTableName().getCatalog().toString());
		assertEquals("bar", t.asQualifiedTableName().getSchema().toString());
		assertEquals("quux", t.asQualifiedTableName().getTable().toString());
		assertTrue(t.equals(t));
	}
	
	@Test public void testUnqualifiedDelimited() {
		TableOrViewName t = TableOrViewName.create("\"test\"");
		assertTrue(t.isValid());
		assertEquals("\"test\"", t.toString());
		assertNull(t.asQualifiedTableName().getCatalog());
		assertNull(t.asQualifiedTableName().getSchema());
		assertEquals("\"test\"", t.asQualifiedTableName().getTable().toString());
		assertTrue(t.equals(t));
	}
	
	@Test public void testDoubleQuotes() {
		TableOrViewName t = TableOrViewName.create("\"te\"\"st\"");
		assertTrue(t.isValid());
		assertEquals("te\"st", t.asQualifiedTableName().getTable().getName());
		assertTrue(t.equals(t));
	}
	
	@Test public void testQualifiedDelimited() {
		TableOrViewName t = TableOrViewName.create("\"foo\".\"bar\".\"quux\"");
		assertTrue(t.isValid());
		assertEquals("\"foo\".\"bar\".\"quux\"", t.toString());
		assertEquals("\"foo\"", t.asQualifiedTableName().getCatalog().toString());
		assertEquals("\"bar\"", t.asQualifiedTableName().getSchema().toString());
		assertEquals("\"quux\"", t.asQualifiedTableName().getTable().toString());
		assertTrue(t.equals(t));
	}
	
	@Test public void testEmpty() {
		TableOrViewName t = TableOrViewName.create("");
		t.accept(validator);
		assertFalse(t.isValid());
		assertEquals("", t.toString());
		assertNull(t.asQualifiedTableName());
		assertEquals(Problem.MALFORMED_TABLE_OR_VIEW_NAME, report.getMessages().get(0).getProblem());
		assertEquals("UNEXPECTED_END", 
				report.getMessages().get(0).getDetailCode());
		assertEquals("", report.getMessages().get(0).getValue());
		assertTrue(t.equals(t));
	}
	
	@Test public void testTooManyIdentifiers() {
		TableOrViewName t = TableOrViewName.create("a.b.c.d");
		t.accept(validator);
		assertFalse(t.isValid());
		assertEquals("TOO_MANY_IDENTIFIERS",
				report.getMessages().get(0).getDetailCode());
	}
	
	@Test public void testIllegalCharInUndelimited() {
		TableOrViewName t = TableOrViewName.create("foo bar");
		t.accept(validator);
		assertFalse(t.isValid());
		assertEquals("UNEXPECTED_CHARACTER",
				report.getMessages().get(0).getDetailCode());
	}
	
	@Test public void testRunawayDelimited() {
		TableOrViewName t = TableOrViewName.create("\"foo");
		t.accept(validator);
		assertFalse(t.isValid());
		assertEquals("UNEXPECTED_END",
				report.getMessages().get(0).getDetailCode());
	}
	
	@Test public void testEmptyDelimited() {
		TableOrViewName t = TableOrViewName.create("foo.\"\".bar");
		t.accept(validator);
		assertFalse(t.isValid());
		assertEquals("EMPTY_DELIMITED_IDENTIFIER",
				report.getMessages().get(0).getDetailCode());
	}
	
	@Test public void testDoublePeriod() {
		TableOrViewName t = TableOrViewName.create("foo..bar");
		t.accept(validator);
		assertFalse(t.isValid());
		assertEquals("UNEXPECTED_CHARACTER",
				report.getMessages().get(0).getDetailCode());
	}
	
	@Test public void testEquals() {
		assertEquals(
				TableOrViewName.create("foo.\"BAR\""), 
				TableOrViewName.create("\"FOO\".bar"));
		assertEquals(
				TableOrViewName.create("FOO.\"BAR\""), 
				TableOrViewName.create("\"FOO\".BAR"));
		assertFalse(TableOrViewName.create("\"foo\"").equals(TableOrViewName.create("\"FOO\"")));
	}
	
	@Test public void testHashCode() {
		assertEquals(
				TableOrViewName.create("foo.\"BAR\"").hashCode(),
				TableOrViewName.create("\"FOO\".bar").hashCode());
		assertEquals(
				TableOrViewName.create("FOO.\"BAR\"").hashCode(),
				TableOrViewName.create("\"FOO\".BAR").hashCode());
		assertNotSame(
				TableOrViewName.create("\"foo\"").hashCode(),
				TableOrViewName.create("\"FOO\"").hashCode());
	}
}
