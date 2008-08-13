package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindQuery.java,v 1.17 2008/08/13 06:35:00 cyganiak Exp $
 */
public class FindQuery {
	private final Triple triplePattern;
	private final Collection tripleRelations;
	
	public FindQuery(Triple triplePattern, Collection tripleRelations) {
		this.triplePattern = triplePattern;
		this.tripleRelations = tripleRelations;
	}

	private List selectedTripleRelations() {
		URIMakerRule rule = new URIMakerRule();
		List sortedTripleRelations = rule.sortRDFRelations(tripleRelations);
		URIMakerRuleChecker subjectChecker = rule.createRuleChecker(triplePattern.getSubject());
		URIMakerRuleChecker objectChecker = rule.createRuleChecker(triplePattern.getObject());
		List result = new ArrayList();
		Iterator it = sortedTripleRelations.iterator();
		while (it.hasNext()) {
			TripleRelation tripleRelation = (TripleRelation) it.next();
			TripleRelation selectedTripleRelation = tripleRelation.selectTriple(triplePattern);
			if (selectedTripleRelation != null
					&& subjectChecker.canMatch(tripleRelation.nodeMaker(TripleRelation.SUBJECT))
					&& objectChecker.canMatch(tripleRelation.nodeMaker(TripleRelation.OBJECT))) {
				subjectChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.SUBJECT));
				objectChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.OBJECT));
				result.add(new JoinOptimizer(selectedTripleRelation).optimize());
			}
		}
		return result;
	}
	
	public ExtendedIterator iterator() {
		ExtendedIterator result = NullIterator.emptyIterator();
		Iterator it = CompatibleRelationGroup.groupTripleRelations(
				selectedTripleRelations()).iterator();
		while (it.hasNext()) {
			CompatibleRelationGroup group = (CompatibleRelationGroup) it.next();
			if (!group.baseRelation().equals(Relation.EMPTY)) {
				result = result.andThen(
						RelationToTriplesIterator.create(
								group.baseRelation(), group.tripleMakers()));
			}
		}
		return result;
	}
}
