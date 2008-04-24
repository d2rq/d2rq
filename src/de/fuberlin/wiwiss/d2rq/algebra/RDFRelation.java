package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.TripleMaker;

/**
 * A relation, as defined in relational algebra, plus a set of NodeMakers
 * attached to the relation, plus a set of TripleMakers attached to the
 * NodeMakers. Very much work in progress.
 * 
 * TODO Probably shouldn't have projectionSpecs(), renameColumns() and
 * 		isUnique(), all of which sound like properties of the baseRelation()
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: RDFRelation.java,v 1.8 2008/04/24 17:48:52 cyganiak Exp $
 */
public abstract class RDFRelation implements TripleMaker {
	
	public static final RDFRelation EMPTY = new RDFRelation() {
		public Relation baseRelation() { return Relation.EMPTY; }
		public Set projectionSpecs() { return Collections.EMPTY_SET; }
		public boolean isUnique() { return true; }
		public Collection makeTriples(ResultRow row) { return Collections.EMPTY_LIST; }
		public NodeMaker nodeMaker(int index) { return NodeMaker.EMPTY; }
		public RDFRelation selectTriple(Triple triplePattern) { return RDFRelation.EMPTY; }
		public RDFRelation renameColumns(ColumnRenamer renamer) { return RDFRelation.EMPTY; }
		public Collection names() { return Collections.EMPTY_LIST; }
		public NodeMaker namedNodeMaker(String name) { return null; }
		public String toString() { return "RDFRelation.EMPTY"; }
	};

	public abstract Relation baseRelation();
	
	public abstract Set projectionSpecs();
	
	public abstract boolean isUnique();

	/**
	 * TODO Get rid of RDFRelation.nodeMaker(index)
	 * @param index 0, 1 or 2 
	 * @return The subject, predicate or object NodeMaker
	 */
	public abstract NodeMaker nodeMaker(int index);
	
	public abstract RDFRelation selectTriple(Triple triplePattern);
	
	public abstract RDFRelation renameColumns(ColumnRenamer renamer);
	
	public abstract Collection names();
	
	public abstract NodeMaker namedNodeMaker(String name);
	
	public Set allKnownAttributes() {
		Set results = new HashSet();
		Iterator it = projectionSpecs().iterator();
		while (it.hasNext()) {
			ProjectionSpec spec = (ProjectionSpec) it.next();
			results.addAll(spec.requiredAttributes());
		}
		results.addAll(baseRelation().condition().columns());
		it = baseRelation().joinConditions().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			results.addAll(join.attributes1());
			results.addAll(join.attributes2());
		}
		return results;
	}
	
	public Set tables() {
		Set results = new HashSet();
		Iterator it = allKnownAttributes().iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			results.add(attribute.relationName());
		}
		return results;
	}
}