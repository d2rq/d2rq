package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.map.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.TripleMaker;

/**
 * A relation, as defined in relational algebra, plus a set of NodeMakers
 * attached to the relation, plus a set of TripleMakers attached to the
 * NodeMakers. Very much work in progress.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: RDFRelation.java,v 1.4 2006/09/10 22:18:44 cyganiak Exp $
 */
public interface RDFRelation extends TripleMaker {
	
	static final RDFRelation EMPTY = new RDFRelation() {
		public Relation baseRelation() { return Relation.EMPTY; }
		public Set projectionColumns() { return Collections.EMPTY_SET; }
		public boolean isUnique() { return true; }
		public Collection makeTriples(ResultRow row) { return Collections.EMPTY_LIST; }
		public NodeMaker nodeMaker(int index) { return NodeMaker.EMPTY; }
		public RDFRelation selectTriple(Triple triplePattern) { return RDFRelation.EMPTY; }
		public RDFRelation renameColumns(ColumnRenamer renamer) { return RDFRelation.EMPTY; }
		public String toString() { return "RDFRelation.EMPTY"; }
	};

	Relation baseRelation();
	
	Set projectionColumns();
	
	boolean isUnique();

	/**
	 * TODO Get rid of RDFRelation.nodeMaker(index)
	 * @param index 0, 1 or 2 
	 * @return The subject, predicate or object NodeMaker
	 */
	NodeMaker nodeMaker(int i);
	
	RDFRelation selectTriple(Triple triplePattern);
	
	RDFRelation renameColumns(ColumnRenamer renamer);
}