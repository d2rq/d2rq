package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.find.QueryContext;
import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Expression;
import de.fuberlin.wiwiss.d2rq.map.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.map.TripleMaker;

/**
 * Encapsulates a query for a triple pattern on a specific
 * {@link RDFRelation}.
 * 
 * TODO: Introduce SelectingNodeMaker -- a FixedNodeMaker with ColumnValues that stelect the fixed node
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TripleSelection.java,v 1.2 2006/09/09 20:51:49 cyganiak Exp $
 */
public class TripleSelection implements RDFRelation {
	private RDFRelation base;
	private Map columnValues = new HashMap();
	private Set selectColumns = new HashSet();
	private NodeMaker subjectMaker = null;
	private NodeMaker predicateMaker = null;
	private NodeMaker objectMaker = null;

	/**
	 * Constructs a new TripleQuery.
	 * @param base We select from this relation
	 * @param triplePattern The triple we are looking for, might contain Node.ANY
	 */
	public TripleSelection(RDFRelation base, Triple triplePattern) {
		this.base = base;
		if (triplePattern.getSubject().isConcrete()) {
			this.columnValues.putAll(base.getSubjectMaker().getColumnValues(triplePattern.getSubject()));
			this.subjectMaker = new FixedNodeMaker(triplePattern.getSubject());
		} else {
			this.selectColumns.addAll(base.getSubjectMaker().getColumns());
		}
		if (triplePattern.getPredicate().isConcrete()) {
			this.columnValues.putAll(base.getPredicateMaker().getColumnValues(triplePattern.getPredicate()));
			this.predicateMaker = new FixedNodeMaker(triplePattern.getPredicate());
		} else {
			this.selectColumns.addAll(base.getPredicateMaker().getColumns());
		}
		if (triplePattern.getObject().isConcrete()) {
			this.columnValues.putAll(base.getObjectMaker().getColumnValues(triplePattern.getObject()));
			this.objectMaker = new FixedNodeMaker(triplePattern.getObject());
		} else {
			this.selectColumns.addAll(base.getObjectMaker().getColumns());
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
		return this.base.couldFit(t, context);
	}

	public int getEvaluationPriority() {
		return this.base.getEvaluationPriority();
	}

	public NodeMaker getSubjectMaker() {
		return (this.subjectMaker == null) ? this.base.getSubjectMaker() : this.subjectMaker;
	}
	
	public NodeMaker getPredicateMaker() {
		return (this.predicateMaker == null) ? this.base.getPredicateMaker() : this.predicateMaker;
	}
	
	public NodeMaker getObjectMaker() {
		return (this.objectMaker == null) ? this.base.getObjectMaker() : this.objectMaker;
	}
	
	public Set getJoins() {
		return this.base.getJoins();
	}
	
	public AliasMap getAliases() {
		return this.base.getAliases();
	}
	
	public Expression condition() {
		return this.base.condition();
	}

	public Map getColumnValues() {
		return this.columnValues;
	}
	
	public Set getSelectColumns() {
		return this.selectColumns;
	}

	public Database getDatabase() {
		return this.base.getDatabase();
	}

	public boolean mightContainDuplicates() {
		return this.base.mightContainDuplicates();
	}

	public String toString() {
		return "PropertyBridgeQuery(\n" +
				"    " + getSubjectMaker() + "\n" +
				"    " + getPredicateMaker() + "\n" +
				"    " + getObjectMaker() + "\n" +
				")";
	}
	
	public TripleMaker tripleMaker(final Map columnNamesToIndices) {
		return new TripleMaker() {
			public Collection makeTriples(String[] row) {
				Node s = getSubjectMaker().getNode(row, columnNamesToIndices);
				Node p = getPredicateMaker().getNode(row, columnNamesToIndices);
				Node o = getObjectMaker().getNode(row, columnNamesToIndices);
				if (s == null || p == null || o == null) {
					return Collections.EMPTY_LIST;
				}
				return Collections.singleton(new Triple(s, p, o));
			}
		};
	}
}
