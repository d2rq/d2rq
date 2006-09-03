package de.fuberlin.wiwiss.d2rq;

import junit.framework.TestCase;
import junit.framework.TestResult;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;

import de.fuberlin.wiwiss.d2rq.functional_tests.AllTests;

/**
 * Functional tests that exercise a ModelD2RQ by running RDQL queries against it. 
 * For notes on running the tests, see {@link AllTests}.
 * 
 * Each test method runs one RDQL query and automatically compares the actual
 * results to the expected results. For some tests, only the number of returned triples
 * is checked. For others, the returned values are compared against expected values.
 * 
 * If a test fails, the dump() method can be handy. It shows the actual results returned
 * by a query on System.out.
 *
 * To see debug information, uncomment the enableDebug() call in the setUp() method.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TestFramework.java,v 1.4 2006/09/03 00:08:11 cyganiak Exp $
 */
public class TestFramework extends TestCase {
	protected String D2RQMap = "file:doc/manual/ISWC-d2rq.n3"; // "file:srcMac/jgISWC-d2rq.n3"; 
	protected static String NS = "http://annotation.semanticweb.org/iswc/iswc.daml#";
	protected final static RDFDatatype xsdString =
			TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#string");
	protected final static RDFDatatype xsdYear =
			TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#gYear");
	
	public static String getNS() {
		return NS;
	}
	public static void setNS(String ns) {
		NS = ns;
	}
	
	public void run(TestResult result) {
		super.run(result); // calls setUp, runTest and tearDown
	}
	protected void setUp() throws Exception {
		super.setUp();
	}
	protected void runTest() throws Throwable {
		super.runTest();
	}
	protected void tearDown() throws Exception {
		super.tearDown();
		// set Breakpoint here
	}
}
