package de.fuberlin.wiwiss.d2rq.fastpath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.Pipe;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;

/**
 * TODO The entire package is obsolete now thanks to {@link QueryEngineD2RQ} and should be removed.
 * 
 * TODO instanciate just one PatternQueryCombiner? it could do some caching
 * 		or leave the caching for graph? e.g. triple -> list of bridges
 * 
 * TODO keep just one instance of PatternQueryCombiner and update Property Bridges
 *		only when updated with previous stage (see varInfo.boundDomainIndexToShared)
 *
 * @author jg
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FastpathEngine.java,v 1.7 2008/08/12 06:47:36 cyganiak Exp $
 */
public class FastpathEngine {
	private final Pipe input;
	private final Pipe output;
	private final Collection tripleRelations;
	private final int tripleCount;
	private final StageInfo stageInfo;
	private boolean cancel = false;
	
	public FastpathEngine(Pipe input, Pipe output,
			Collection rdfRelations, Mapping map,
			ExpressionSet constraints, Triple[] triples) {
		this.input = input;
		this.output = output;
		this.tripleRelations = rdfRelations;
		this.stageInfo = new StageInfo(map, constraints, triples);
		this.tripleCount = triples.length;
	}

	public void execute() {
		// TODO We could already know that there will be no solution
		// if the triples don't match here; we don't need to exhaust
		// the input then
		while (this.input.hasNext()) {
			Domain inputDomain = this.input.get();
			findFrom(inputDomain);
		}
	}

	public void cancel() {
		this.cancel = true;
	}

	/**
	 * recursively calls findFrom until the triples.length is reached.
	 */
	private void findFrom(Domain domain) {
		Triple[] triples = new Triple[this.tripleCount];
		for (int i = 0; i < triples.length; i++) {
			triples[i] = this.stageInfo.compiled(i).asTripleMatch(domain).asTriple();
		}
		TripleRelation[][] tripleQueries = candidateRelationsForEachTriple(triples); 
		if (tripleQueries == null) {
			return;
		}
		ClosableIterator it = new PQCResultIterator(
				tripleQueries, 
				this.stageInfo.vars().allBindings, 
				this.stageInfo.vars().allExpressions);
		while (it.hasNext() && !this.cancel) {
			Triple[] resultTriples = (Triple[]) it.next();
			Domain copy = domain.copy();
			if (matchAndEvalGuards(copy, resultTriples)) {
				this.output.put(copy);
			}
		}
		it.close();
	}

	public TripleRelation[][] candidateRelationsForEachTriple(Triple[] triples) {
		TripleRelation[][] results = new TripleRelation[triples.length][];
		for (int i = 0; i < triples.length; i++) {
			List candidates = findAndPrefixCandidates(triples[i], i);
			results[i] = (TripleRelation[]) candidates.toArray(
					new TripleRelation[candidates.size()]);
		}
		return results;
	}

	private List findAndPrefixCandidates(Triple triple, int prefix) {
		List results = new ArrayList();
		Iterator it = this.tripleRelations.iterator();
		while (it.hasNext()) {
			TripleRelation bridge = (TripleRelation) it.next();
			TripleRelation candidate = bridge.selectTriple(triple);
			if (candidate == null) continue;
			results.add(candidate.withPrefix(prefix));
		}
		return results;
	}

	/**
	 * Matches domain with resultTriples.
	 * Thereby modifies domain.
	 * @param domain
	 * @param resultTriples
	 */
	private boolean matchAndEvalGuards(Domain domain, Triple[] resultTriples) {
		for (int i = 0; i < resultTriples.length; i++) {
			if (!this.stageInfo.compiled(i).match(
					domain, resultTriples[i])) {
				return false;
			}
		}
		return this.stageInfo.evalGuard(domain);
	}
}
