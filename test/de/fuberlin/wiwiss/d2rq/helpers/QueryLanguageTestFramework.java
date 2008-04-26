package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import junit.framework.TestCase;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.DC;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.sql.BeanCounter;
import de.fuberlin.wiwiss.d2rq.vocab.FOAF;
import de.fuberlin.wiwiss.d2rq.vocab.ISWC;
import de.fuberlin.wiwiss.d2rq.vocab.SKOS;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author jgarbers
 * @version $Id: QueryLanguageTestFramework.java,v 1.5 2008/04/26 21:27:09 cyganiak Exp $
 */
public abstract class QueryLanguageTestFramework extends TestCase {
	protected ModelD2RQ model;
	protected Set results;
	protected String queryString;
	protected Map currentSolution = new HashMap();
	
	// compare fields
	int nTimes=1;
	BeanCounter startInst;
	boolean compareQueryHandlers=false;
    int configs;
	BeanCounter diffInfo[];
	Set resultMaps[];
	String printed[];
	String handlerDescription[];
	boolean usingD2RQ[];
	boolean verbatim[];

	
	protected void setUpHandlers() {
	    configs=2;
		diffInfo=new BeanCounter[configs];
		resultMaps= new HashSet[configs];
		printed = new String[configs];
		handlerDescription= new String[] { "SimpleQueryHandler", "D2RQQueryHandler"};
		usingD2RQ = new boolean[] {false, true};
	    verbatim = new boolean[] {false,true};
	}
	
	private final static String pckg = "de.fuberlin.wiwiss.d2rq.testing.";
	protected static Logger bigStringInResultLogger = Logger.getLogger(pckg + "BigStringInResult");
	// Loggers used for switching on/off output
	protected Logger dumpLogger = Logger.getLogger(pckg + "Dump");
	protected Logger usingLogger = Logger.getLogger(pckg + "Using");
	protected Logger testCaseSeparatorLogger = Logger.getLogger(pckg + "TestCaseSeparator");
	protected Logger performanceLogger = Logger.getLogger(pckg + "Performance");
	protected Logger rdqlLogger = Logger.getLogger(pckg + "RDQL");
	protected Logger differentLogger = Logger.getLogger(pckg + "Different");
	protected Logger differenceLogger = Logger.getLogger(pckg + "Difference");
	protected Logger sqlResultSetLogger = Logger.getLogger(pckg + "SQLResultSet");
	protected Logger oldSQLResultSetLogger;
	protected Logger oldSQLResultSetSeparatorLogger;
	
	public QueryLanguageTestFramework() {
		super();
		setUpHandlers();
	}
	
	protected void setUpShowPerformance() {
	    nTimes=10;
	    compareQueryHandlers=true;
	    rdqlLogger.setLevel(Level.DEBUG);
		rdqlLogger.setLevel(Level.DEBUG);
		performanceLogger.setLevel(Level.DEBUG);
	}
	protected void setUpMixOutputs(boolean v) {
	    compareQueryHandlers=v;
	    usingLogger.setLevel(v ? Level.DEBUG : Level.INFO);	    
		testCaseSeparatorLogger.setLevel(v ? Level.DEBUG : Level.INFO);
	}
	protected void setUpShowStatements() {
	    rdqlLogger.setLevel(Level.DEBUG);
		sqlResultSetLogger.setLevel(Level.DEBUG);	    
	}
	protected void setUpShowErrors() {
		differentLogger.setLevel(Level.DEBUG);
		differenceLogger.setLevel(Level.DEBUG);	    
	}
	protected void setUpShowWarnings() {
		bigStringInResultLogger.setLevel(Level.DEBUG);	    
	}
	protected void setUpShowAll() {
	    setUpMixOutputs(true);
	    verbatim[0]=true;
	    setUpShowPerformance();
	    setUpShowStatements();
	    setUpShowErrors();
	    setUpShowWarnings();
	}
	
	protected abstract String mapURL();
	
	protected void setUp() throws Exception {
		this.model = new ModelD2RQ(mapURL(), "N3", "http://test/");
//		this.model.enableDebug();
	    setUpShowErrors(); // should be activated all the time
//	    setUpShowPerformance(); // activate (only) to test performance (only)
	    //setUpShowStatements(); // activate to analyse generated SQL statements
	    //setUpMixOutputs(true); // activate to mix output from two QueryHandlers nicely
		//setUpShowAll(); // activate to get most verbatim output
	}
	
