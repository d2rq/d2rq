package de.fuberlin.wiwiss.d2rq.fastpath;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.Pattern;
import com.hp.hpl.jena.graph.query.Pipe;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * TODO instanciate just one PatternQueryCombiner? it could do some caching
 * 		or leave the caching for graph? e.g. triple -> list of bridges
 * 
 * TODO keep just one instance of PatternQueryCombiner and update Property Bridges
 *		only when updated with previous stage (see varInfo.boundDomainIndexToShared)
 *
 * @author jg
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FastpathEngine.java,v 1.3 2006/09/28 12:17:43 cyganiak Exp $
 */
public class FastpathEngine {
	private final Pipe input;
	private final Pipe output;
	private final Collection rdfRelations;
	private final Triple[] triples;
	private final StageInfo stageInfo;
	private final Map candidateBridgeLists; // Database -> List[] 
	private final BitSet multipleDatabasesMarker;
	private final boolean mayYieldResults;

	private List[] soleCandidateBridgeLists;
	private boolean cancel = false;
	
	public FastpathEngine(Pipe input, Pipe output,
			Collection rdfRelations, Mapping map,
			ExpressionSet constraints, Triple[] triples) {
		this.input = input;
		this.output = output;
		this.triples = new Triple[triples.length];
		// some contraints are eaten up at this point!
		// so use s.th. like a clone() and setter method at invocation time
		this.rdfRelations = rdfRelations;
		this.candidateBridgeLists = GraphUtils.makeDatabaseToPrefixedPropertyBridges(
				this.rdfRelations, GraphUtils.varsToANY(triples), false);
		this.stageInfo = new StageInfo(map, constraints, triples, multipleDatabases());
		this.multipleDatabasesMarker = new BitSet(triples.length);
		this.mayYieldResults = GraphUtils.existsEntryInEveryPosition(
				this.candidateBridgeLists, triples.length,
				this.multipleDatabasesMarker);
	}

	public void execute() {
		if (!this.mayYieldResults) {
			return;
		}
		while (this.input.hasNext()) {
			Domain inputDomain = this.input.get();
			findFrom(inputDomain, 0, null);
		}
	}

	public void cancel() {
		this.cancel = true;
	}

	private boolean multipleDatabases() {
		return this.candidateBridgeLists.size() > 1;
	}
	
	private List[] getSoleCandidateBridgeLists() {
		if (soleCandidateBridgeLists==null) {
			Iterator it=candidateBridgeLists.values().iterator();
			List[] first=null;
			if (it.hasNext())
				first=(List[])it.next();
			if (it.hasNext())
				throw new RuntimeException("more than one element");
			soleCandidateBridgeLists=first;
		}
		return soleCandidateBridgeLists;
	}

