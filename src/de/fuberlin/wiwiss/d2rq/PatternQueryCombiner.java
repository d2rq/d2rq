/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.PatternStage;
import com.hp.hpl.jena.graph.query.Pattern;
import com.hp.hpl.jena.graph.query.Pipe;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.fuberlin.wiwiss.d2rq.helpers.ConjunctionIterator;
import de.fuberlin.wiwiss.d2rq.helpers.IndexArray;
import de.fuberlin.wiwiss.d2rq.helpers.VariableBindings;


/** 
 * Handles a triple pattern query on a D2RQ mapped database.
 * (Current)Assumption: all PropertyBridges refer to same database.
 * Contract: after Constructor, first call setup() 
 * then  call resultTriplesIterator().
 * <p>
 * This class does not need to know anything about D2RQPatternStage, except
 * it's {@link VariableBinding} semantics.
 * <p>
 * It seems that some of the information computed here could be reused in successive
 * calls from D2RQPatternStage2. On the other hand, lots of preprocessing is
 * useless, if there are (Bound) variables for predicates.
 * 
 * @author jgarbers
 * @see QueryCombiner
 */
class PatternQueryCombiner {
    /** if false then contradiction, no SQL query necessary. */
	private boolean possible=true; 
	protected Database database;
	protected GraphD2RQ graph;
	protected Triple [] triples; // nodes are ANY or fixed
	protected int tripleCount;
	protected ExpressionSet constraints; // RDQL-Constraints

	VariableBindings bindings; // includes variables and sharedVariables
	
	/** holds for each triple its 'a priory' appliable bridges */
	protected ArrayList[] bridges; 
	
	/** holds for each triple its number of bridges or tripleQueries */
	protected int[] bridgesCounts; 
								  
