/*
 * Created on 14.04.2005 by Joerg Garbers, FU-Berlin
 *
 */
package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.RDQLTestFramework;
import de.fuberlin.wiwiss.d2rq.SPARQLTestFramework;
import de.fuberlin.wiwiss.d2rq.find.SQLResultSet;
import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.map.Database;
import junit.framework.TestCase;

/**
 * Note:
 * Currently (Jena 2.3) the expressions are not passed from the SPARQL parser down to the pattern stage.
 * So no reasonable results are given by this test case.
 * @author jgarbers
 *
 */
public class SPARQLExpressionTest extends ExpressionTestFramework { 
    
    public SPARQLExpressionTest(String arg0) {
        super(arg0);
     }
	
	String getSPARQLQuery(String condition) {
	    String triples=
	        " ?x <http://annotation.semanticweb.org/iswc/iswc.daml#author> ?z . " +
			" ?z <http://annotation.semanticweb.org/iswc/iswc.daml#eMail> ?y ";
	    String query="SELECT ?x ?y WHERE { " + triples + " . FILTER (" + condition + ") }";
	    return query;
	}
		
	void sparqlQuery() {
	    sparql(getSPARQLQuery(condition));
	    if (translatedLogger.debugEnabled()) {
	        translatedLogger.debug(condition + " ->");
	        Iterator it=ExpressionTranslator.logSqlExpressions.iterator();
	        while (it.hasNext()) {
	            String translated=(String) it.next();
	            translatedLogger.debug(translated);
	        }
	        if (sqlCondition!=null)
	            assertTrue(ExpressionTranslator.logSqlExpressions.contains(sqlCondition));
	        // else
	        //    assertTrue(ExpressionTranslator.logSqlExpressions.size()==0);
	    }
	}

	public void testRDQLGetAuthorsAndEmailsWithCondition() {
	    // GraphD2RQ.setUsingD2RQQueryHandler(false);
	    condition="(?x = ?z)";
	    //sqlCondition=""
	    sparqlQuery();
	}
	public void testNobody() {
	    condition="(?z != \"Nobody\")";
	    sparqlQuery();
	}
	public void testIsSeaborne() {
	    //condition="?z eq \"http://www-uk.hpl.hp.com/people#andy_seaborne\"";
	    condition="(?z = <http://www-uk.hpl.hp.com/people#andy_seaborne>)";
	    sparqlQuery();
	}
	public void testIsNotSeaborne() {
	    //condition="?z eq \"http://www-uk.hpl.hp.com/people#andy_seaborne\"";
	    condition="! (?z = <http://www-uk.hpl.hp.com/people#andy_seaborne>)";
	    sparqlQuery();
	}
	public void testEasy() {
	    condition="(1 = 1)";
	    sparqlQuery();
	}
	public void testImpossible() {
	    condition="! (1 = 1)";
	    sparqlQuery();
	}
	public void testAnd() {
	    condition="(1 = 1) && true";
	    sparqlQuery();
	}

}
