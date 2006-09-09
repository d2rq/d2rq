package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.find.QueryContext;
import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Expression;
import de.fuberlin.wiwiss.d2rq.map.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;

/**
 * Encapsulates a query for a triple pattern on a specific
 * {@link RDFRelation}.
 * 
 * TODO: Introduce SelectingNodeMaker -- a FixedNodeMaker with ColumnValues that stelect the fixed node
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TripleSelection.java,v 1.1 2006/09/09 15:40:05 cyganiak Exp $
 */
public class TripleSelection implements RDFRelation {
	private RDFRelation bridge;
	private Map columnValues = new HashMap();
	private Set selectColumns = new HashSet();
	private NodeMaker subjectMaker = null;
	private NodeMaker predicateMaker = null;
	private NodeMaker objectMaker = null;

	/**
	 * Constructs a new TripleQuery.
	 * @param bridge We look for triples matching this property bridge
	 * @param triplePattern The triple we are looking for, might contain Node.ANY
	 */
	public TripleSelection(RDFRelation bridge, Triple triplePattern) {
		this.bridge = bridge;
		if (triplePattern.getSubject().isConcrete()) {
			this.columnValues.putAll(bridge.getSubjectMaker().getColumnValues(triplePattern.getSubject()));
			this.subjectMaker = new FixedNodeMaker(triplePattern.getSubject());
		} else {
			this.selectColumns.addAll(bridge.getSubjectMaker().getColumns());
		}
		if (triplePattern.getPredicate().isConcrete()) {
			this.columnValues.putAll(bridge.getPredicateMaker().getColumnValues(triplePattern.getPredicate()));
			this.predicateMaker = new FixedNodeMaker(triplePattern.getPredicate());
		} else {
			this.selectColumns.addAll(bridge.getPredicateMaker().getColumns());
		}
		if (triplePattern.getObject().isConcrete()) {
			this.columnValues.putAll(bridge.getObjectMaker().getColumnValues(triplePattern.getObject()));
			this.objectMaker = new FixedNodeMaker(triplePattern.getObject());
		} else {
			this.selectColumns.addAll(bridge.getObjectMaker().getColumns());
		}
	}

	/**
	 * Checks if a given triple could match this bridge without
	 * querying the database.
	 */
	public boolean couldFit(Triple t, QueryContext context) {
		if (this.subjectMaker != null && !this.subjectMaker.couldFit(t.getSubject())) {
			return false;
		}
		if (this.predicateMaker != null && !this.predicateMaker.couldFit(t.getPredicate())) {
			return false;
		}
		if (this.objectMaker != null && !this.objectMaker.couldFit(t.getSubject())) {
			return false;
		}
		return this.bridge.couldFit(t, context);
	}

	public int getEvaluationPriority() {
		return this.bridge.getEvaluationPriority();
	}

	public NodeMaker getSubjectMaker() {
		return (this.subjectMaker == null) ? this.bridge.getSubjectMaker() : this.subjectMaker;
	}
	
	public NodeMaker getPredicateMaker() {
		return (this.predicateMaker == null) ? this.bridge.getPredicateMaker() : this.predicateMaker;
	}
	
	public NodeMaker getObjectMaker() {
		return (this.objectMaker == null) ? this.bridge.getObjectMaker() : this.objectMaker;
	}
	
	public Set getJoins() {
		return this.bridge.getJoins();
	}
	
	public AliasMap getAliases() {
		return this.bridge.getAliases();
	}
	
	public Expression condition() {
		return this.bridge.condition();
	}

	public Map getColumnValues() {
		return this.columnValues;
	}
	
	public Set getSelectColumns() {
		return this.selectColumns;
	}

	public Database getDatabase() {
		return this.bridge.getDatabase();
	}

	public boolean mightContainDuplicates() {
		return this.bridge.mightContainDuplicates();
	}

	public String toString() {
		return "PropertyBridgeQuery(\n" +
				"    " + getSubjectMaker() + "\n" +
				"    " + getPredicateMaker() + "\n" +
				"    " + getObjectMaker() + "\n" +
				")";
	}
}
