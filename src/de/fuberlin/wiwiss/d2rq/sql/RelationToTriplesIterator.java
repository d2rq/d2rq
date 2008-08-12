package de.fuberlin.wiwiss.d2rq.sql;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;


/**
 * Takes a {@link Relation}, runs the corresponding SELECT query, and applies
 * a {@link TripleMaker} to each SQL result row. Iterates over each of the 
 * generated triples. Skips <tt>null</tt> triples produced by the triple makers.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: RelationToTriplesIterator.java,v 1.1 2008/08/12 06:47:36 cyganiak Exp $
 */
public class RelationToTriplesIterator implements ClosableIterator {
	
	public static ClosableIterator createTripleIterator(Relation relation, TripleMaker tripleMaker) {
		if (relation.equals(Relation.EMPTY)) {
			return NullIterator.instance;
		}
		if (relation.isTrivial()) {
			return new SingletonIterator(ResultRow.NO_ATTRIBUTES);
		}
		return new RelationToTriplesIterator(relation, tripleMaker);
	}
	
	private TripleMaker tripleMaker;
	private ClosableIterator sqlIterator;
    private LinkedList tripleQueue = new LinkedList();
    private boolean explicitlyClosed = false;

    private RelationToTriplesIterator(Relation relation, TripleMaker tripleMaker) {
    	SelectStatementBuilder select = new SelectStatementBuilder(relation);
    	this.sqlIterator = new QueryExecutionIterator(
    			select.getSQLStatement(), select.getColumnSpecs(), relation.database());
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
