package de.fuberlin.wiwiss.d2rq.examples;

import java.util.Iterator;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DC;

import de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;

public class JenaGraphExample {

	public static void main(String[] args) {
		// Load mapping file
		Model mapModel = FileManager.get().loadModel("doc/example/mapping-iswc.ttl");
		
		// Parse mapping file
		MapParser parser = new MapParser(mapModel, "http://localhost:2020/");
		Mapping mapping = parser.parse();
		
		// Set up the GraphD2RQ
		GraphD2RQ g = new GraphD2RQ(mapping);

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
		g.close();
	}
}
