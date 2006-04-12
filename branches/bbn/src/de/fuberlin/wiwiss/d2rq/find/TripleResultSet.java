/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Iterator;
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
public class TripleResultSet extends SQLResultSet {

	/** List of tripleMakers that are used on every row of the result set. */
    private ArrayList tripleMakers = new ArrayList();
    private Iterator tripleMakerIterator;
    protected boolean rsForward = false;

	/**
	 * The chached triple. A value of null means there is no triple
	 * in the cache. A triple gets chached by hasNext() and is
	 * delivered afterwards by next().
	 */
    private Triple chachedTriple;
	
	public TripleResultSet(String SQL, Map columnNameNumberMap, Database db) {
		super(SQL, columnNameNumberMap, db);
	}


	public void addTripleMaker(TripleQuery tripMaker) {
    		this.tripleMakers.add(tripMaker);
	}

	public boolean hasTripleMakers() {
		return this.tripleMakers.size() > 0;
    	}

	public boolean hasNext() {
		if (this.chachedTriple == null) {
			this.chachedTriple = next();
		}
		return this.chachedTriple != null;
	}

    /**
     * Returns the next triple.
     * If there are no more triple makers for the current row of the result set
     * then the next row is cached and the triple makers iterator is reset.
     * @return The next triple, or null if no more triples.
     */
	public Triple next() {
		if (!hasTripleMakers()) {
			return null;
		}
		if (this.chachedTriple != null) {
			Triple t = this.chachedTriple;
			this.chachedTriple = null;
			return t;
		}
		if (!this.queryHasBeenExecuted) {
			executeSQLQuery();
			this.queryHasBeenExecuted = true;
		}
		if (this.rsForward && !this.tripleMakerIterator.hasNext()) {
			this.rsForward = false;
		}
		if (!this.rsForward) {
			this.currentRow = nextRow();
			if (this.currentRow == null) {
				return null;
			}
			this.tripleMakerIterator = this.tripleMakers.iterator();
			this.rsForward = true;
		}
		TripleQuery tripMaker = (TripleQuery) this.tripleMakerIterator.next();
		Triple triple = tripMaker.makeTriple(this.currentRow, this.columnNameNumberMap);
		return (triple != null) ? triple : next();
    }

}
