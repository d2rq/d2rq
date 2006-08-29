package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.fuberlin.wiwiss.d2rq.find.PropertyBridgeQuery;
import de.fuberlin.wiwiss.d2rq.helpers.ConjunctionIterator;
import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/** 
 * Iterator for PatternQueryCombiner results.
 * @author jgarbers
 *
 */
public class PQCResultIterator extends NiceIterator implements ClosableIterator {

    public static Logger logger;
    public static int instanceCounter=1;
    public int instanceNr=instanceCounter++;
    public int returnedTriplePatternNr=0;
    
    private String additionalLogInfo;

	private final VariableBindings variableBindings;
	private final Collection constraints;
	
	/** Iterator for TripleQuery conjunctions */
    protected ConjunctionIterator conjunctionsIterator;
    /** next TripleQuery conjunction to be processed */
	private PropertyBridgeQuery[] conjunction; 
	/** iterator helper */
	protected Triple[] prefetchedResult=null;
	/** iterator helper */
	protected boolean didPrefetch=false;
	/** iterator that returns triple arrays for database rows */
	ApplyTripleMakerRowIterator resultSet=null; 
	
	Database nextDatabase;
											

	public PQCResultIterator(PropertyBridgeQuery[][] tripleQueries, VariableBindings variableBindings, Collection constraints) { // or maybe pass conjunctionsIterator as
		//combiner = combiner4;
		this.variableBindings=variableBindings;
		this.constraints=constraints;
		conjunction=new PropertyBridgeQuery[tripleQueries.length];
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

	public boolean isDebugEnabled() {
	    return logger!=null && logger.debugEnabled();
	}
	
	private void logPattern(String msg, Triple[] ret) {
        if (logger!=null && logger.debugEnabled()) {
            String str=Arrays.asList(ret).toString();
            // if (lastPrintedInstanceNr!=instanceNr) {
            //     logger.debug("-- Iterator switch --");
            //     lastPrintedInstanceNr=instanceNr;
            // }
            logger.debug("PQCResultIterator4-" + instanceNr + " " + msg +
                    " length-" + ret.length + 
                    " DB-" + nextDatabase + (additionalLogInfo!=null?additionalLogInfo:"") + ":\n" + str + "\n");
        }
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
        logPattern("resultPattern-" + returnedTriplePatternNr ,ret);
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
			String statement=sql.getSQLStatement();
			Map map=sql.getColumnNameNumberMap();
			nextDatabase = conjunction[0].getDatabase();
			QueryExecutionIterator it = new QueryExecutionIterator(statement, this.nextDatabase);
			this.resultSet = new ApplyTripleMakerRowIterator(it, conjunction, map);
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