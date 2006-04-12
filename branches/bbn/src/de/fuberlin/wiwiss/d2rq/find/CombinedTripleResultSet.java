/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq.find;

import java.util.Map;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.map.Database;

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
public class CombinedTripleResultSet extends SQLResultSet {

	/** List of tripleMakers that are used on every row of the result set. */
    private Object[] tripleMakers = null;
    private Object[] compiled = null;
	/**
	 * The chached triples. A value of null means there is no triple
	 * in the cache. A triple gets chached by hasNext() and is
	 * delivered afterwards by next().
	 */
    private Triple[] chachedTriples;

	public CombinedTripleResultSet(String SQL, Map columnNameNumberMap, Database db) {
		super(SQL, columnNameNumberMap, db);
	}
	
	/**
	 * Sets the value of tripleMakers
	 * @param tripMakers an Object array of type TripleQuery.
	 */
	
	public void setTripleMakers(Object[] tripMakers) {
    		this.tripleMakers = tripMakers;
	}
    
	/**
	 * Returns the value of TripleMakers
	 * @return TripleQuery array. 
	 */
    public Object[] getTripleMakers() {
       return tripleMakers;
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

    /**
     * @return Returns the array of compiled triples.
     */
    public Object[] getCompiled() {
        return compiled;
    }

    /**
     * @param compiled The compiled set of triples.
     */
    public void setCompiled(Object[] compiled) {
        this.compiled = compiled;
    }
}
