package de.fuberlin.wiwiss.d2rq.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class SD {
	private static Model vocabModel = ModelFactory.createDefaultModel();

	public static final String NS = "http://www.w3.org/ns/sparql-service-description#";

	public static final Resource NAMESPACE = vocabModel.createResource(NS);

	public static final Resource Service = vocabModel.createResource(NS
			+ "Service");
	
	public static final Resource Dataset = vocabModel.createResource(NS
			+ "Dataset");
	
	public static final Resource Graph = vocabModel.createResource(NS
			+ "Graph");
	
	public static final Property url = vocabModel
			.createProperty(NS + "url");
	
	public static final Property defaultDatasetDescription = vocabModel
			.createProperty(NS + "defaultDatasetDescription");
	
	public static final Property defaultGraph = vocabModel
			.createProperty(NS + "defaultGraph");
	
	public static final Property resultFormat = vocabModel
			.createProperty(NS + "resultFormat");
	
	
	
}
