package org.d2rq.examples;

import java.util.Iterator;

import org.d2rq.CompiledMapping;
import org.d2rq.jena.GraphD2RQ;
import org.d2rq.lang.D2RQReader;
import org.d2rq.lang.Mapping;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DC;


public class JenaGraphExample {

	public static void main(String[] args) {
		// Load mapping file
		Model mapModel = FileManager.get().loadModel("doc/example/mapping-iswc.ttl");
		
		// Read mapping file
		D2RQReader reader = new D2RQReader(mapModel, "http://localhost:2020/");
		Mapping mapping = reader.getMapping();
		
		// Compile mapping for D2RQ engine
		CompiledMapping compiled = mapping.compile();

		// Set up the GraphD2RQ
		GraphD2RQ g = new GraphD2RQ(compiled);

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
