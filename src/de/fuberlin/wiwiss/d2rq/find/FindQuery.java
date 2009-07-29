package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_ANY;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.algebra.CompatibleRelationGroup;
import de.fuberlin.wiwiss.d2rq.algebra.JoinOptimizer;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.find.URIMakerRule.URIMakerRuleChecker;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;


/**
 * A find query on a collection of {@link TripleRelation}s. Results are 
 * delivered as an iterator of triples. Will combine queries on multiple
 * relations into one SQL statement where possible.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindQuery.java,v 1.22 2009/07/29 12:03:53 fatorange Exp $
 */
public class FindQuery {
	private final Triple triplePattern;
	private final Collection tripleRelations;
	private final boolean serveVocabulary;
	private final boolean checkPredicates;
	private final Model vocabularyModel;
	
	public FindQuery(Triple triplePattern, Collection tripleRelations, boolean serveVocabulary, boolean checkPredicates, Model vocabularyModel) {
		this.triplePattern = triplePattern;
		this.tripleRelations = tripleRelations;
		this.serveVocabulary = serveVocabulary;
		this.checkPredicates = checkPredicates;
		this.vocabularyModel = vocabularyModel;
	}
	
	public FindQuery(Triple triplePattern, Collection tripleRelations) {
		this(triplePattern, tripleRelations, false, true, null);
	}	

	private List selectedTripleRelations() {
		URIMakerRule rule = new URIMakerRule();
		List sortedTripleRelations = rule.sortRDFRelations(tripleRelations);
		URIMakerRuleChecker subjectChecker = rule.createRuleChecker(triplePattern.getSubject());
		URIMakerRuleChecker predicateChecker = rule.createRuleChecker(triplePattern.getPredicate());
		URIMakerRuleChecker objectChecker = rule.createRuleChecker(triplePattern.getObject());
		List result = new ArrayList();
		Iterator it = sortedTripleRelations.iterator();
		while (it.hasNext()) {
			TripleRelation tripleRelation = (TripleRelation) it.next();
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
	
	public ExtendedIterator iterator() {
		ExtendedIterator result = NullIterator.emptyIterator();
		
		/* Answer from vocabulary model */
		if (serveVocabulary && vocabularyModel != null) {
			result = result.andThen(vocabularyModel.getGraph().find(triplePattern));
		}

		/* Answer from database */
		Iterator it = CompatibleRelationGroup.groupTripleRelations(
				selectedTripleRelations()).iterator();
		while (it.hasNext()) {
			CompatibleRelationGroup group = (CompatibleRelationGroup) it.next();
			if (!group.baseRelation().equals(Relation.EMPTY) && group.baseRelation().limit()!=0) {
				result = result.andThen(
						RelationToTriplesIterator.create(
								group.baseRelation(), group.tripleMakers()));
			}
		}
		return result;
	}
}
