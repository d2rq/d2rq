package de.fuberlin.wiwiss.d2rq.vocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class VoID {
	private static Model vocabModel = ModelFactory.createDefaultModel();

	public static final String NS = "http://rdfs.org/ns/void#";

	public static final Resource NAMESPACE = vocabModel.createResource(NS);

	public static final Resource Dataset = vocabModel.createResource(NS
			+ "Dataset");

	public static final Property homepage = vocabModel
			.createProperty("http://xmlns.com/foaf/0.1/homepage");
	
	public static final Property feature = vocabModel
			.createProperty(NS + "feature");
	
	public static final Property rootResource = vocabModel
			.createProperty(NS + "rootResource");
	
	public static final Property uriSpace = vocabModel
			.createProperty(NS + "uriSpace");
	
	public static final Property class_ = vocabModel
			.createProperty(NS + "class");
	
	public static final Property property = vocabModel
			.createProperty(NS + "property");
	
	public static final Property vocabulary = vocabModel
			.createProperty(NS + "vocabulary");
	
	public static final Property classPartition = vocabModel
			.createProperty(NS + "classPartition");
	
	public static final Property propertyPartition = vocabModel
			.createProperty(NS + "propertyPartition");
	
	public static final Property sparqlEndpoint = vocabModel
			.createProperty(NS + "sparqlEndpoint");
	
	public static final Property inDataset = vocabModel
			.createProperty(NS + "inDataset");
}
