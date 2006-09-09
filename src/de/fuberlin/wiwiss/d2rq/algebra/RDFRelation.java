package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.find.QueryContext;
import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Expression;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.map.TripleMaker;

/**
 * A relation, as defined in relational algebra, plus a set of NodeMakers
 * attached to the relation, plus a set of TripleMakers attached to the
 * NodeMakers. Very much work in progress.
 * 
 * TODO: Many methods, like getSelectColumns(), can contradict the corresponding
 *       methods in the NodeMakers; refactor the interfaces to prevent this
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: RDFRelation.java,v 1.2 2006/09/09 20:51:49 cyganiak Exp $
 */
public interface RDFRelation {
	
	/**
	 * Checks if a given triple could match this relation without
	 * querying the database.
	 */
	boolean couldFit(Triple t, QueryContext context);

	Database getDatabase();

	Set getSelectColumns();

	AliasMap getAliases();

	Set getJoins();

	Map getColumnValues();

	/**
	 * Returns the SQL WHERE condition that must hold for a given
	 * database row or the bridge will not generate a triple.
	 * @return An SQL expression; {@link Expression#TRUE} indicates no condition
	 */
	Expression condition();

	int getEvaluationPriority();

	boolean mightContainDuplicates();

	NodeMaker getSubjectMaker();

	NodeMaker getPredicateMaker();

	NodeMaker getObjectMaker();
	
	TripleMaker tripleMaker(Map columnNamesToIndices);
}