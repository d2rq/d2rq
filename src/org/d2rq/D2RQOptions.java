package org.d2rq;

import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.Symbol;

/**
 * Configuration options, mostly related to optional optimizations,
 * for use in an ARQ {@link Context}
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class D2RQOptions {
	public final static String NS = "http://d2rq.org/terms/opt.ttl#";
	
	public final static Symbol MULTIPLEX_QUERIES = Symbol.create(NS + "multiplexQueries");
	public final static Symbol SOFT_CONDITIONS = Symbol.create(NS + "softConditions");
	public final static Symbol TRIM_JOINS = Symbol.create(NS + "trimJoins");
	public final static Symbol AVOID_SELF_JOINS = Symbol.create(NS + "avoidSelfJoins");
	public final static Symbol FILTER_TO_SQL = Symbol.create(NS + "filterToSQL");

	public static Context getContext(boolean fastMode) {
		Context result = ARQ.getContext().copy();
		String defaultValue = fastMode ? "true" : "false";
		result.set(D2RQOptions.MULTIPLEX_QUERIES, defaultValue);
		result.set(D2RQOptions.FILTER_TO_SQL, defaultValue);
		return result;
	}
	
	// Cannot instantiate, static only
	private D2RQOptions() {}
}
