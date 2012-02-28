package de.fuberlin.wiwiss.d2rq.examples;

import java.util.Iterator;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DC;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

public class JenaGraphExample {

	public static void main(String[] args) {
		// Load mapping file
		Model mapping = FileManager.get().loadModel("doc/example/mapping-iswc.ttl");
		
		// Set up the GraphD2RQ
		GraphD2RQ g = new GraphD2RQ(mapping, "http://localhost:2020/");

		// Create a find(spo) pattern 
		Node subject = Node.ANY;
		Node predicate = DC.date.asNode();
		Node object = Node.createLiteral("2003", null, XSDDatatype.XSDgYear);
		Triple pattern = new Triple(subject, predicate, object);

		// Query the graph
		Iterator<Triple> it = g.find(pattern);
		
		// Output query results
		while (it.hasNext()) {
			Triple t = it.next();
		    System.out.println("Published in 2003: " + t.getSubject());
		}
	}
}
