package de.fuberlin.wiwiss.d2rq.fastpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.algebra.JoinOptimizer;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.find.FindQuery;

/** 
 * Handles a triple pattern query on a D2RQ mapped database.
 * (Current)Assumption: all PropertyBridges refer to same database.
 * Contract: after Constructor, first call setup() 
 * then  call resultTriplesIterator().
 * <p>
 * This class does not need to know anything about D2RQPatternStage, except
 * it's {@link VariableBindings} semantics.
 * <p>
 * It seems that some of the information computed here could be reused in successive
 * calls from D2RQPatternStage2. On the other hand, lots of preprocessing is
 * useless, if there are (Bound) variables for predicates.
 * 
 * @author jgarbers
 * @version $Id: PatternQueryCombiner.java,v 1.3 2006/09/28 12:17:43 cyganiak Exp $
 * @see FindQuery
 */
public class PatternQueryCombiner {
	private final boolean careForPossible;
	private final Collection rdfRelations;
	private final Triple [] triples; // nodes are ANY or fixed
	private final int tripleCount;

	/** holds for each triple a list of bridge candidates */
	private final List[] candidateBridges; // may be longer than triples[] (for speed somewhere else)

	/** holds for each triple its 'a priory' appliable bridges */
	private List[] bridges; 

	/** holds for each triple its number of bridges or tripleQueries */
	private int[] bridgesCounts; 

	/** holds for each triple its disjunctive SQL-TripleQuery Objects */
	private RDFRelation[][] tripleQueries; 

	/** if false then contradiction, no SQL query necessary. */
	private boolean possible=true;
	
	public PatternQueryCombiner(Collection rdfRelations, List[] candidateBridges, Triple [] triples, boolean careForPossible) {
		// alternative modelling
		this.rdfRelations = rdfRelations;
		this.candidateBridges=candidateBridges;
		if (candidateBridges == null) {
			this.tripleCount=triples.length;
		} else {
			this.tripleCount=Math.min(triples.length,candidateBridges.length);
		}
		this.triples=triples;
		this.careForPossible = careForPossible;
	}	

	public RDFRelation[][] tripleQueries() {
		if (!cont())
			return null;
		this.bridges = new ArrayList[this.tripleCount];
		this.bridgesCounts = new int[this.tripleCount];
		this.tripleQueries = new RDFRelation[this.tripleCount][];
		makePropertyBridges(); // -> setsOfPossiblePropertyBridges
		// reducePropertyBridges();
		makeTripleQueries();
		// TODO handle bridges from multiple databases correctly
		// database=((PropertyBridge)bridges[0].get(0)).getDatabase();
		// makeCompatibleConjunctions(); // can be returned by an iterator
		// conjunctionResultIterator(); // pulls answers from database
		//	for each result
		//		get query variables from the
		// triple-disjunctions
		//		check conditions that are not checked by SQL
		// (stage)
		//		push into iterator
		return this.tripleQueries;
	}
	
	public boolean possible() {
		return this.possible;
	}
	
	public int possibleLength() {
		for (int i=0; i<bridgesCounts.length; i++)
			if (bridgesCounts[i]<1)
				return i+1;
		return bridgesCounts.length;
	}

	private boolean cont() {
		return possible || !careForPossible;
	}

	/** 
	 * Creates copies of the property bridges that could fit the <code>triples</code>.
	 * Two triples that are combined with AND generally have nothing in common,
	 * so we create individual instances of the bridges and systematically rename 
	 * their tables. We prefix each Table with "T<n>_ where <n> is the index of the
	 *  triple in the overall query.
	 */
	private void makePropertyBridges() {
		if (this.candidateBridges==null) {
			this.bridges = GraphUtils.makePrefixedPropertyBridges(
					this.rdfRelations, this.triples);
		} else {
			this.bridges = GraphUtils.refinePropertyBridges(
					this.candidateBridges, this.triples);
		}
		if (this.bridges == null) {
			this.possible = false;
		}
	}

	/** 
	 * Creates a {@link TripleSelection} for each {@link TripleRelation}.
	 * As a side effect we also set <code>bridgesCounts</code>.
	 */
	private void makeTripleQueries() {
		if (!cont())
			return;
		for (int i=0; i<tripleCount; i++) {
			Triple t=triples[i];
			int bridgesCount=bridges[i].size();
			tripleQueries[i]=new RDFRelation[bridgesCount];
			bridgesCounts[i]=bridgesCount;
			for (int j=0; j<bridgesCount; j++) {
				tripleQueries[i][j]=new JoinOptimizer(
						((RDFRelation)bridges[i].get(j)).selectTriple(t)).optimize();
			}
		}
	}
}