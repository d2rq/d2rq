package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Collection;
import java.util.NoSuchElementException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/** 
 * Iterator for PatternQueryCombiner results.
 * @author jgarbers
 * @version $Id: PQCResultIterator.java,v 1.11 2006/09/13 14:06:23 cyganiak Exp $
 */
public class PQCResultIterator extends NiceIterator implements ClosableIterator {
    public static int instanceCounter=1;
    public int instanceNr=instanceCounter++;
    public int returnedTriplePatternNr=0;
    
    private String additionalLogInfo;

	private final VariableBindings variableBindings;
	private final Collection constraints;
	
	/** Iterator for TripleQuery conjunctions */
    protected ConjunctionIterator conjunctionsIterator;
    /** next TripleQuery conjunction to be processed */
	private RDFRelation[] conjunction; 
	/** iterator helper */
	protected Triple[] prefetchedResult=null;
	/** iterator helper */
	protected boolean didPrefetch=false;
	/** iterator that returns triple arrays for database rows */
	ApplyTripleMakerRowIterator resultSet=null; 
	
	ConnectedDB nextDatabase;

	public PQCResultIterator(RDFRelation[][] tripleQueries, VariableBindings variableBindings, Collection constraints) { // or maybe pass conjunctionsIterator as
		//combiner = combiner4;
		this.variableBindings=variableBindings;
		this.constraints=constraints;
		conjunction=new RDFRelation[tripleQueries.length];
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
        returnedTriplePatternNr++;
		return new CombinedPatternStage.IteratorResult(ret,nextDatabase);
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
				prefetchedResult = resultSet.nextRow();
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
			SelectStatementBuilder sql=PatternQueryCombiner.getSQL(conjunction);
			ch.addConstraintsToSQL(sql);
			nextDatabase = conjunction[0].baseRelation().database();
			this.resultSet = new ApplyTripleMakerRowIterator(sql.execute(), conjunction);
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

    /**
     * @return Returns the additionalLogInfo.
     */
    public String getAdditionalLogInfo() {
        return additionalLogInfo;
    }
    /**
     * @param additionalLogInfo The additionalLogInfo to set.
     */
    public void setAdditionalLogInfo(String additionalLogInfo) {
        this.additionalLogInfo = additionalLogInfo;
    }
}