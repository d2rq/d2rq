/*
 * $Id: RDQLTestFramework.java,v 1.2 2005/03/07 10:08:48 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.functional_tests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestResult;

import com.hp.hpl.jena.rdql.Query;
import com.hp.hpl.jena.rdql.QueryEngine;
import com.hp.hpl.jena.rdql.QueryResults;
import com.hp.hpl.jena.rdql.ResultBinding;
import com.hp.hpl.jena.rdql.ResultBinding.ResultBindingIterator;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.InfoD2RQ;
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
		super.setUp();
		this.model = new ModelD2RQ(D2RQMap);
//		this.model.enableDebug();
	}

	public static boolean compareQueryHandlers=true;
	
	public void runTest() throws Throwable {
		if (!compareQueryHandlers) {
			super.run();
			return;
		}		
		boolean oldState=GraphD2RQ.isUsingD2RQQueryHandler();
		try {
			InfoD2RQ startInst, firstInst, simpleHandler, d2rqHandler;
			startInst=InfoD2RQ.instance();
			
			GraphD2RQ.setUsingD2RQQueryHandler(false);
			super.runTest();
			Set first=results;
			firstInst=InfoD2RQ.instance();
			simpleHandler=firstInst.minus(startInst);
				
			GraphD2RQ.setUsingD2RQQueryHandler(true);
			super.runTest();
			Set second=results;
			d2rqHandler=InfoD2RQ.instanceMinus(firstInst);
			
			Set firstMap, secondMap;
			firstMap=resultBindingsToMaps(first);
			secondMap=resultBindingsToMaps(second);
			assertEquals(firstMap,secondMap);
			System.out.println("D2RQQueryHandler vs. SimpleQueryHandler = " + 
					d2rqHandler.sqlPerformanceString() + " : " + simpleHandler.sqlPerformanceString() +
					" (duration - SQL queries/rows/fields)");
//			if (firstMap.equals(secondMap)) {
//				System.out.println("RDQL same : D2RQQueryHandler returns same results as SimpleQueryHandler");
//			} else {
//				System.out.println("RDQL different : D2RQQueryHandler returns different results than SimpleQueryHandler");
//				System.out.println("SimpleQueryHandler-Result:");	
//				System.out.println(firstMap.toString());					
//				System.out.println("D2RQQueryHandler-Result:");
//				System.out.println(secondMap.toString());	
//			}
		} catch (Exception e) {
			GraphD2RQ.setUsingD2RQQueryHandler(oldState);
			System.out.println(e.toString());
		}
		GraphD2RQ.setUsingD2RQQueryHandler(oldState);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
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
	
	public static Set resultBindingsToMaps(Set b) {
		Set result=new HashSet();
		Iterator it=b.iterator();
		while (it.hasNext()) {
			ResultBinding bind=(ResultBinding)it.next();
			result.add(resultBindingToMap(bind));
		}
		return result;
	}
	
	public static Map resultBindingToMap(ResultBinding b) {
		Map m=new HashMap();
		ResultBindingIterator it=b.iterator();
		while (it.hasNext()) {
			it.next();
			String var=it.varName();
			Object val=it.value();
			m.put(var,val.toString());
		}
		return m;
	}
	
// not finished. Better do it in the Map Domain.
//	protected static void compareBindings(Set b1,Set b2) {
//		for (int i=0; i<2; i++) {
//			Set source,target;
//			if (i==0) {
//				source=b1;
//				target=b2;
//			} else {
//				source=b2;
//				target=b1;
//			}
//			Iterator s,t;
//			s=source.iterator();
//			while (s.hasNext()) {
//				ResultBinding sourceBinding=(ResultBinding) s.next();
//				ResultBindingIterator it=sourceBinding.iterator();
//				while (it.hasNext()) {
//					it.next();
//					String var=it.varName();
//					Object val=it.value();
//				}
//			}
//		}
//	}

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
