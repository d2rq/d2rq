package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.algebra.JoinOptimizer;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.find.URIMakerRule.URIMakerRuleChecker;
import de.fuberlin.wiwiss.d2rq.sql.RelationToTriplesIterator;


/**
 * A find query on a list of property bridges. Results are delivered
 * as an iterator. Will combine queries on multiple bridges into one
 * SQL statement where possible.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindQuery.java,v 1.15 2008/08/12 13:07:53 cyganiak Exp $
 */
public class FindQuery {
	private List compatibleRelations = new ArrayList();
	
	public FindQuery(Triple triplePattern, Collection propertyBridges) {
		URIMakerRule rule = new URIMakerRule();
		propertyBridges = rule.sortRDFRelations(propertyBridges);
		URIMakerRuleChecker subjectChecker = rule.createRuleChecker(triplePattern.getSubject());
		URIMakerRuleChecker objectChecker = rule.createRuleChecker(triplePattern.getObject());
		Iterator it = propertyBridges.iterator();
		while (it.hasNext()) {
			TripleRelation tripleRelation = (TripleRelation) it.next();
			TripleRelation selectedTripleRelation = tripleRelation.selectTriple(triplePattern);
			if (selectedTripleRelation != null
					&& subjectChecker.canMatch(tripleRelation.nodeMaker(TripleRelation.SUBJECT_NODE_MAKER))
					&& objectChecker.canMatch(tripleRelation.nodeMaker(TripleRelation.OBJECT_NODE_MAKER))) {
				subjectChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.SUBJECT_NODE_MAKER));
				objectChecker.addPotentialMatch(tripleRelation.nodeMaker(TripleRelation.OBJECT_NODE_MAKER));
				addRelation(new JoinOptimizer(selectedTripleRelation).optimize());
			}
		}
	}
	
	private void addRelation(TripleRelation tripleRelation) {
		Iterator it = this.compatibleRelations.iterator();
		while (it.hasNext()) {
			CompatibleRelationGroup group = (CompatibleRelationGroup) it.next();
			if (group.isCompatible(tripleRelation.baseRelation())) {
				group.add(tripleRelation.baseRelation(), tripleRelation);
				return;
			}
		}
		this.compatibleRelations.add(new CompatibleRelationGroup(tripleRelation.baseRelation(), tripleRelation));
	}

	public ExtendedIterator iterator() {
		ExtendedIterator result = NullIterator.emptyIterator();
		Iterator it = compatibleRelations.iterator();
		while (it.hasNext()) {
			CompatibleRelationGroup group = (CompatibleRelationGroup) it.next();
			if (!group.baseRelation().equals(Relation.EMPTY)) {
				result = result.andThen(
						RelationToTriplesIterator.createTripleIterator(group.baseRelation(), group));
			}
		}
		return result;
	}
}
