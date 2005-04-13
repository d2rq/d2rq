/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq.find;

import java.util.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.graph.*;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

/**
 * Extended iterator over the results of a find(spo) query.
 * The D2RQResultIterator returns the results of a find(spo) query in the form of triples.
 * The D2RQResultIterator iterates over all contained ResultTripleSets.
 *
 * <BR>History: 06-21-2004   : Initial version of this class.
 * @version V0.1
 * <p>History:<br>
 * 06-21-2004: Initial version of this class.<br>
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 *
 * @see de.fuberlin.wiwiss.d2rq.GraphD2RQ
 * @see de.fuberlin.wiwiss.d2rq.find.TripleResultSet
 */
public class D2RQResultIterator extends NiceIterator implements ExtendedIterator {

    /** 
     * All TripleResultSets for this D2RQResultIterator.
	 * There can be serveral because query results can be stored in several tables (e.g. rdf:type)
	 */
    private ArrayList tripleResultSets;

    /** Iterator over all TripleResultSets  */
    private Iterator tripleResultSetIterator;

    /** Flag that the iteration has finished */
    private boolean m_finished;

    /** Flag that a triple has been prefetched */
    private boolean m_prefetched = false;

    /** Prefetched Triple */
    private Triple m_prefetchedTriple = null;

    /** Prefetched TripleResultSet */
    private TripleResultSet m_prefetchedTripleResultSet = null;

    /**
     * Create an empty iterator.
     */
    public  D2RQResultIterator() {
    		this.m_finished = true;      // Prevent reading until a TripleResultSet is added
    		this.tripleResultSets = new ArrayList();
    }

    /** Adds a triple result set to the list of all result sets. */
    public void addTripleResultSet(TripleResultSet resultSet) {
    		this.tripleResultSets.add(resultSet);
    		this.tripleResultSetIterator = this.tripleResultSets.iterator();
    		this.m_finished = false;
    }

    /**
     * Test if there is a next result to return
     */
    public boolean hasNext() {
        if (!this.m_finished && !this.m_prefetched) moveForward();
        return !this.m_finished;
    }

    /**
     * Return the current row
     */
    public Object next() {
        if (!this.m_finished && !this.m_prefetched) moveForward();
        this.m_prefetched = false;
        if (this.m_finished) {
            throw new NoSuchElementException();
        }
        return this.m_prefetchedTriple;
    }

    /**
     * More forward one triple.
     * Iterates till there are no more triples in the current triple result set
     * and moves to the next resultset afterwards.
     * Sets the m_finished flag if there is no more to fetch.
     */
    private void moveForward() {
    		if (!this.m_finished ) {
    			if (this.m_prefetchedTripleResultSet == null) {
    				this.m_prefetchedTripleResultSet = (TripleResultSet) this.tripleResultSetIterator.next();
    			}
    			if (this.m_prefetchedTripleResultSet.hasNext()) {
    				// Get new Triple from current TripleResultSet
    				this.m_prefetchedTriple = this.m_prefetchedTripleResultSet.next();
    				this.m_prefetched = true;
    			} else {
    				// Close Result set after being used to the end
    				this.m_prefetchedTripleResultSet.close();
    				// Check if there are more TripleResultSets
    				if (this.tripleResultSetIterator.hasNext()) {
    					// Get a triple from the next TripleResultSets
    					this.m_prefetchedTripleResultSet = (TripleResultSet) this.tripleResultSetIterator.next();
    					moveForward();
    				} else {
    					// No more TripleResultSets => close()
    					close();
    				}
    			}
    		} else {
    			close();
    		}
    }

    /**
     * Delete the current row entry
     */
    public void remove() {
        cantRemove();
    }

    public Object removeNext()
        { cantRemove(); return null; }
    
    private void cantRemove() {
        throw new UnsupportedOperationException("D2RQResultIterator can't remove results.");
    }

    /**
     * Clean up the allocated resources - result set and statement.
     * If we know of an SQLCache return the statement there, otherwise close it.
     */
    public void close() {
    		if (!this.m_finished) {
    			while (this.tripleResultSetIterator.hasNext()) {
    				((TripleResultSet) this.tripleResultSetIterator.next()).close();
    			}
    		}
    		this.m_finished = true;
    }

    /**
     * Clean up the database cursor. Normally the client should read to the end
     * or explicity close but....
     */
    public void finalize() {
    		if (!this.m_finished) close();
    }

	/**
         return a new iterator which delivers all the elements of this iterator and
         then all the elements of the other iterator. Does not copy either iterator;
         they are consumed as the result iterator is consumed.
     */
	public ExtendedIterator andThen(ClosableIterator other) {
		return NiceIterator.andThen(this, other);
	}

}
