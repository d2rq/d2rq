package de.fuberlin.wiwiss.d2rq.examples;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;

public class SPARQLExample {

	public static void main(String[] args) {
		ModelD2RQ m = new ModelD2RQ("file:doc/example/mapping-iswc.ttl");
		String sparql = 
			"PREFIX dc: <http://purl.org/dc/elements/1.1/>" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>" +
			"SELECT ?paperTitle ?authorName WHERE {" +
			"    ?paper dc:title ?paperTitle . " +
			"    ?paper dc:creator ?author ." +
			"    ?author foaf:name ?authorName ." +
			"}";
		Query q = QueryFactory.create(sparql); 
		ResultSet rs = QueryExecutionFactory.create(q, m).execSelect();
		while (rs.hasNext()) {
			QuerySolution row = rs.nextSolution();
			System.out.println("Title: " + row.getLiteral("paperTitle").getString());
			System.out.println("Author: " + row.getLiteral("authorName").getString());
		}
		m.close();
	}
}
