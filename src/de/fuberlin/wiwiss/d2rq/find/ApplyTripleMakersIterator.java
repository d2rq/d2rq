package de.fuberlin.wiwiss.d2rq.find;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.map.TripleMaker;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;

/**
 * Iterates over the triple stream created by applying several triple makers
 * to each row of an SQL {@link QueryExecutionIterator}. Skips <tt>null</tt>
 * triples produced by the triple makers.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ApplyTripleMakersIterator.java,v 1.4 2006/09/09 15:40:05 cyganiak Exp $
 */
public class ApplyTripleMakersIterator implements ClosableIterator {
	private Collection tripleMakers;
	private ClosableIterator sqlIterator;
    private LinkedList tripleQueue = new LinkedList();
    private boolean explicitlyClosed = false;

	public ApplyTripleMakersIterator(ClosableIterator sqlIterator, Collection tripleMakers) {
		this.sqlIterator = sqlIterator;
		this.tripleMakers = tripleMakers;
	}

	public boolean hasNext() {
		if (this.explicitlyClosed || this.tripleMakers.isEmpty()) {
			// prevents starting up the underlying SQL iterator if we have no triple makers
			return false;
		}
		if (this.tripleQueue.isEmpty()) {
			tryFillTripleQueue();
		}
		return !this.tripleQueue.isEmpty();
	}

	public Object next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return this.tripleQueue.removeFirst();
	}
	
	public Triple nextTriple() {
		return (Triple) next();
	}

	public void close() {
		this.explicitlyClosed = true;
		this.sqlIterator.close();
	}
	
	public void remove() {
		throw new IllegalArgumentException("Operation not supported");
	}
	
	/**
	 * Puts some triples into the queue if triples can still be produced.
	 * If this method leaves the queue empty, then we are done.
	 */
	private void tryFillTripleQueue() {
		while (this.sqlIterator.hasNext() && this.tripleQueue.isEmpty()) {
			String[] nextRow = (String[]) this.sqlIterator.next();
			Iterator it = this.tripleMakers.iterator();
			while (it.hasNext()) {
				TripleMaker tripleMaker = (TripleMaker) it.next();
				Triple product = tripleMaker.makeTriple(nextRow);
				if (product != null) {
					this.tripleQueue.add(product);
				}
			}
		}
    }
}
