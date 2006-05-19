/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.rdql.Query;
import com.hp.hpl.jena.rdql.QueryEngine;
import com.hp.hpl.jena.rdql.QueryResults;
import com.hp.hpl.jena.rdql.ResultBinding;
import com.hp.hpl.jena.rdql.ResultBindingIterator;

import de.fuberlin.wiwiss.d2rq.find.SQLResultSet;
import de.fuberlin.wiwiss.d2rq.functional_tests.AllTests;
import de.fuberlin.wiwiss.d2rq.helpers.InfoD2RQ;
import de.fuberlin.wiwiss.d2rq.helpers.JenaCompatibility;
import de.fuberlin.wiwiss.d2rq.helpers.Logger;

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
	
	// compare fields
	int nTimes=1;
	InfoD2RQ startInst;
	boolean compareQueryHandlers=false;
    int configs;
	InfoD2RQ diffInfo[];
	Set resultMaps[];
	String printed[];
	String handlerDescription[];
	boolean usingD2RQ[];
	boolean verbatim[];

	
	protected void setUpHandlers() {
	    configs=2;
		diffInfo=new InfoD2RQ[configs];
		resultMaps= new HashSet[configs];
		printed = new String[configs];
		handlerDescription= new String[] { "SimpleQueryHandler", "D2RQQueryHandler"};
		usingD2RQ = new boolean[] {false, true};
	    verbatim = new boolean[] {false,true};
	}
	
	protected static Logger bigStringInResultLogger=new Logger();
	// Loggers used for switching on/off output
	protected Logger dumpLogger=new Logger();
	protected Logger usingLogger=new Logger();
	protected Logger testCaseSeparatorLogger=new Logger();
	protected Logger performanceLogger=new Logger();
	protected Logger rdqlLogger=new Logger();
	protected Logger differentLogger=new Logger();
	protected Logger differenceLogger=new Logger();
	protected Logger sqlResultSetLogger=new Logger();
	protected Logger oldSQLResultSetLogger;
	protected Logger oldSQLResultSetSeparatorLogger;
	protected boolean oldIsUsingD2RQQueryHandler;
	
	public RDQLTestFramework(String arg0) {
		super(arg0);
		setUpHandlers();
	}
	
	protected void setUpShowPerformance() {
	    nTimes=10;
	    compareQueryHandlers=true;
	    rdqlLogger.setDebug(true);
		rdqlLogger.setDebug(true);
		performanceLogger.setDebug(true);
	}
	protected void setUpMixOutputs(boolean v) {
	    compareQueryHandlers=v;
	    usingLogger.setDebug(v);	    
		testCaseSeparatorLogger.setDebug(v);
	}
	protected void setUpShowStatements() {
	    rdqlLogger.setDebug(true);
		sqlResultSetLogger.setDebug(true);	    
	}
	protected void setUpShowErrors() {
		differentLogger.setDebug(true);
		differenceLogger.setDebug(true);	    
	}
	protected void setUpShowWarnings() {
		bigStringInResultLogger.setDebug(true);	    
	}
	protected void setUpShowAll() {
	    setUpMixOutputs(true);
	    verbatim[0]=true;
	    setUpShowPerformance();
	    setUpShowStatements();
	    setUpShowErrors();
	    setUpShowWarnings();
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		this.model = new ModelD2RQ(D2RQMap);
		oldIsUsingD2RQQueryHandler=GraphD2RQ.isUsingD2RQQueryHandler();
		GraphD2RQ.setUsingD2RQQueryHandler(true);
		oldSQLResultSetLogger=SQLResultSet.logger;
		oldSQLResultSetSeparatorLogger=SQLResultSet.separatorLogger;
		SQLResultSet.logger=sqlResultSetLogger;
		SQLResultSet.separatorLogger=new Logger(); // silent
//		this.model.enableDebug();
	    setUpShowErrors(); // should be activated all the time
	    setUpShowPerformance(); // activate (only) to test performance (only)
	    //setUpShowStatements(); // activate to analyse generated SQL statements
	    //setUpMixOutputs(true); // activate to mix output from two QueryHandlers nicely
		//setUpShowAll(); // activate to get most verbatim output
	}
	
	protected void tearDown() throws Exception {
	    GraphD2RQ.setUsingD2RQQueryHandler(oldIsUsingD2RQQueryHandler);
		SQLResultSet.logger=oldSQLResultSetLogger;
		SQLResultSet.separatorLogger=oldSQLResultSetSeparatorLogger;
		this.model.close();
		this.results = null;
		super.tearDown();
	}
	
	public void runTest() throws Throwable {
	    testCaseSeparatorLogger.debug("");
		if (!compareQueryHandlers) {
			super.runTest();
			return;
		}		
		boolean oldState=GraphD2RQ.isUsingD2RQQueryHandler();
		boolean oldRDQLLoggerState=rdqlLogger.debugEnabled();
		boolean oldSqlResultSetLoggerState=sqlResultSetLogger.debugEnabled();
		try {
			for (int i=0; i<configs; i++) {
			    GraphD2RQ.setUsingD2RQQueryHandler(usingD2RQ[i]);
			    rdqlLogger.setDebug(verbatim[i] && oldRDQLLoggerState);
			    sqlResultSetLogger.setDebug(verbatim[i] && oldSqlResultSetLoggerState);
			    usingLogger.debug("using " + handlerDescription[i] + " ...");
			    startInst=InfoD2RQ.instance();
			    for (int j=0; j< nTimes; j++) {
			        if (j>0) {
					    rdqlLogger.setDebug(false);
					    sqlResultSetLogger.setDebug(false);
			        }
			        super.runTest();
			    }
				diffInfo[i]=InfoD2RQ.instanceMinus(startInst);
				diffInfo[i].div(nTimes);
				resultMaps[i]=results;
			}
			// performanceLogger.debug("RDQL-Query: " + queryString);
			performanceLogger.debug(handlerDescription[0] + " vs. " + handlerDescription[1] + " = " + 
			        diffInfo[0].sqlPerformanceString() + " : " + diffInfo[1].sqlPerformanceString() +
					" (duration - SQL queries/rows/fields)");
			if (!resultMaps[0].equals(resultMaps[1])) {
			    differentLogger.debug(handlerDescription[0] + " vs. " + handlerDescription[1] + " different results (" +
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
			throw e; 
		} finally {
			GraphD2RQ.setUsingD2RQQueryHandler(oldState);
			rdqlLogger.setDebug(oldRDQLLoggerState);
			sqlResultSetLogger.setDebug(oldSqlResultSetLoggerState);
		}
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
	    int i=0;
	    while (it.hasNext()) {
	        Map.Entry e=(Map.Entry)it.next();
	        a[i]=printObject(e.getKey()) + " = " + printObject(e.getValue());
	        i++;
	    }
	    Arrays.sort(a);
	    return printArray(a);
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
			this.results.add(resultBindingToMap(binding, qr.getResultVars()));
		}
	}

	protected void assertResultCount(int count) {
		assertEquals(count, this.results.size());
	}

	protected void assertResult(Map map) {
		Iterator it = this.results.iterator();
		while (it.hasNext()) {
			if (it.next().equals(map)) {
				return;
			}
		}
		fail();
	}
	
	public static Map resultBindingToMap(ResultBinding b, List variables) {
		Map m=new HashMap();
		ResultBindingIterator it=b.iterator();
		while (it.hasNext()) {
			it.next();
		    String var=JenaCompatibility.resultBindingIteratorVarName(it);
		    if (!variables.contains(var)) {
		    	continue;
		    }
		    Object val=JenaCompatibility.resultBindingIteratorValue(it,b);
			String strVal=val.toString();
			int size=strVal.length();
			if (size>250)
			    bigStringInResultLogger.debug("Big string (" + size + ") in resultBinding:\n" + strVal);
			m.put(var,val);
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

	protected void dump() {
	    if (!dumpLogger.debugEnabled())
	        return;
	    dumpLogger.debug("\n#Results: " + results.size() + ":");
		Iterator it = this.results.iterator();
		int count = 1;
		while (it.hasNext()) {
		    dumpLogger.debug("Result binding " + count + ":");
			Map binding = (Map) it.next();
			Iterator it2 = binding.keySet().iterator();
			while (it2.hasNext()) {
			    String varName = (String) it2.next();
			    Object val = binding.get(varName);
			    dumpLogger.debug("    " + varName + " => " + val); 
			}
			count++;
		}
	}
		
 }
