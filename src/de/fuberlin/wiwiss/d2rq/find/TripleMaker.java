package de.fuberlin.wiwiss.d2rq.find;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * Creates an RDF triple from a query result row.
 * 
 * TODO: Make a subclass of BindingMaker?
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TripleMaker {
	private final NodeMaker subjects;
	private final NodeMaker predicates;
	private final NodeMaker objects;
	private final ProjectionSpec condition;
	
	public TripleMaker(TripleRelation tripleRelation) {
		this(
				tripleRelation.nodeMaker(TripleRelation.SUBJECT),
				tripleRelation.nodeMaker(TripleRelation.PREDICATE),
				tripleRelation.nodeMaker(TripleRelation.OBJECT));
	}
	
	public TripleMaker(TripleMaker tripleMaker, ProjectionSpec condition) {
		this(tripleMaker.subjects, tripleMaker.predicates, tripleMaker.objects, condition);
	}
	
	public TripleMaker(NodeMaker s, NodeMaker p, NodeMaker o) {
		this(s, p, o, null);
	}
	
	public TripleMaker(NodeMaker s, NodeMaker p, NodeMaker o, ProjectionSpec condition) {
		this.subjects = s;
		this.predicates = p;
		this.objects = o;
		this.condition = condition;
	}
	
	public Triple makeTriple(ResultRow row) {
		if (condition != null) {
			String value = row.get(condition);
			if (value == null || "false".equals(value) || "0".equals(value) || "".equals(value)) {
				return null;
			}
		}
		Node s = subjects.makeNode(row);
		Node p = predicates.makeNode(row);
		Node o = objects.makeNode(row);
		if (s == null || p == null || o == null) {
			return null;
		}
		return new Triple(s, p, o);
	}
}
