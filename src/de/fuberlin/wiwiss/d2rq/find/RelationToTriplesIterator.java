package de.fuberlin.wiwiss.d2rq.find;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.CollectionFactory;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;
import com.hp.hpl.jena.util.iterator.WrappedIterator;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;


/**
 * Takes a {@link Relation}, runs the corresponding SELECT query, and applies
 * a {@link TripleMaker} to each SQL result row. Iterates over each of the 
 * generated triples. Skips <tt>null</tt> triples produced by the triple makers.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class RelationToTriplesIterator implements ClosableIterator {
	
	public static ClosableIterator create(Relation relation, Collection tripleMakers) {
		if (relation.equals(Relation.EMPTY)) {
			return NullIterator.instance();
		}
		if (relation.isTrivial()) {
			ArrayList tripleList = new ArrayList();
			Iterator it = tripleMakers.iterator();
			while (it.hasNext()) {
				TripleMaker tripleMaker = (TripleMaker) it.next();
				Triple t = tripleMaker.makeTriple(ResultRow.NO_ATTRIBUTES);
				if (t != null)
					tripleList.add(t);
			}
			return WrappedIterator.create(tripleList.iterator());				
		}
		
		return new RelationToTriplesIterator(relation, tripleMakers);
	}
	
	private Collection tripleMakers;
	private ClosableIterator sqlIterator;
    private LinkedList tripleQueue = new LinkedList();
    private boolean explicitlyClosed = false;
    private String sql;
    
    private RelationToTriplesIterator(Relation relation, Collection tripleMakers) {
    	SelectStatementBuilder select = new SelectStatementBuilder(relation);
    	this.sqlIterator = new QueryExecutionIterator(
    			select.getSQLStatement(), select.getColumnSpecs(), relation.database());
		this.tripleMakers = tripleMakers;
    }
    
	public boolean hasNext() {
		if (this.explicitlyClosed) {
			return false;
		}
		if (this.tripleQueue.isEmpty()) {
			tryFillTripleQueue();
		}
		/*
		 * Jena's NiceIterator.andThen() assumes exhausted iterators to auto-close,
		 * see Jena Bug 2565071
		 */ 
		if (this.tripleQueue.isEmpty()) {
			close();
			return false;
		}
		else
			return true;
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
			Iterator it = tripleMakers.iterator();
			while (it.hasNext()) {
				TripleMaker tripleMaker = (TripleMaker) it.next();
				Triple t = tripleMaker.makeTriple(nextRow);
				if (t != null) this.tripleQueue.add(t);
			}
		}
    }
}
