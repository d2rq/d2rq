/*
 * Created on 14.04.2005 by Joerg Garbers, FU-Berlin
 *
 */
package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.ArrayList;

import de.fuberlin.wiwiss.d2rq.RDQLTestFramework;
import de.fuberlin.wiwiss.d2rq.find.SQLResultSet;
import junit.framework.TestCase;

/**
 * @author jgarbers
 *
 */
public class ExpressionTest extends RDQLTestFramework {
    String condition;

    public ExpressionTest(String arg0) {
        super(arg0);
    }

	protected void setUp() throws Exception {
		super.setUp();
		SQLResultSet.simulationMode=true;
		SQLResultSet.protocol=new ArrayList();
	}
	
	String getQuery(String condition) {
	    String triples=
	        "(?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?z), " +
			"(?z, <http://annotation.semanticweb.org/iswc/iswc.daml#eMail> , ?y)";
	    String query="SELECT ?x, ?y WHERE " + triples + " AND " + condition;
	    return query;
	}
	
	void query() {
	    rdql(getQuery(condition));
	}

	public void testRDQLGetAuthorsAndEmailsWithCondition() {
	    // GraphD2RQ.setUsingD2RQQueryHandler(false);
	    condition="! (?x eq ?z)";
		query();
	}
}
