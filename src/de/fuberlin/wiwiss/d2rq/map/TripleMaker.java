package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;

/**
 * Knows how to create triples from String arrays produced by a database query.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TripleMaker.java,v 1.1 2006/09/09 15:40:03 cyganiak Exp $
 */
public class TripleMaker {
	private NodeMaker subjectMaker;
	private NodeMaker predicateMaker;
	private NodeMaker objectMaker;
	private Map columnNamesToIndices;
	
	/**
	 * @param relation The query from which to produce triples
	 * @param columnNameNumberMap A map from column names to Integer indices into result row arrays
	 */
	public TripleMaker(RDFRelation relation, Map columnNamesToIndices) {
		this.subjectMaker = relation.getSubjectMaker();
		this.predicateMaker = relation.getPredicateMaker();
		this.objectMaker = relation.getObjectMaker();
		this.columnNamesToIndices = columnNamesToIndices;
	}
	
	/**
	 * Creates a triple from a database result row.
	 * @param row a database result row
	 * @return a triple extracted from the row
	 */
	public Triple makeTriple(String[] row) {
		Node s = this.subjectMaker.getNode(row, this.columnNamesToIndices);
		Node p = this.predicateMaker.getNode(row, this.columnNamesToIndices);
		Node o = this.objectMaker.getNode(row, this.columnNamesToIndices);
		if (s == null || p == null || o == null) {
			return null;
		}
		return new Triple(s, p, o);
	}
}
