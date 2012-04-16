package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.algebra.CompatibleRelationGroup;
import de.fuberlin.wiwiss.d2rq.algebra.JoinOptimizer;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.find.URIMakerRule.URIMakerRuleChecker;


/**
 * A find query on a collection of {@link TripleRelation}s. Results are 
 * delivered as an iterator of triples. Will combine queries on multiple
 * relations into one SQL statement where possible.
 * An option for limiting the number of triples returned from each
 * {@link TripleRelation} is available.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class FindQuery {
	private final Triple triplePattern;
	private final Collection<TripleRelation> tripleRelations;
	private final int limitPerRelation;
	
	public FindQuery(Triple triplePattern, Collection<TripleRelation> tripleRelations) {
		this(triplePattern, tripleRelations, Relation.NO_LIMIT);
	}	

	public FindQuery(Triple triplePattern, Collection<TripleRelation> tripleRelations, int limit) {
		this.triplePattern = triplePattern;
		this.tripleRelations = tripleRelations;
		this.limitPerRelation = limit;
	}	

	private List<TripleRelation> selectedTripleRelations() {
		URIMakerRule rule = new URIMakerRule();
		List<TripleRelation> sortedTripleRelations = rule.sortRDFRelations(tripleRelations);
		URIMakerRuleChecker subjectChecker = rule.createRuleChecker(triplePattern.getSubject());
		URIMakerRuleChecker predicateChecker = rule.createRuleChecker(triplePattern.getPredicate());
		URIMakerRuleChecker objectChecker = rule.createRuleChecker(triplePattern.getObject());
		List<TripleRelation> result = new ArrayList<TripleRelation>();
		for (TripleRelation tripleRelation: sortedTripleRelations) {
			TripleRelation selectedTripleRelation = tripleRelation.selectTriple(triplePattern);
			if (selectedTripleRelation != null
					&& subjectChecker.canMatch(tripleRelation.nodeMaker(TripleRelation.SUBJECT))
					&& predicateChecker.canMatch(tripleRelation.nodeMaker(TripleRelation.PREDICATE))
					&& objectChecker.canMatch(tripleRelation.nodeMaker(TripleRelation.OBJECT))) {
				subjectChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.SUBJECT));
				predicateChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.PREDICATE));
				objectChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.OBJECT));
				TripleRelation r = new JoinOptimizer(selectedTripleRelation).optimize();
				if (limitPerRelation != Relation.NO_LIMIT) {
					r = r.limit(limitPerRelation);
				}
				result.add(r);
			}
		}
		return result;
	}
	
	public ExtendedIterator<Triple> iterator() {
		ExtendedIterator<Triple> result = NullIterator.emptyIterator();
		for (CompatibleRelationGroup group: 
				CompatibleRelationGroup.groupNodeRelations(selectedTripleRelations())) {
			if (!group.baseRelation().equals(Relation.EMPTY) && group.baseRelation().limit()!=0) {
				result = result.andThen(RelationToTriplesIterator.create(
						group.baseRelation(), group.bindingMakers()));
			}
		}
		return result;
	}
}
