package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.ArrayList;
import java.util.HashSet;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.helpers.QueryLanguageTestFramework;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;

public abstract class ExpressionTestFramework extends QueryLanguageTestFramework {
    String condition, sqlCondition=null;
    Logger translatedLogger=new Logger();

//    private static void activateExpressionTranslator(GraphD2RQ graph) {
//    	Map dbMap=graph.getPropertyBridgesByDatabase();
//    	Iterator it=dbMap.keySet().iterator();
//    	while (it.hasNext()) {
//   		DBConnection db=(DBConnection)it.next();
//   		String tr;
//   		tr=ExpressionTranslator.class.getName();
//    		//tr=MySQLExpressionTranslator.class.getName();
//  		db.setExpressionTranslator(tr);
//    	}
//    }
    	
	protected void setUp() throws Exception {
		super.setUp();
		sqlResultSetLogger.setDebug(false); // true
		rdqlLogger.setDebug(false); // true
		translatedLogger.setDebug(false);
		if (translatedLogger.debugEnabled())
		    ExpressionTranslator.logSqlExpressions=new HashSet();
// RC	QueryExecutionIterator.simulated=true;
		setUpMixOutputs(false);
		GraphD2RQ.setUsingD2RQQueryHandler(true);
		QueryExecutionIterator.protocol=new ArrayList();
// RC	activateExpressionTranslator((GraphD2RQ) model.getGraph());
	}
}