	/** holds for each triple its disjunctive SQL-TripleQuery Objects */
	protected TripleQuery[][] tripleQueries; 
	
public PatternQueryCombiner( GraphD2RQ graph, VariableBindings bindings, ExpressionSet constraints, Triple [] triples ) {
	this.graph=graph;
	this.bindings=bindings;
	tripleCount=triples.length;
	this.triples=triples; 
	this.constraints=constraints;
}	

void setup() {
	if (!possible)
		return;
	makeStores();
	makePropertyBridges(); // -> setsOfPossiblePropertyBridges
	// reducePropertyBridges();
	makeTripleQueries();
	database=((PropertyBridge)bridges[0].get(0)).getDatabase();
	// makeCompatibleConjunctions(); // can be returned by an iterator
	// conjunctionResultIterator(); // pulls answers from database
								 //	for each result
								 //		get query variables from the
								 // triple-disjunctions
								 //		check conditions that are not checked by SQL
								 // (stage)
								 //		push into iterator
}

/** allocates arrays */
void makeStores() {
	if (!possible)
		return;
	bridges=new ArrayList[tripleCount];
	bridgesCounts=new int[tripleCount];
	tripleQueries=new TripleQuery[tripleCount][];
}

/** 
 * Creates copies of the property bridges that could fit the <code>triples</code>.
 * Two triples that are combined with AND generally have nothing in common,
 * so we create individual instances of the bridges and systematically rename 
 * their tables. We prefix each Table with "T<n>_ where <n> is the index of the
 *  triple in the overall query.
 * 
 * @see TablePrefixer 
 */
void makePropertyBridges() {
	if (!possible)
		return;
	for (int i=0; possible && (i<tripleCount); i++) {
		Triple t=triples[i];
		ArrayList tBridges=graph.propertyBridgesForTriple(t);
		bridges[i]=tBridges;
		int bridgesCount=tBridges.size();
		if (bridgesCount==0) {
			possible=false;
			return;
		}
		for (int j=0; j<bridgesCount; j++) {
			TablePrefixer prefixer=new TablePrefixer(i); // really i!
			PropertyBridge p=(PropertyBridge)prefixer.prefix(tBridges.get(j));
			p.setTablePrefixer(prefixer);
			tBridges.set(j,p);
		}
	}
}

/** 
 * Creates a {@link TripleQuery} for each {@link PropertyBridge}.
 * As a side effect we also set <code>bridgesCounts</code>.
 */
void makeTripleQueries() {
	if (!possible)
		return;
	for (int i=0; i<tripleCount; i++) {
		Triple t=triples[i];
		int bridgesCount=bridges[i].size();
		tripleQueries[i]=new TripleQuery[bridgesCount];
		bridgesCounts[i]=bridgesCount;
		for (int j=0; j<bridgesCount; j++) {
			tripleQueries[i][j]=new TripleQuery((PropertyBridge)bridges[i].get(j), t.getSubject(), t.getPredicate(), t.getObject());
		}
	}
	
}

void reducePropertyBridges() {
	if (!possible)
		return;
	// TODO: 1. compute for each triple the weakest condition wrt. all its propertyBridges
	// TODO: 2. check if the conjunction of all weakest conditions is fulfillable (possible)
	// some of it is in NodeConstraint!
	// use addTypeAssertions()
}

// can we precompute the sets of compatible tripleQuery-combinations?
// if N is the number of triples and
//    M is the average number of propertyBridges,
// then we have (combination is x*(x-1)/2, roughly x^2)
//    O( N^2 * M^2) computations for precomputing compatibility of pairs of triples

//RDFS-optimizations. TODO in next version
//void addTypeAssertions() {
//	for each variable ?x
//		build the intersection of all possible involved property domains where ?x
// is subject
//		intersect with intersection of ranges
//}

//void makeCompatibleConjunctions() {
//	Iterator it = new ConjunctionIterator((Object[][]) tripleQueries, null);
//	while (it.hasNext()) {
//		conjunctions.add(it.next());
//			//		remove if set of possible types for variables is empty
//			//		remove if there are contradictions in conditions and/or variables
//			//		simplifyConjunctions();
//	}
//}

//void simplifyConjunctions() {
//	//	if two factors in a conjunction refer to the same rows then simplify.
//	//		this means: set of table keys are the same
//	//		e.g. Tr1_Papers.PaperID = Tr2_Papers.PaperID
//	//		replace all instances of Tr2_Papers with Tr1_Papers
//}


private SQLStatementMaker getSQL(TripleQuery[] conjunction) {
	boolean possible=true;
	Database db=conjunction[0].getDatabase();
	SQLStatementMaker sql=new SQLStatementMaker(db);
	sql.setEliminateDuplicates(db.correctlyHandlesDistinct());	
	
	for (int i=0; (i<conjunction.length) && possible; i++) {
		TripleQuery t=conjunction[i];
		sql.addAliasMap(t.getPropertyBridge().getAliases());
		sql.addSelectColumns(t.getSelectColumns());
		sql.addJoins(t.getJoins());
		sql.addColumnValues(t.getColumnValues());
		// addConditions should be last, because checks if a textual token is likely to 
		// be a table based on previously in select, join and column values seen tables
		sql.addConditions(t.getConditions()); 
		sql.addColumnRenames(t.getReplacedColumns()); // ?
	}
	return sql;
}

public ClosableIterator resultTriplesIterator() {
	PQCResultIterator result = new PQCResultIterator();
	// TODO ?
	return result;
}

/** 
 * Iterator for PatternQueryCombiner results.
 * @author jgarbers
 *
 */
private class PQCResultIterator extends NiceIterator implements ClosableIterator {

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
											

	public PQCResultIterator() { // or maybe pass conjunctionsIterator as
								 // argument
		if (!possible)
			return;
		conjunction=new TripleQuery[tripleCount];
		conjunctionsIterator= new ConjunctionIterator((Object[][]) tripleQueries, conjunction);
	}
	
	public boolean hasNext() {
		if (!possible)
			return false;
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
			ConstraintHandler ch=new ConstraintHandler();
			ch.setVariableBindings(bindings);
			ch.setTripleQueryConjunction(conjunction);
			ch.setRDQLConstraints(constraints);
			ch.makeConstraints();
			if (!ch.possible)
			    continue;
			SQLStatementMaker sql=getSQL(conjunction);
			ch.addConstraintsToSQL(sql);
			resultSet = new 
				CombinedTripleResultSet(sql.getSQLStatement(),
											sql.getColumnNameNumberMap(),
											database);
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
} // class PQCResultIterator

}
