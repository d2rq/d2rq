/*
 * $Id: PatternQueryCombiner.java,v 1.1 2005/03/02 09:23:53 garbers Exp $
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


// Contract: after Constructor
//           tirst call setup()
//           then  call resultTriplesIterator()

// assume: all PropertyBridges refer to same database
class PatternQueryCombiner { // jg. reference: QueryCombiner
	private boolean possible=true; // flag. false shows contradiction.
	protected Database database;
	protected GraphD2RQ graph;
	protected Triple [] triples;
	protected int tripleCount;
	protected Set variables; // set of Node
	protected Set sharedVariables; // set of Node
//	protected TablePrefixer[][] prefixers;
	protected ArrayList[] bridges; // holds for each triple its a priory
										  // appliable bridges
	protected int[] bridgesCounts; // holds for each triple its number of
								   // bridges resp. tripleQueries
	protected TripleQuery[][] tripleQueries; // holds for each triple its
											 // disjunctive SQL-TripleQuery
											 // Objects
//	protected ArrayList conjunctions = new ArrayList(10);
	
public PatternQueryCombiner( GraphD2RQ graph, Mapping map, ExpressionSet constraints, Triple [] triples ) {
	this.graph=graph;
	// jg: maybe use the compiled patterns, that super produces?
	tripleCount=triples.length;
	this.triples=triples; // new Triple[tripleCount]; // we put the more
						  // instanciated triples here in run()
}	

void makeStores() {
	if (!possible)
		return;
	variables=new HashSet();
	sharedVariables=new HashSet();
	bridges=new ArrayList[tripleCount];
	tripleQueries=new TripleQuery[tripleCount][];
	bridgesCounts=new int[tripleCount];
}

void setup() {
	if (!possible)
		return;
	makeStores();
	makeVariables();
	makePropertyBridges(); // -> setsOfPossiblePropertyBridges
	reducePropertyBridges();
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

void makeVariables() {
	if (!possible)
		return;
	for (int i=0; i<tripleCount; i++) {
		Triple t=triples[i];
		for (int j=0; j<3; j++) {
			Node n=null;
			switch (j) {
			case 0: n=t.getSubject(); break;
			case 1: n=t.getPredicate(); break;
			case 2: n=t.getObject(); break;
			}
			if (variables.contains(n)) {
				sharedVariables.add(n);
			} else {
				variables.add(n);
			}
		}
	}
}


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
			// renameColumns(); // two triples generally have nothing in common
			// so rename tables
			// e.g. with "Tr<n>_ where <n> is the number of the triple in the
			// over all query
		}
	}
}

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
	// use bridge.couldFit(Triple,queryContext) ?
	// use sharedVariables to constrain involved Nodes
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

// Pull in iterator
// 

private SQLStatementMaker getSQL(TripleQuery[] conjunction, TablePrefixer[] prefixerConjunction) {
	boolean possible=true;
	Database db=conjunction[0].getDatabase();
	SQLStatementMaker result=new SQLStatementMaker(db);
	result.setEliminateDuplicates(true);	
	
	for (int i=0; (i<conjunction.length) && possible; i++) {
		TripleQuery t=conjunction[i];
		result.addSelectColumns(t.getSelectColumns());
		result.addJoins(t.getJoins());
		result.addColumnValues(t.getColumnValues());
		// addConditions should be last, because checks if a textual token is likely to 
		// be a table based on previously in select, join and column values seen tables
		result.addConditions(t.getConditions()); 
		result.addColumnRenames(t.getReplacedColumns()); // ?
	}
	return result;
}

public ClosableIterator resultTriplesIterator() {
	PQCResultIterator result = new PQCResultIterator();
	// TODO 
	return result;
}

private class PQCResultIterator extends NiceIterator implements ClosableIterator {
	protected TripleQuery[] conjunction; // next conjunction to be processed
	protected TablePrefixer[] prefixerConjunction;
	protected ConjunctionIterator conjunctionsIterator;
//	protected ConjunctionIterator prefixerConjunctionIterator;
	protected Triple[] prefetchedResult=null;
	protected boolean didPrefetch=false;
	CombinedTripleResultSet resultSet=null; // iterator that returns triple
											// arrays for database rows

	public PQCResultIterator() { // or maybe pass conjunctionsIterator as
								 // argument
		if (!possible)
			return;
		conjunction=new TripleQuery[tripleCount];
		conjunctionsIterator= new ConjunctionIterator((Object[][]) tripleQueries, conjunction);
//		prefixerConjunctionIterator = new ConjunctionIterator((Object[][]) prefixers, prefixerConjunction);
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
			SQLStatementMaker sql=getSQL(conjunction, prefixerConjunction);
			resultSet = new 
				CombinedTripleResultSet(sql.getSQLStatement(),
											sql.getColumnNameNumberMap(),
											database);
			resultSet.setTripleMakers(conjunction);
		} // enless while loop
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.util.iterator.ClosableIterator#close()
	 */
	public void close() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		// TODO Auto-generated method stub
		
	}
} // class PQCResultIterator

}
