/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import java.sql.*;
import java.util.*;

/**
 * Contains the result set from one SQL query and transforms it into triples.
 * A triple is produced for TripleMaker in TripleMaker and each row in the result set.
 *
 * <p>History:<br>
 * 06-06-2004: Initial version of this class.<br>
 * 08-03-2004: Almost complete rewrite to make logic more explicit.<br>
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
class CombinedTripleResultSet extends SQLResultSet {

	/** List of tripleMakers that are used on every row of the result set. */
    private TripleQuery[] tripleMakers = null;

	/**
	 * The chached triples. A value of null means there is no triple
	 * in the cache. A triple gets chached by hasNext() and is
	 * delivered afterwards by next().
	 */
    private Triple[] chachedTriples;

	public CombinedTripleResultSet(String SQL, Map columnNameNumberMap, Database db) {
		super(SQL, columnNameNumberMap, db);
	}
	
	public void setTripleMakers(TripleQuery[] tripMakers) {
    		this.tripleMakers = tripMakers;
	}

	public boolean hasNext() {
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
		if (!this.queryHasBeenExecuted) {
			executeSQLQuery();
			this.queryHasBeenExecuted = true;
		}

		int tripleCount=tripleMakers.length;
		Triple[] results=new Triple[tripleCount];
		boolean done;
		do {
			this.currentRow = nextRow();
			if (this.currentRow == null) {
				return null;
			}
			done=true;
			for (int i=0; i < tripleCount; i++) {
				TripleQuery tripMaker = (TripleQuery) tripleMakers[i];
				Triple triple = tripMaker.makeTriple(this.currentRow, this.columnNameNumberMap);
				if (triple==null) {
					done=false;
					break;
				}
				results[i]=triple; // jg 5.3.
			}
		} while (!done);
		return results;
    }
}
