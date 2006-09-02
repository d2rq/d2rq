package de.fuberlin.wiwiss.d2rs;

import junit.framework.TestCase;

/**
 * TODO Do everything in section 14.1 of http://www.ietf.org/rfc/rfc2616.txt
 * e.g. type/*, precedence rules, whitespace, case sensitivity, numerical range of quality
 */
public class ContentNegotiatorTest extends TestCase {

	public void testNullGetsHTML() {
		assertEquals("text/html", new ContentNegotiator(null).bestFormat());
	}
	public void testAcceptEverythingGetsHTML() {
		assertEquals("text/html", new ContentNegotiator("*/*").bestFormat());
	}
	
	public void testAcceptHTMLGetsHTML() {
		assertEquals("text/html", new ContentNegotiator("text/html").bestFormat());
		assertEquals("text/html", new ContentNegotiator("application/xhtml+xml").bestFormat());
	}
	
	public void testAcceptRDFGetsRDF() {
		assertEquals("application/rdf+xml", new ContentNegotiator("application/rdf+xml").bestFormat());
		assertEquals("application/rdf+xml", new ContentNegotiator("text/rdf").bestFormat());
	}

	public void testIgnoreParameters() {
		assertEquals("application/rdf+xml", new ContentNegotiator("application/rdf+xml;q=0.9").bestFormat());
		assertEquals("application/rdf+xml", new ContentNegotiator("application/rdf+xml;foo=bar").bestFormat());
	}
	
	public void testIgnoreOtherTypes() {
		assertEquals("text/html",
				new ContentNegotiator("application/x-foo,text/html,application/x-bar").bestFormat());
		assertEquals("application/rdf+xml",
				new ContentNegotiator("text/x-foo,application/rdf+xml,text/x-bar").bestFormat());
	}
	
	public void testPreferHTML() {
		assertEquals("text/html",
				new ContentNegotiator("application/rdf+xml,text/html").bestFormat());
		assertEquals("text/html",
				new ContentNegotiator("text/html,application/rdf+xml").bestFormat());
		assertEquals("text/html",
				new ContentNegotiator("application/rdf+xml;q=0.5,text/html;q=0.5").bestFormat());
		assertEquals("text/html",
				new ContentNegotiator("text/html;q=0.5,application/rdf+xml;q=0.5").bestFormat());
	}
	
	public void testPreferHigherQuality() {
		assertEquals("application/rdf+xml",
				new ContentNegotiator("application/rdf+xml,text/html;q=0.9").bestFormat());
		assertEquals("application/rdf+xml",
				new ContentNegotiator("text/html;q=0.9,application/rdf+xml").bestFormat());
		assertEquals("application/rdf+xml",
				new ContentNegotiator("application/rdf+xml;q=0.6,text/html;q=0.5").bestFormat());
		assertEquals("application/rdf+xml",
				new ContentNegotiator("text/html;q=0.5,application/rdf+xml;q=0.6").bestFormat());
	}
	
	public void testWhenInDoubtPreferHTML() {
		assertEquals("text/html", new ContentNegotiator("no credit cards").bestFormat());
	}
}
