/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import java.sql.*;
import java.util.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.graph.*;

/**
 * Extended iterator over the results of a find(spo) query.
 * The D2RQResultIterator returns the results of a find(spo) query in the form of triples.
 * The D2RQResultIterator iterates over all contained ResultTripleSets.
 *
 * <BR>History: 06-21-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 *
 * @see de.fuberlin.wiwiss.d2rq.GraphD2RQ
 * @see de.fuberlin.wiwiss.d2rq.TripleResultSet
 */
public class D2RQResultIterator extends NiceIterator implements ExtendedIterator {

    /** All TripleResultSets for this D2RQResultIterator
	* There can be serveral because query results can be stored in several tables (e.g. rdf:type)
	*/
    protected ArrayList tripleResultSets;

    /** Iterator over all TripleResultSets  */
    protected Iterator tripleResultSetIterator;

    /** Flag that the iteration has finished */
    protected boolean m_finished;

    /** Flag that a triple has been prefetched */
    protected boolean m_prefetched = false;

    /** Prefetched Triple */
    protected Triple m_prefetchedTriple = null;

    /** Prefetched TripleResultSet */
    protected TripleResultSet m_prefetchedTripleResultSet = null;

    /**
     * Create an empty iterator.
     */
    public  D2RQResultIterator() {
        m_finished = true;      // Prevent reading until a TripleResultSet is added
        tripleResultSets = new ArrayList();
    }

    /** Adds a triple result set to the list of all result sets. */
    public void addTripleResultSet(TripleResultSet resultSet) {
        tripleResultSets.add(resultSet);
        tripleResultSetIterator = tripleResultSets.iterator();
        m_finished = false;
    }

    /**
     * Test if there is a next result to return
     */
    public boolean hasNext() {
        if (!m_finished && !m_prefetched) moveForward();
        return !m_finished;
    }

    /**
     * Return the current row
     */
    public Object next() {
        if (!m_finished && !m_prefetched) moveForward();
        m_prefetched = false;
        if (m_finished) {
            throw new NoSuchElementException();
        }
        return m_prefetchedTriple;
    }

    /**
     * More forward one triple.
     * Iterates till there are no more triples in the current triple result set
     * and moves to the next resultset afterwards.
     * Sets the m_finished flag if there is no more to fetch.
     */
    protected void moveForward() {
            if (!m_finished ) {
                if (m_prefetchedTripleResultSet == null) {
                    m_prefetchedTripleResultSet = (TripleResultSet) tripleResultSetIterator.next();
                }
                if (m_prefetchedTripleResultSet.hasNext()) {
                    // Get new Triple from current TripleResultSet
                     m_prefetchedTriple = m_prefetchedTripleResultSet.next();
                     m_prefetched = true;
                } else {
                    // Close Result set after being used to the end
                    m_prefetchedTripleResultSet.close();
                    // Check if there are more TripleResultSets
                    if (tripleResultSetIterator.hasNext()) {
                        // Get a triple from the next TripleResultSets
                        m_prefetchedTripleResultSet = (TripleResultSet) tripleResultSetIterator.next();
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
    
    protected void cantRemove() {
        throw new UnsupportedOperationException("D2RQResultIterator can't remove results.");
    }

    /**
     * Clean up the allocated resources - result set and statement.
     * If we know of an SQLCache return the statement there, otherwise close it.
     */
    public void close() {
        if (!m_finished) {
            while (tripleResultSetIterator.hasNext()) {
                ((TripleResultSet) tripleResultSetIterator.next()).close();
            }
        }
        m_finished = true;
    }

    /**
     * Clean up the database cursor. Normally the client should read to the end
     * or explicity close but....
     */
    protected void finalize() {
        if (!m_finished) close();
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
