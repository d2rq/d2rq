/*
 * $Id: RDQLTestFramework.java,v 1.1 2005/03/02 13:03:48 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.functional_tests;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
public class RDQLTestFramework extends TestFramework {
	protected ModelD2RQ model;
	protected Set results;

	public RDQLTestFramework(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		this.model = new ModelD2RQ(D2RQMap);
//		this.model.enableDebug();
	}

	protected void tearDown() throws Exception {
		this.model.close();
		this.results = null;
	}

	protected void rdql(String rdql) {
		this.results = new HashSet();
		Query query = new Query(rdql);
		query.setSource(this.model);
		QueryResults qr = new QueryEngine(query).exec();
		while (qr.hasNext()) {
			ResultBinding binding = (ResultBinding) qr.next();
			this.results.add(binding);
		}
	}
	
	protected void assertResultCount(int count) {
		assertEquals(count, this.results.size());
	}

	protected void assertResult(Map map) {
		Iterator it = this.results.iterator();
		while (it.hasNext()) {
			ResultBinding binding = (ResultBinding) it.next();
			if (containsAll(binding, map)) {
				return;
			}
		}
		fail();
	}

	protected boolean containsAll(ResultBinding actual, Map expected) {
		Iterator it = expected.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			String actualResource = actual.get((String) entry.getKey()).toString();
			String expectedResource = entry.getValue().toString();
			if (!actualResource.equals(expectedResource)) {
				return false;
			}
		}
		return true;
	}
	
	protected void dump() {
		Iterator it = this.results.iterator();
		int count = 1;
		while (it.hasNext()) {
			System.out.println("Result binding " + count + ":");
			ResultBinding binding = (ResultBinding) it.next();
			ResultBindingIterator it2 = binding.iterator();
			while (it2.hasNext()) {
				it2.next();
				System.out.println("    " + it2.varName() + " => " + it2.value());
			}
			count++;
		}
	}
}
