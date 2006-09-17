package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.algebra.JoinOptimizer;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.algebra.UnionOverSameBase;
import de.fuberlin.wiwiss.d2rq.find.URIMakerRule.URIMakerRuleChecker;
import de.fuberlin.wiwiss.d2rq.sql.ApplyTripleMakerIterator;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;


/**
 * A find query on a list of property bridges. Results are delivered
 * as an iterator. Will combine queries on multiple bridges into one
 * SQL statement where possible.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindQuery.java,v 1.10 2006/09/17 00:36:27 cyganiak Exp $
 */
public class FindQuery {
	private Collection compatibleRelations = new ArrayList();
	
	public FindQuery(Triple triplePattern, Collection propertyBridges) {
		URIMakerRule rule = new URIMakerRule();
		propertyBridges = rule.sortRDFRelations(propertyBridges);
		URIMakerRuleChecker subjectChecker = rule.createRuleChecker(triplePattern.getSubject());
		URIMakerRuleChecker objectChecker = rule.createRuleChecker(triplePattern.getObject());
		Iterator it = propertyBridges.iterator();
		while (it.hasNext()) {
			RDFRelation bridge = (RDFRelation) it.next();
			RDFRelation selectionBridge = bridge.selectTriple(triplePattern);
			if (!selectionBridge.equals(RDFRelation.EMPTY)
					&& subjectChecker.canMatch(bridge.nodeMaker(0))
					&& objectChecker.canMatch(bridge.nodeMaker(2))) {
				subjectChecker.addPotentialMatch(bridge.nodeMaker(0));
				objectChecker.addPotentialMatch(bridge.nodeMaker(2));
				addRelation(new JoinOptimizer(selectionBridge).optimize());
			}
		}
	}
	
	private void addRelation(RDFRelation relation) {
		Iterator it = this.compatibleRelations.iterator();
		while (it.hasNext()) {
			List queries = (List) it.next();
			if (UnionOverSameBase.isSameBase((RDFRelation) queries.get(0), relation)) {
				queries.add(relation);
				return;
			}
		}
		List newList = new ArrayList();
		newList.add(relation);
		this.compatibleRelations.add(newList);
	}

	public ExtendedIterator iterator() {
		ExtendedIterator result = new NullIterator();
		Iterator it = this.compatibleRelations.iterator();
		while (it.hasNext()) {
			List relations = (List) it.next();
			RDFRelation union = new UnionOverSameBase(relations);
			SelectStatementBuilder sql = new SelectStatementBuilder(union.baseRelation().database());
			sql.setEliminateDuplicates(!union.isUnique());
			sql.addSelectColumns(union.projectionColumns());
			sql.addRelation(union.baseRelation());
			result = result.andThen(new ApplyTripleMakerIterator(sql.execute(), union));
		}
		return result;
	}
}
