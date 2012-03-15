package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
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
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class FindQuery {
	private final Triple triplePattern;
	private final Collection<TripleRelation> tripleRelations;
	private final boolean serveVocabulary;
	private final boolean checkPredicates;
	private final Model vocabularyModel;
	
	public FindQuery(Triple triplePattern, Collection<TripleRelation> tripleRelations, boolean serveVocabulary, boolean checkPredicates, Model vocabularyModel) {
		this.triplePattern = triplePattern;
		this.tripleRelations = tripleRelations;
		this.serveVocabulary = serveVocabulary;
		this.checkPredicates = checkPredicates;
		this.vocabularyModel = vocabularyModel;
	}
	
	public FindQuery(Triple triplePattern, Collection<TripleRelation> tripleRelations) {
		this(triplePattern, tripleRelations, false, true, null);
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
					&& (!checkPredicates || predicateChecker.canMatch(tripleRelation.nodeMaker(TripleRelation.PREDICATE)))
					&& objectChecker.canMatch(tripleRelation.nodeMaker(TripleRelation.OBJECT))) {
				subjectChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.SUBJECT));
				if (checkPredicates)
					predicateChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.PREDICATE));
				objectChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.OBJECT));
				result.add(new JoinOptimizer(selectedTripleRelation).optimize());
			}
		}
		return result;
	}
	
	public ExtendedIterator<Triple> iterator() {
		ExtendedIterator<Triple> result = NullIterator.emptyIterator();
		
		/* Answer from vocabulary model */
		if (serveVocabulary && vocabularyModel != null) {
			result = result.andThen(vocabularyModel.getGraph().find(triplePattern));
		}

		/* Answer from database */
		for (CompatibleRelationGroup group: 
				CompatibleRelationGroup.groupNodeRelations(selectedTripleRelations())) {
			if (!group.baseRelation().equals(Relation.EMPTY) && group.baseRelation().limit()!=0) {
				result = result.andThen(
						RelationToTriplesIterator.create(
								group.baseRelation(), group.bindingMakers()));
			}
		}
		return result;
	}
}