	/**
	 * Sets up {@link PatternQueryCombiner} and returns its
	 * resultTriplesIterator. Passes stage information to the
	 * PatternQueryCombiner.
	 * 
	 * @param triples
	 *            identical to this.triples
	 * @return an iterator. Each result is a possible and full instanciation of
	 *         triples according to D2RQ.
	 */
	private ClosableIterator resultIteratorForTriplePattern(Triple[] triples,
			int nextToFind, Object aboutPrevious) {
		if (!multipleDatabases()) {
			List[] candidates = getSoleCandidateBridgeLists();
			return makeCombinedIterator(candidates, triples,
					this.stageInfo.vars().allBindings, this.stageInfo.vars().allExpressions);
		} else {
			Map reducedCandidateBridgeLists;
			if (aboutPrevious == null)
				reducedCandidateBridgeLists = candidateBridgeLists;
			else {
				ConnectedDB previousDatabase = (ConnectedDB) aboutPrevious;
				reducedCandidateBridgeLists = new HashMap();
				reducedCandidateBridgeLists.putAll(candidateBridgeLists);
				reducedCandidateBridgeLists.remove(previousDatabase);
			}
			Map cuttedCandidateBridgeLists = new HashMap();
			GraphUtils.cutLists(reducedCandidateBridgeLists, nextToFind,
					cuttedCandidateBridgeLists, true);
			ExtendedIterator ret = NiceIterator.emptyIterator();
			Iterator it = cuttedCandidateBridgeLists.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry e = (Map.Entry) it.next();
				List[] dbBridges = (List[]) e.getValue();
				Triple[] triplesCut = new Triple[dbBridges.length];
				System.arraycopy(triples, nextToFind, triplesCut, 0,
						triplesCut.length);
				PatternQueryCombiner combiner = new PatternQueryCombiner(
						this.rdfRelations, dbBridges, triplesCut, false);
				RDFRelation[][] tripleQueries = combiner.tripleQueries();
				int maxlen = combiner.possibleLength();
				for (int len = maxlen; len > 0; len--) {
					// either full length or devide where options from other
					// databases
					if (len == maxlen
							|| multipleDatabasesMarker.get(nextToFind + len)) {
						RDFRelation[][] tripleQueriesPart = new RDFRelation[len][];
						for (int i = 0; i < len; i++) {
							tripleQueriesPart[i] = tripleQueries[i];
						}
						int partEnd = nextToFind + len - 1;
						PQCResultIterator item = makeCombinedIterator(
								tripleQueriesPart,
								this.stageInfo.vars().partBindings[nextToFind][partEnd],
								this.stageInfo.vars().partExpressions[nextToFind][partEnd]);
						if (item != null)
							ret = ret.andThen(item);
					}
				}
			}
			return ret;
		}
	}

	private PQCResultIterator makeCombinedIterator(List[] candidates, Triple[] triples, VariableBindings variableBindings, Collection constraints) {
		PatternQueryCombiner combiner = new PatternQueryCombiner(
				this.rdfRelations, candidates, triples, true); 
		if (!combiner.possible()) {
			return null;
		}
		return makeCombinedIterator(combiner.tripleQueries(),variableBindings,constraints);
	}

	private PQCResultIterator makeCombinedIterator(RDFRelation[][] tripleQueries, VariableBindings variableBindings, Collection constraints) {
		PQCResultIterator it=new PQCResultIterator(tripleQueries, 
				variableBindings, constraints);
		return it;
	}


	/**
	 * recursively calls findFrom until the triples.length is reached.
	 */
	private void findFrom(Domain domain, int nextToFind, Object aboutPrevious) {
		// the following two methods should be called in sync
		// because triples is often overridden
		updateTriplesWithDomain(domain, nextToFind);
		// get solutions from hook method and put in sink
		ClosableIterator it = resultIteratorForTriplePattern(triples,nextToFind, aboutPrevious);
		if (it==null)
			return;
		while (it.hasNext() && !this.cancel) {
			IteratorResult res=(IteratorResult)it.next();
			Object aboutNext=res.about; 
			Triple[] resultTriples = res.triples;
			int resultLen=resultTriples.length;
			if (resultTriples==null || resultLen==0)
				throw new RuntimeException("Iterator returns resultTriple array of length 0.");
			int next2=nextToFind+resultLen;
			boolean possible = true;
			// TODO check indices
			Domain copy=domain.copy();
			possible=matchAndEvalGuards(copy,resultTriples,nextToFind,next2-1);
			if (possible) {
				if (next2==triples.length)
					this.output.put(copy);
				else
					findFrom(copy,next2,aboutNext);
			}
		}
		it.close();
	}

	/**
	 * Matches domain with resultTriples.
	 * Thereby modifies domain.
	 * @param domain
	 * @param resultTriples
	 * @param from
	 * @param to
	 */
	private boolean matchAndEvalGuards(Domain domain, Triple[] resultTriples, int from, int to) {
		boolean possible = true;
		//  evaluate all matches and guards at once
		int resultTripleIndex=0;
		for (int index = from; possible && (index <= to); index++, resultTripleIndex++) {
			Pattern p = this.stageInfo.compiled(index);
			possible = p.match(domain, resultTriples[resultTripleIndex]);
			if (possible) {
				possible = this.stageInfo.evalGuard(index, domain);
			}
		}
		if (possible && to >= this.triples.length-1) {
			possible = this.stageInfo.evalGuard(domain);
		}
		return possible;
	}

	/**
	 * Updates <code>triples</code> with a new set of bindings from the previous stage.
	 * @param inputDomain for each variable number a binding entry
	 */
	private void updateTriplesWithDomain(Domain inputDomain, int nextToFind) {
		// TODO can be more efficient, just check varInfo.boundDomainIndexToShared
		int tripleCount = this.triples.length;
		// Triple[] triples = new Triple[tripleCount];
		for (int index = nextToFind; index < tripleCount; index++) {
			Pattern p = stageInfo.compiled(index);
			triples[index] = p.asTripleMatch(inputDomain).asTriple();
		}
	}

	public static class IteratorResult {
		public Triple[] triples;
		public Object about;
		public IteratorResult(Triple[] triples, Object about) {
			this.triples=triples;
			this.about=about;
		}
	}
}
