package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.NoSuchElementException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.fuberlin.wiwiss.d2rq.find.CombinedTripleResultSet;
import de.fuberlin.wiwiss.d2rq.find.SQLStatementMaker;
import de.fuberlin.wiwiss.d2rq.find.TripleQuery;
import de.fuberlin.wiwiss.d2rq.helpers.ConjunctionIterator;

/** 
 * Iterator for PatternQueryCombiner results.
 * @author jgarbers
 *
 */
class PQCResultIterator extends NiceIterator implements ClosableIterator {

    /**
	 * 
	 */
	private final PatternQueryCombiner combiner;
	/** Iterator for TripleQuery conjunctions */
    protected ConjunctionIterator conjunctionsIterator;
    /** next TripleQuery conjunction to be processed */
	protected TripleQuery[] conjunction; 
	/** iterator helper */
	protected Triple[] prefetchedResult=null;
	/** iterator helper */
	protected boolean didPrefetch=false;
	/** iterator that returns triple arrays for database rows */
	CombinedTripleResultSet resultSet=null; 
											

	public PQCResultIterator(PatternQueryCombiner combiner) { // or maybe pass conjunctionsIterator as
		this.combiner = combiner;
		// argument
		// if (!possible)
		//	return;
		conjunction=new TripleQuery[combiner.tripleCount];
		conjunctionsIterator= new ConjunctionIterator((Object[][]) combiner.tripleQueries, conjunction);
	}
	
	public boolean hasNext() {
		// if (!possible)
		//	return false;
		if (!didPrefetch) {
			prefetch();
			didPrefetch=true;
		}
		return (prefetchedResult!=null);
	}
	
	public Object next() {
		if (!didPrefetch) {
			prefetch();
		}
		if (prefetchedResult==null)
			throw new NoSuchElementException();
		Object ret=prefetchedResult;
		prefetchedResult=null;
		didPrefetch=false;
		return ret;
	}
	
	/**
	 * Tries to prefetch a <code>prefetchedResult</code>.
	 * There are two resources to draw from:
	 * 1. another row from the current SQL query (resultSet)
	 * 2. a new SQL query can be started
	 * Only those TripleQuery conjunctions are considered that may have
	 * solutions in terms of NodeConstraints on shared variables.
	 */
	protected void prefetch() {
		prefetchedResult=null;
		while (true) {
			if ((resultSet!=null) && (resultSet.hasNext())) {
				prefetchedResult = resultSet.next();
				return;
			}
			if (!conjunctionsIterator.hasNext())
				return;
			conjunctionsIterator.next();
			// TODO partition conjunction and conditions into parts
			// that can be handled by separate databases.
			// keep intermediate results and perform cross-products
			// in java.
			ConstraintHandler ch=new ConstraintHandler();
			ch.setVariableBindings(combiner.bindings);
			ch.setTripleQueryConjunction(conjunction);
			ch.setRDQLConstraints(combiner.constraints);
			ch.makeConstraints();
			if (!ch.possible)
			    continue;
			SQLStatementMaker sql=PatternQueryCombiner4.getSQL(conjunction);
			ch.addConstraintsToSQL(sql);
			resultSet = new 
				CombinedTripleResultSet(sql.getSQLStatement(),
											sql.getColumnNameNumberMap(),
											sql.getDatabase());
			resultSet.setTripleMakers(conjunction);
		} // enless while loop
	}

	/* 
	 * Closes query to database.
	 * @see com.hp.hpl.jena.util.iterator.ClosableIterator#close()
	 */
	public void close() {
		if (resultSet!=null)
		    resultSet.close();
	}

	/* 
	 * Not supported.
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}
}