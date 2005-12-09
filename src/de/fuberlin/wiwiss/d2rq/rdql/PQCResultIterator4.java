package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.fuberlin.wiwiss.d2rq.find.CombinedTripleResultSet;
import de.fuberlin.wiwiss.d2rq.find.SQLStatementMaker;
import de.fuberlin.wiwiss.d2rq.find.TripleQuery;
import de.fuberlin.wiwiss.d2rq.helpers.ConjunctionIterator;
import de.fuberlin.wiwiss.d2rq.map.Database;

/** 
 * Iterator for PatternQueryCombiner results.
 * @author jgarbers
 *
 */
class PQCResultIterator4 extends NiceIterator implements ClosableIterator {

    /**
	 * 
	 */
	//private final PatternQueryCombiner4 combiner;
	private final TripleQuery[][] tripleQueries;
	private final VariableBindings variableBindings;
	private final Collection constraints;
	
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
	
	Database nextDatabase;
											

	public PQCResultIterator4(TripleQuery[][] tripleQueries, VariableBindings variableBindings, Collection constraints) { // or maybe pass conjunctionsIterator as
		//combiner = combiner4;
		this.tripleQueries=tripleQueries;
		this.variableBindings=variableBindings;
		this.constraints=constraints;
		conjunction=new TripleQuery[tripleQueries.length];
		conjunctionsIterator= new ConjunctionIterator(tripleQueries, conjunction);
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
		Triple[] ret=prefetchedResult;
		prefetchedResult=null;
		didPrefetch=false;
		return new CombinedPatternStage4.IteratorResult(ret,nextDatabase);
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
			ch.setVariableBindings(variableBindings);
			ch.setTripleQueryConjunction(conjunction);
			ch.setRDQLConstraints(constraints);
			ch.makeConstraints();
			if (!ch.possible)
			    continue;
			SQLStatementMaker sql=PatternQueryCombiner4.getSQL(conjunction);
			ch.addConstraintsToSQL(sql);
			String statement=sql.getSQLStatement();
			Map map=sql.getColumnNameNumberMap();
			nextDatabase=sql.getDatabase();
			resultSet = new 
				CombinedTripleResultSet(statement,map,nextDatabase);
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