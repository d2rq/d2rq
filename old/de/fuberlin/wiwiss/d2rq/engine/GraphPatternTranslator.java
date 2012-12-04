package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.d2rq.algebra.NodeRelation;
import org.d2rq.algebra.NodeRelationUtil;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.op.mutators.OpUtil;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.util.Context;


/**
 * Matches a BGP against a collection of {@link TripleRelation}s
 * and returns a collection of {@link NodeRelation}s.
 * 
 * The node relations produce the same bindings that one would
 * get from matching the BGP against the materialized triples
 * produced by the triple relations.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class GraphPatternTranslator {
	private final List<Triple> triplePatterns;
	private final Collection<TripleRelation> tripleRelations;
	private final Context context;
	
	public GraphPatternTranslator(List<Triple> triplePatterns, 
			Collection<TripleRelation> tripleRelations, 
			Context context) 
	{
		this.triplePatterns = triplePatterns;
		this.tripleRelations = tripleRelations;
		this.context = context;
	}

	/**
	 * @return A list of {@link NodeRelation}s
	 */
	public List<NodeRelation> translate() {
		Iterator<Triple> it = triplePatterns.iterator();
		List<CandidateList> candidateLists = new ArrayList<CandidateList>(triplePatterns.size());
		int index = 1;
		while (it.hasNext()) {
			Triple triplePattern = (Triple) it.next();
			// use always index
			// index is now unique over one sparql-query-execution
			CandidateList candidates = new CandidateList(
					triplePattern, triplePatterns.size() > 1, index);
			if (candidates.isEmpty()) {
				return Collections.<NodeRelation>emptyList();
			}
			candidateLists.add(candidates);
			index++;
		}
		Collections.sort(candidateLists);
		List<TripleRelationJoiner> joiners = new ArrayList<TripleRelationJoiner>();
		joiners.add(TripleRelationJoiner.create(context));
		for (CandidateList candidates: candidateLists) {
			List<TripleRelationJoiner> nextJoiners = new ArrayList<TripleRelationJoiner>();
			for (TripleRelationJoiner joiner: joiners) {
				nextJoiners.addAll(joiner.joinAll(candidates.triplePattern(), candidates.all()));
			}
			joiners = nextJoiners;
		}
		List<NodeRelation> results = new ArrayList<NodeRelation>(joiners.size());
		for (TripleRelationJoiner joiner: joiners) {
			NodeRelation nodeRelation = joiner.toNodeRelation();
			if (!OpUtil.isEmpty(nodeRelation.getBaseTabular())) {
				results.add(nodeRelation);
			}
		}
		return results;
	}

	private class CandidateList implements Comparable<CandidateList> {
		private final Triple triplePattern;
		private final List<NodeRelation> candidates;
		CandidateList(Triple triplePattern, boolean useIndex, int index) {
			this.triplePattern = triplePattern;
			List<NodeRelation> matches = findMatchingTripleRelations(triplePattern);
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
		List<NodeRelation> all() {
			return candidates;
		}
		public int compareTo(CandidateList other) {
			CandidateList otherList = (CandidateList) other;
			if (candidates.size() < otherList.candidates.size()) {
				return -1;
			}
			if (candidates.size() > otherList.candidates.size()) {
				return 1;
			}
			return 0;
		}
		private List<NodeRelation> findMatchingTripleRelations(Triple triplePattern) {
			List<NodeRelation> results = new ArrayList<NodeRelation>();
			for (TripleRelation tripleRelation: tripleRelations) {
				TripleRelation selected = tripleRelation.selectTriple(triplePattern);
				if (selected == null) continue;
				results.add(selected);
			}
			return results;
		}
		private List<NodeRelation> prefixTripleRelations(List<NodeRelation> tripleRelations, int index) {
			List<NodeRelation> results = new ArrayList<NodeRelation>(tripleRelations.size());
			for (NodeRelation tripleRelation: tripleRelations) {
				results.add(NodeRelationUtil.renameWithPrefix(tripleRelation, index));
			}
			return results;
		}
		public String toString() {
			return "CandidateList(" + triplePattern + ")[" + candidates + "]";
		}
	}
}
