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

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
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
 * @version $Id: FastpathEngine.java,v 1.1 2006/09/18 16:59:26 cyganiak Exp $
 */
public class FastpathEngine {
	private Pipe input;
	private Pipe output;
	private GraphD2RQ graph;
	protected Triple[] triples;
	StageInfo stageInfo;
	private boolean mayYieldResults = true;
	protected Map candidateBridgeLists; // Database -> List[] 
	protected List[] soleCandidateBridgeLists;
	boolean refersToMultipleDatabases;
	BitSet multipleDatabasesMarker;
	private boolean cancel = false;
	
	public FastpathEngine(Pipe input, Pipe output,
			GraphD2RQ graph, Mapping map,
			ExpressionSet constraints, Triple[] triples) {
		this.input = input;
		this.output = output;
		this.stageInfo = new StageInfo(map, constraints, triples);
		this.triples = new Triple[triples.length];
		// some contraints are eaten up at this point!
		// so use s.th. like a clone() and setter method at invocation time
		this.graph = graph;
		Triple[] query=stageInfo.queryTriples;
		Triple[] mostGeneral=GraphUtils.varsToANY(query);
		candidateBridgeLists=GraphUtils.makeDatabaseToPrefixedPropertyBridges(graph,mostGeneral,false);
		multipleDatabasesMarker=new BitSet(query.length);
		mayYieldResults=GraphUtils.existsEntryInEveryPosition(candidateBridgeLists,query.length,multipleDatabasesMarker);
		if (!mayYieldResults)
			return;
		refersToMultipleDatabases=candidateBridgeLists.size()>1;
		if (refersToMultipleDatabases) 
			stageInfo.setupForPartsProcessing();
		else
			stageInfo.setupForAllProcessing();
	}

	public void execute() {
		if (!mayYieldResults)
			return;
		while (this.input.hasNext()) {
			Domain inputDomain = this.input.get();
			stageInfo.vars.setInputDomain(inputDomain);
			findFrom(inputDomain,0,null);
		}
	}

	public void cancel() {
		this.cancel = true;
	}

	public void remove() {
		throw new UnsupportedOperationException();
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
		if (!refersToMultipleDatabases) {
			List[] candidates = getSoleCandidateBridgeLists();
			return makeCombinedIterator(candidates, triples,
					stageInfo.vars.allBindings, stageInfo.vars.allExpressions);
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
						graph, dbBridges, triplesCut);
				combiner.setCareForPossible(false);
				combiner.setup();
				int maxlen = combiner.possibleLength();
				for (int len = maxlen; len > 0; len--) {
					// either full length or devide where options from other
					// databases
					if (len == maxlen
							|| multipleDatabasesMarker.get(nextToFind + len)) {
						RDFRelation[][] tripleQueries = new RDFRelation[len][];
						System.arraycopy(combiner.tripleQueries, 0,
								tripleQueries, 0, len);
						int partEnd = nextToFind + len - 1;
						PQCResultIterator item = makeCombinedIterator(
								tripleQueries,
								stageInfo.vars.partBindings[nextToFind][partEnd],
								stageInfo.vars.partExpressions[nextToFind][partEnd]);
						if (item != null)
							ret = ret.andThen(item);
					}
				}
			}
			return ret;
		}
	}

	private PQCResultIterator makeCombinedIterator(List[] candidates, Triple[] triples, VariableBindings variableBindings, Collection constraints) {
		PatternQueryCombiner combiner = new PatternQueryCombiner(graph, candidates, triples); 
		combiner.setup();
		if (!combiner.possible)
			return null;
		return makeCombinedIterator(combiner.tripleQueries,variableBindings,constraints);
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
			Pattern p = stageInfo.compiled[index];
			possible = p.match(domain, resultTriples[resultTripleIndex]);
			if (possible && stageInfo.guards!=null) {
				stageInfo.guards[index].evalBool(domain);
			}
		}
		if (possible && stageInfo.guard!=null && to>=triples.length-1) {
			possible=stageInfo.guard.evalBool(domain);
		}
		return possible;
	}

	/**
	 * Updates <code>triples</code> with a new set of bindings from the previous stage.
	 * @param inputDomain for each variable number a binding entry
	 */
	private void updateTriplesWithDomain(Domain inputDomain, int nextToFind) {
		// TODO can be more efficient, just check varInfo.boundDomainIndexToShared
		int tripleCount = stageInfo.compiled.length;
		// Triple[] triples = new Triple[tripleCount];
		for (int index = nextToFind; index < tripleCount; index++) {
			Pattern p = stageInfo.compiled[index];
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
