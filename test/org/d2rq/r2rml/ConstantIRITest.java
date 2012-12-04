package org.d2rq.r2rml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.d2rq.r2rml.ConstantIRI;
import org.d2rq.r2rml.Mapping;
import org.d2rq.r2rml.MappingValidator;
import org.d2rq.validation.Report;
import org.d2rq.validation.Message.Problem;
import org.junit.Before;
import org.junit.Test;


public class ConstantIRITest {
	private MappingValidator validator;
	private Report report;
	
	@Before public void setUp() {
		validator = new MappingValidator(new Mapping("http://example.com/"));
		report = validator.getReport();
		report.setIgnoreInfo(true);
	}
	
	@Test public void okIRI() {
		ConstantIRI.create("http://example.com/test.html").accept(validator);
		assertFalse(report.hasError());
	}
	
	@Test public void relativeIRI() {
		ConstantIRI.create("test.html").accept(validator);
		assertTrue(report.hasError());
		assertEquals(1, report.getMessages().size());
		assertEquals(Problem.IRI_NOT_ABSOLUTE, report.getMessages().get(0).getProblem());
		assertEquals("test.html", report.getMessages().get(0).getValue());
	}
	
	@Test public void space() {
		String iri = "http://example.com/test html";
		ConstantIRI.create(iri).accept(validator);
		assertEquals(1, report.getMessages().size());
		assertEquals(Problem.INVALID_IRI, report.getMessages().get(0).getProblem());
		assertEquals(iri, report.getMessages().get(0).getValue());
		assertEquals("WHITESPACE", report.getMessages().get(0).getDetailCode());
	}
	
	@Test public void pointyBracket() {
		String iri = "http://example.com/?q=sadf<asdf>";
		ConstantIRI.create(iri).accept(validator);
		assertEquals(1, report.getMessages().size());
		assertEquals(Problem.INVALID_IRI, report.getMessages().get(0).getProblem());
		assertEquals(iri, report.getMessages().get(0).getValue());
		assertEquals("UNWISE_CHARACTER", report.getMessages().get(0).getDetailCode());
	}
	
	@Test public void specialCharInHost() {
		String iri = "http://exam:ple.com/";
		ConstantIRI.create(iri).accept(validator);
		assertEquals(1, report.getMessages().size());
		assertEquals(Problem.INVALID_IRI, report.getMessages().get(0).getProblem());
		assertEquals(iri, report.getMessages().get(0).getValue());
		assertEquals("ILLEGAL_CHARACTER", report.getMessages().get(0).getDetailCode());
	}
	
	@Test public void brokenPercentEncoding() {
		String iri = "http://example.com/%X";
		ConstantIRI.create(iri).accept(validator);
		assertEquals(1, report.getMessages().size());
		assertEquals(Problem.INVALID_IRI, report.getMessages().get(0).getProblem());
		assertEquals(iri, report.getMessages().get(0).getValue());
		assertEquals("ILLEGAL_PERCENT_ENCODING", report.getMessages().get(0).getDetailCode());
	}
}
