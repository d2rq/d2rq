/*
 * $Id: TestFramework.java,v 1.1 2005/03/02 13:03:48 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.functional_tests;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestCase;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdql.Query;
import com.hp.hpl.jena.rdql.QueryEngine;
import com.hp.hpl.jena.rdql.QueryResults;
import com.hp.hpl.jena.rdql.ResultBinding;
import com.hp.hpl.jena.rdql.ResultBinding.ResultBindingIterator;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

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
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
public class TestFramework extends TestCase {
	protected static String D2RQMap = "file:doc/manual/ISWC-d2rq.n3";
	protected static String NS = "http://annotation.semanticweb.org/iswc/iswc.daml#";
	protected final static RDFDatatype xsdString =
			TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#string");
	protected final static RDFDatatype xsdYear =
			TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#gYear");
	
	
	public static String getD2RQMap() {
		return D2RQMap;
	}
	public static void setD2RQMap(String map) {
		D2RQMap = map;
	}
	public static String getNS() {
		return NS;
	}
	public static void setNS(String ns) {
		NS = ns;
	}
	
	public TestFramework(String arg0) {
		super(arg0);
	}

}
