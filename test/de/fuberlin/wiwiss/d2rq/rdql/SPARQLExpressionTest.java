package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Iterator;

/**
 * Note:
 * Currently (Jena 2.3) the expressions are not passed from the SPARQL parser down to the pattern stage.
 * So no reasonable results are given by this test case.
 * @author jgarbers
 * @version $Id: SPARQLExpressionTest.java,v 1.3 2006/09/03 00:08:12 cyganiak Exp $
 */
public class SPARQLExpressionTest extends ExpressionTestFramework { 
    
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
