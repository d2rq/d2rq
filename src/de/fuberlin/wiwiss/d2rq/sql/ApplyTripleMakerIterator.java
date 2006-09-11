package de.fuberlin.wiwiss.d2rq.sql;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;


/**
 * Iterates over the triple stream created by applying several triple makers
 * to each row of an SQL {@link QueryExecutionIterator}. Skips <tt>null</tt>
 * triples produced by the triple makers.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ApplyTripleMakerIterator.java,v 1.1 2006/09/11 23:22:25 cyganiak Exp $
 */
public class ApplyTripleMakerIterator implements ClosableIterator {
	private TripleMaker tripleMaker;
	private ClosableIterator sqlIterator;
    private LinkedList tripleQueue = new LinkedList();
    private boolean explicitlyClosed = false;

	public ApplyTripleMakerIterator(ClosableIterator sqlIterator, TripleMaker tripleMaker) {
		this.sqlIterator = sqlIterator;
		this.tripleMaker = tripleMaker;
	}

	public boolean hasNext() {
		if (this.explicitlyClosed) {
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
			ResultRow nextRow = (ResultRow) this.sqlIterator.next();
			this.tripleQueue.addAll(this.tripleMaker.makeTriples(nextRow));
		}
    }
}
