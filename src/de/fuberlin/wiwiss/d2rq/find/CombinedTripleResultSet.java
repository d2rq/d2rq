package de.fuberlin.wiwiss.d2rq.find;

import java.util.Map;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;

/**
 * Contains the result set from one SQL query and transforms it into triples.
 * A triple is produced for TripleMaker in TripleMaker and each row in the result set.
 *
 * @author jgarbers
 * @version $Id: CombinedTripleResultSet.java,v 1.3 2006/08/29 15:13:12 cyganiak Exp $
 */
public class CombinedTripleResultSet {
	private Map columnNameNumberMap;
	private QueryExecutionIterator sqlIterator;
	/** List of tripleMakers that are used on every row of the result set. */
    private TripleQuery[] tripleMakers = null;

	/**
	 * The chached triples. A value of null means there is no triple
	 * in the cache. A triple gets chached by hasNext() and is
	 * delivered afterwards by next().
	 */
    private Triple[] chachedTriples;
    private boolean exhausted = false;
    
	public CombinedTripleResultSet(String sql, Map columnNameNumberMap, Database db) {
		this.columnNameNumberMap = columnNameNumberMap;
		this.sqlIterator = new QueryExecutionIterator(sql, db);
	}
	
	public void setTripleMakers(TripleQuery[] tripMakers) {
    		this.tripleMakers = tripMakers;
	}

	public boolean hasNext() {
		if (this.exhausted) {
			return false;
		}
		if (this.chachedTriples == null) {
			this.chachedTriples = next();
		}
		return this.chachedTriples != null;
	}

	public Triple[] next() {
		if (this.chachedTriples != null) {
			Triple[] t = this.chachedTriples;
			this.chachedTriples = null;
			return t;
		}
		int tripleCount=tripleMakers.length;
		Triple[] results=new Triple[tripleCount];
		boolean done;
		do {
			if (!this.sqlIterator.hasNext()) {
				this.exhausted = true;
				return null;
			}
			String[] nextRow = this.sqlIterator.nextRow();
			done=true;
			for (int i=0; i < tripleCount; i++) {
				TripleQuery tripMaker = (TripleQuery) tripleMakers[i];
				Triple triple = tripMaker.makeTriple(nextRow, this.columnNameNumberMap);
				if (triple==null) {
					done=false;
					break;
				}
				results[i]=triple; // jg 5.3.
			}
		} while (!done);
		return results;
    }
	
	public void close() {
		this.sqlIterator.close();
	}
}
