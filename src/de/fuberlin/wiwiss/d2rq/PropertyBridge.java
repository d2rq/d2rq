/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * A respresentation of a d2rq:PropertyBridge, responsible for
 * creating a set of virtual triples. The predicate is fixed,
 * subjects and objects come from one of the various {@link NodeMaker}s.
 *
 * TODO: Rename to TripleSet?
 * 
 * <BR>History: 06-03-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
class PropertyBridge {
	private Node id;
	private NodeMaker subjectMaker;
	private NodeMaker predicateMaker;
	private NodeMaker objectMaker; 
	private Database database;
	private Set joins;
	private Set conditions = new HashSet(1);
	private URIMatchPolicy uriMatchPolicy = new URIMatchPolicy();
	private boolean mayContainDuplicates = false;

	public PropertyBridge(Node id, Node property, NodeMaker subjectMaker, NodeMaker objectMaker, Database database, Set joins) {
		this.id = id;
		this.subjectMaker = subjectMaker;
		this.predicateMaker = new FixedNodeMaker(property);
		this.objectMaker = objectMaker;
		this.database = database;
		this.joins = joins;
	}

	public PropertyBridge(Node id, Node property, NodeMaker subjectMaker, NodeMaker objectMaker, Database database) {
		this(id, property, subjectMaker, objectMaker, database, new HashSet(0));
	}

	/**
	 * Adds SQL WHERE conditions that must evaluate to TRUE for a given
	 * database row or the bridge will not generate a triple.
	 * @param whereConditions a set of Strings
	 */
	public void addConditions(Set whereConditions) {
		if (whereConditions != null) {
			this.conditions.addAll(whereConditions);
		}
	}

	/**
	 * Returns the SQL WHERE conditions that must hold for a given
	 * database row or the bridge will not generate a triple.
	 * @return a set of Strings
	 */
	public Set getConditions() {
		return this.conditions;
	}

	public void setURIMatchPolicy(URIMatchPolicy policy) {
		this.uriMatchPolicy = policy;
	}

	public int getEvaluationPriority() {
		return this.uriMatchPolicy.getEvaluationPriority();
	}

	public void setMayContainDuplicates(boolean mayContainDuplicates) {
		this.mayContainDuplicates = mayContainDuplicates;
	}

	public boolean mayContainDuplicates() {
		return this.mayContainDuplicates;
	}
	/**
	 * Checks if a given triple could match this bridge without
	 * querying the database.
	 */
	public boolean couldFit(Triple t, QueryContext context) {
		if (!this.subjectMaker.couldFit(t.getSubject()) ||
				!this.predicateMaker.couldFit(t.getPredicate()) ||
				!this.objectMaker.couldFit(t.getObject())) {
			return false;
		}
		if (t.getSubject().isConcrete()) {
			if (!this.uriMatchPolicy.couldFitSubjectInContext(context)) {
				return false;
			}
			this.uriMatchPolicy.updateContextAfterSubjectMatch(context);
		}
		if (t.getObject().isConcrete()) {
			if (!this.uriMatchPolicy.couldFitObjectInContext(context)) {
				return false;
			}
			this.uriMatchPolicy.updateContextAfterObjectMatch(context);
		}
		return true;
	}

	public Database getDatabase() {
		return this.database;
	}

	public NodeMaker getSubjectMaker() {
		return this.subjectMaker;
	}

	public NodeMaker getPredicateMaker() {
		return this.predicateMaker;
	}

	public NodeMaker getObjectMaker() {
		return this.objectMaker;
	}
    
	public Set getJoins() {
		return this.joins;
	}

	public String toString() {
		return this.id.toString();
	}
}