package de.fuberlin.wiwiss.d2rq.map;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQException;

public class ConstantValueClassMapTest extends TestCase {
	
	private Model model;
	private Mapping mapping;
	private Database database;
	
	private ClassMap concept;
	private PropertyBridge conceptTypeBridge;
	
	private ClassMap collection;
	private PropertyBridge collectionTypeBridge;
	
	private PropertyBridge memberBridge;
	
	private ClassMap createClassMap(String uriPattern) {
		ClassMap result = new ClassMap(this.model.createResource());
		result.setDatabase(this.database);
		result.setURIPattern(uriPattern);
		this.mapping.addClassMap(result);
		return result;
	}
	
	private ClassMap createConstantClassMap(String uri) {
		ClassMap result = new ClassMap(this.model.createResource());
		result.setDatabase(this.database);
		result.setConstantValue(this.model.createResource(uri));
		this.mapping.addClassMap(result);
		return result;
	}

	private PropertyBridge createPropertyBridge(ClassMap classMap, String propertyURI) {
		PropertyBridge result = new PropertyBridge(this.model.createResource());
		result.setBelongsToClassMap(classMap);
		result.addProperty(this.model.createProperty(propertyURI));
		classMap.addPropertyBridge(result);
		return result;
	}
	
	public void setUp() {
		this.model = ModelFactory.createDefaultModel();
		this.mapping = new Mapping();
		this.database = new Database(this.model.createResource());
		this.mapping.addDatabase(this.database);
		
		concept = createClassMap("http://example.com/concept#@@c.ID@@");
		conceptTypeBridge = createPropertyBridge(concept, RDF.type.getURI());
		conceptTypeBridge.setConstantValue(model.createResource("http://www.w3.org/2004/02/skos/core#Concept"));
		
		collection = createConstantClassMap("http://example.com/collection#MyConceptCollection");
		collectionTypeBridge = createPropertyBridge(collection, RDF.type.getURI());
		collectionTypeBridge.setConstantValue(model.createResource("http://www.w3.org/2004/02/skos/core#Collection"));

		memberBridge = createPropertyBridge(collection, "http://www.w3.org/2004/02/skos/core#member");
		memberBridge.setRefersToClassMap(concept);
		memberBridge.addCondition("c.foo = 1");
	}
	
	public void testValidate()
	{
		try {
			collection.validate();
		} catch (D2RQException e) {
			fail("Should validate without exceptions");
		}
	}
}
