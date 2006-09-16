package de.fuberlin.wiwiss.d2rq.parser;

import junit.framework.TestCase;

public class URITest extends TestCase {

	public void testAbsoluteHTTPURIIsNotChanged() {
		assertEquals("http://example.org/foo", MapParser.absolutizeURI("http://example.org/foo"));
	}
	
	public void testAbsoluteFileURIIsNotChanged() {
		assertEquals("file://C:/autoexec.bat", MapParser.absolutizeURI("file://C:/autoexec.bat"));
	}
	
	public void testRelativeFileURIIsAbsolutized() {
		String uri = MapParser.absolutizeURI("foo/bar");
		assertTrue(uri.startsWith("file://"));
		assertTrue(uri.endsWith("/foo/bar"));
	}
	
	public void testRootlessFileURIIsAbsolutized() {
		String uri = MapParser.absolutizeURI("file:foo/bar");
		assertTrue(uri.startsWith("file://"));
		assertTrue(uri.endsWith("/foo/bar"));
	}
}