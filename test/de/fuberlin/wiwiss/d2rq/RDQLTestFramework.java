/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

//for Jena2.1:
//import com.hp.hpl.jena.rdql.ResultBinding.ResultBindingIterator;
//for Jena2.2:
import com.hp.hpl.jena.rdql.ResultBindingIterator;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.InfoD2RQ;
import de.fuberlin.wiwiss.d2rq.Logger;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.functional_tests.AllTests;
import de.fuberlin.wiwiss.d2rq.helpers.JenaCompatibility;

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
	protected String queryString;
	
	protected static Logger separator=new Logger();
	protected static Logger logger=new Logger();
	protected static Logger performanceLogger=new Logger();
	protected static Logger rdqlLogger=new Logger();
	protected static Logger differentLogger=new Logger();
	protected static Logger differenceLogger=new Logger();
	protected static Logger rsLogger=new Logger();
	
	public RDQLTestFramework(String arg0) {
		super(arg0);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		this.model = new ModelD2RQ(D2RQMap);
		GraphD2RQ.setUsingD2RQQueryHandler(true);
		logger.setDebug(false);
		separator.setDebug(true);
		differentLogger.setDebug(true);
		differenceLogger.setDebug(false);
		performanceLogger.setDebug(true);
		rdqlLogger.setDebug(false);
		rsLogger.setDebug(false);
		SQLResultSet.logger=rsLogger;
		SQLResultSet.separatorLogger=new Logger(); // silent
//		this.model.enableDebug();
	}

	public static boolean compareQueryHandlers=true;
	
	public void runTest() throws Throwable {
	    separator.debug("");
		if (!compareQueryHandlers) {
			super.runTest();
			return;
		}		
		boolean oldState=GraphD2RQ.isUsingD2RQQueryHandler();
		try {
		    int configs=2;
			int nTimes=10;

			InfoD2RQ startInst;
			InfoD2RQ diffInfo[]=new InfoD2RQ[configs];
			Set resultSets[]= new HashSet[configs];
			Set resultMaps[]= new HashSet[configs];
			String printed[] = new String[configs];
			String description[]= new String[] { "SimpleQueryHandler", "D2RQQueryHandler"};
			boolean usingD2RQ[] = new boolean[] {false, true};
			boolean verbatim[] = new boolean[] {false,true};
			for (int i=0; i<configs; i++) {
			    GraphD2RQ.setUsingD2RQQueryHandler(usingD2RQ[i]);
			    rdqlLogger.setDebug(verbatim[i]);
			    rsLogger.setDebug(verbatim[i]);
				logger.debug("using " + description[i] + " ...");
			    startInst=InfoD2RQ.instance();
			    for (int j=0; j< nTimes; j++) {
			        if (j>0) {
					    rdqlLogger.setDebug(false);
					    rsLogger.setDebug(false);
			        }
			        super.runTest();
			    }
				diffInfo[i]=InfoD2RQ.instanceMinus(startInst);
				diffInfo[i].div(nTimes);
				resultSets[i]=results;
				resultMaps[i]=resultBindingsToMaps(results);
			}
			// performanceLogger.debug("RDQL-Query: " + queryString);
			performanceLogger.debug(description[0] + " vs. " + description[1] + " = " + 
			        diffInfo[0].sqlPerformanceString() + " : " + diffInfo[1].sqlPerformanceString() +
					" (duration - SQL queries/rows/fields)");
			if (!resultMaps[0].equals(resultMaps[1])) {
			    differentLogger.debug(description[0] + " vs. " + description[1] + " different results (" +
			            resultMaps[0].size() + ":" + resultMaps[1].size() + ")");
			    differenceLogger.debug("Query: " + queryString);
			    printed[0]=printObject(resultMaps[0]);
			    printed[1]=printObject(resultMaps[1]);
			    if (printed[0].equals(printed[1])) {
			        differentLogger.debug("... but printed the same.");
			    } else {
			        differenceLogger.debug("first Result:");
			        differenceLogger.debug(printed[0]);
			        differenceLogger.debug("----------------");
			        differenceLogger.debug("second Result:");
			        differenceLogger.debug(printed[1]);
			    }
			}
			assertEquals(resultMaps[0],resultMaps[1]);
		} catch (Exception e) {
			GraphD2RQ.setUsingD2RQQueryHandler(oldState);
			logger.error(e.toString());
			throw new RuntimeException(e); 
		}
		GraphD2RQ.setUsingD2RQQueryHandler(oldState);
	}
	
	
	private String printObject(Object obj) {
        if (obj instanceof Collection) {
            return printCollection((Collection)obj);
        } if (obj instanceof Map) {
            return printMap((Map)obj);
        } else {
            return obj.toString();
        }
	}
	
	private String printArray(String[] a) {
	    StringBuffer b=new StringBuffer("[");
	    for (int i=0; i< a.length; i++) {
	        if (i>0)
	            b.append(",");
	        b.append(a[i]);
	    }
	    b.append("]\n");
	    return b.toString();
	}
	
	private String printCollection(Collection c) {
	    String a[]=new String[c.size()];
	    Iterator it=c.iterator();
	    int i=0;
	    while (it.hasNext()) {
	        Object obj=it.next();
	        a[i]=printObject(obj);
	        i++;
	    }
	    Arrays.sort(a);
	    return printArray(a);
	}

	private String printMap(Map m) {
	    String a[]=new String[m.size()];
	    Iterator it=m.entrySet().iterator();
	    Object[] side=new Object[2];
	    String[] res=new String[2];
	    int i=0;
	    while (it.hasNext()) {
	        Map.Entry e=(Map.Entry)it.next();
	        a[i]=printObject(e.getKey()) + " = " + printObject(e.getValue());
	        i++;
	    }
	    Arrays.sort(a);
	    return printArray(a);
	}

	
	protected void tearDown() throws Exception {
		super.tearDown();
		this.model.close();
		this.results = null;
	}

	protected void rdql(String rdql) {
	    rdqlLogger.debug("RDQL-Query: " + rdql);
	    queryString=rdql;
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
		    String var=JenaCompatibility.resultBindingIteratorVarName(it);
		    Object val=JenaCompatibility.resultBindingIteratorValue(it,b);
			String strVal=val.toString();
			int size=strVal.length();
			if (size>250)
			    logger.debug("Big string (" + size + ") in resultBinding:\n" + strVal);
			m.put(var,strVal);
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
			logger.debug("Result binding " + count + ":");
			ResultBinding binding = (ResultBinding) it.next();
			ResultBindingIterator it2 = binding.iterator();
			while (it2.hasNext()) {
			    it2.next();
			    String varName=JenaCompatibility.resultBindingIteratorVarName(it2);
			    Object val=JenaCompatibility.resultBindingIteratorValue(it2,binding);
				logger.debug("    " + varName + " => " + val); 
			}
			count++;
		}
	}
		
 }
