package org.d2rq.tmp;

import com.hp.hpl.jena.sparql.engine.QueryEngineFactory;
import com.hp.hpl.jena.sparql.engine.main.QueryEngineMain;

public class QueryEngineD2RQ {
	public static void register() {}
	public static QueryEngineFactory getFactory() {
		return QueryEngineMain.getFactory();
	}
}