	protected void tearDown() throws Exception {
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
		Level oldRDQLLoggerState=rdqlLogger.getLevel();
		Level oldSqlResultSetLoggerState=sqlResultSetLogger.getLevel();
		try {
			for (int i=0; i<configs; i++) {
			    rdqlLogger.setLevel(verbatim[i] ? oldRDQLLoggerState : Level.INFO);
			    sqlResultSetLogger.setLevel(verbatim[i] ? oldSqlResultSetLoggerState: Level.INFO);
			    usingLogger.debug("using " + handlerDescription[i] + " ...");
			    startInst=BeanCounter.instance();
			    for (int j=0; j< nTimes; j++) {
			        if (j>0) {
					    rdqlLogger.setLevel(Level.INFO);
					    sqlResultSetLogger.setLevel(Level.INFO);
			        }
			        super.runTest();
			    }
				diffInfo[i]=BeanCounter.instanceMinus(startInst);
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
			rdqlLogger.setLevel(oldRDQLLoggerState);
			sqlResultSetLogger.setLevel(oldSqlResultSetLoggerState);
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
		rdql += "\nUSING\n";
		rdql += "dc FOR <" + DC.NS + ">\n";
		rdql += "foaf FOR <" + FOAF.NS + ">\n";
		rdql += "skos FOR <" + SKOS.NS + ">\n";
		rdql += "iswc FOR <" + ISWC.NS + ">\n";
		Query query = QueryFactory.create(rdql, Syntax.syntaxRDQL);
		ResultSet qr = QueryExecutionFactory.create(query, this.model).execSelect();
		while (qr.hasNext()) {
			QuerySolution binding = (QuerySolution) qr.next();
			this.results.add(solutionToMap(binding, qr.getResultVars()));
		}
	}

	protected void sparql(String sparql) {
//	    rdqlLogger.debug("RDQL-Query: " + rdql);
		queryString=sparql;
		sparql = 
				"PREFIX dc: <" + DC.NS + ">\n" +
				"PREFIX foaf: <" + FOAF.NS + ">\n" +
				"PREFIX skos: <" + SKOS.NS + ">\n" +
				"PREFIX iswc: <" + ISWC.NS + ">\n" +
				sparql;
		Query query = QueryFactory.create(sparql);
		QueryExecution qe = QueryExecutionFactory.create(query, this.model);
		this.results = new HashSet();
		ResultSet resultSet = qe.execSelect();
		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.nextSolution();
			addSolution(solution);
		}
	}
	
	private void addSolution(QuerySolution solution) {
		Map map = new HashMap();
		Iterator it = solution.varNames();
		while (it.hasNext()) {
			String variable = (String) it.next();
			RDFNode value = solution.get(variable);
			map.put(variable, value);
		}
		this.results.add(map);
	}

	protected void assertResultCount(int count) {
		assertEquals(count, this.results.size());
	}

	protected void expectVariable(String variableName, RDFNode value) {
		this.currentSolution.put(variableName, value);
	}
	
	protected void assertSolution() {
		if (!this.results.contains(this.currentSolution)) {
			fail();
		}
		this.currentSolution.clear();
	}
	
	public static Map solutionToMap(QuerySolution solution, List variables) {
		Map result = new HashMap();
		Iterator it = solution.varNames();
		while (it.hasNext()) {
		    String variableName = (String) it.next();
		    if (!variables.contains(variableName)) {
		    	continue;
		    }
		    RDFNode value = solution.get(variableName);
			int size = value.toString().length();
			if (size>250) {
				bigStringInResultLogger.debug("Big string (" + size + ") in resultBinding:\n" + value);
			}
			result.put(variableName,value);
		}
		return result;
	}
	
	protected void dump() {
		System.out.println("\n#Results: " + results.size() + ":");
		Iterator it = this.results.iterator();
		int count = 1;
		while (it.hasNext()) {
		    System.out.println("Result binding " + count + ":");
			Map binding = (Map) it.next();
			Iterator it2 = binding.keySet().iterator();
			while (it2.hasNext()) {
			    String varName = (String) it2.next();
			    Object val = binding.get(varName);
			    System.out.println("    " + varName + " => " + val); 
			}
			count++;
		}
	}
}
