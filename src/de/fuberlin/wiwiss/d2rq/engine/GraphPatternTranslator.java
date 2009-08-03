package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;

public class GraphPatternTranslator {
	private final List triplePatterns;
	private final Collection tripleRelations;
	boolean useAllOptimizations;
	
	public GraphPatternTranslator(List triplePatterns, Collection tripleRelations, boolean useAllOptimizations) 
	{
		this.triplePatterns = triplePatterns;
		this.tripleRelations = tripleRelations;
		this.useAllOptimizations = useAllOptimizations;
	}

	/**
	 * @return A list of {@link NodeRelation}s
	 */
	public List translate() {
		if (triplePatterns.isEmpty()) {
			return Collections.singletonList(NodeRelation.TRUE);
		}
		Iterator it = triplePatterns.iterator();
		List candidateLists = new ArrayList(triplePatterns.size());
		int index = 1;
		while (it.hasNext()) {
			Triple triplePattern = (Triple) it.next();
			// use always index
			// index is now unique over one sparq-query-execution
			CandidateList candidates = new CandidateList(
					triplePattern, triplePatterns.size() > 1, index);
			if (candidates.isEmpty()) {
				return Collections.EMPTY_LIST;
			}
			candidateLists.add(candidates);
			// inc value
			index++;
		}
		Collections.sort(candidateLists);
		List joiners = new ArrayList();
		joiners.add(TripleRelationJoiner.create(this.useAllOptimizations));
		it = candidateLists.iterator();
		while (it.hasNext()) {
			CandidateList candidates = (CandidateList) it.next();
			List nextJoiners = new ArrayList();
			Iterator it2 = joiners.iterator();
			while (it2.hasNext()) {
				TripleRelationJoiner joiner = (TripleRelationJoiner) it2.next();
				nextJoiners.addAll(joiner.joinAll(candidates.triplePattern(), candidates.all()));
			}
			joiners = nextJoiners;
		}
		List results = new ArrayList(joiners.size());
		it = joiners.iterator();
		while (it.hasNext()) {
			TripleRelationJoiner joiner = (TripleRelationJoiner) it.next();
			NodeRelation nodeRelation = joiner.toNodeRelation();
			if (!nodeRelation.baseRelation().equals(Relation.EMPTY) || !useAllOptimizations)
				results.add(nodeRelation);
		}
		return results;
	}

	private class CandidateList implements Comparable {
		private final Triple triplePattern;
		private final List candidates;
		CandidateList(Triple triplePattern, boolean useIndex, int index) {
			this.triplePattern = triplePattern;
			List matches = findMatchingTripleRelations(triplePattern);
			if (useIndex) {
				candidates = prefixTripleRelations(matches, index);
			} else {
				candidates = matches;
			}
		}
		boolean isEmpty() {
			return candidates.isEmpty();
		}
		Triple triplePattern() {
			return triplePattern;
		}
		List all() {
			return candidates;
		}
		public int compareTo(Object other) {
			CandidateList otherList = (CandidateList) other;
			if (candidates.size() < otherList.candidates.size()) {
				return -1;
			}
			if (candidates.size() > otherList.candidates.size()) {
				return 1;
			}
			return 0;
		}
		private List findMatchingTripleRelations(Triple triplePattern) {
			List results = new ArrayList();
			Iterator it = tripleRelations.iterator();
			while (it.hasNext()) {
				TripleRelation tripleRelation = (TripleRelation) it.next();
				TripleRelation selected = tripleRelation.selectTriple(triplePattern);
				if (selected == null) continue;
				results.add(selected);
			}
			return results;
		}
		private List prefixTripleRelations(List tripleRelations, int index) {
			List results = new ArrayList(tripleRelations.size());
			Iterator it = tripleRelations.iterator();
			while (it.hasNext()) {
				TripleRelation tripleRelation = (TripleRelation) it.next();
				results.add(tripleRelation.withPrefix(index));
			}
			return results;
		}
		public String toString() {
			return "CandidateList(" + triplePattern + ")[" + candidates + "]";
		}
	}
}
