package de.fuberlin.wiwiss.d2rq.vocab;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;


public class VocabularySummarizerTest extends TestCase {

	public void testAllPropertiesEmpty() {
		VocabularySummarizer vocab = new VocabularySummarizer(Object.class);
		assertTrue(vocab.getAllProperties().isEmpty());
	}
	
	public void testAllClassesEmpty() {
		VocabularySummarizer vocab = new VocabularySummarizer(Object.class);
		assertTrue(vocab.getAllClasses().isEmpty());
	}
	
	public void testAllPropertiesContainsProperty() {
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertTrue(vocab.getAllProperties().contains(D2RQ.column));
		assertTrue(vocab.getAllProperties().contains(D2RQ.belongsToClassMap));
	}
	
	public void testAllPropertiesDoesNotContainClass() {
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertFalse(vocab.getAllProperties().contains(D2RQ.Database));
	}
	
	public void testAllPropertiesDoesNotContainTermFromOtherNamespace() {
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertFalse(vocab.getAllProperties().contains(RDF.type));
	}

	public void testAllClassesContainsClass() {
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertTrue(vocab.getAllClasses().contains(D2RQ.Database));
	}
	
	public void testAllClassesDoesNotContainProperty() {
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertFalse(vocab.getAllClasses().contains(D2RQ.column));
	}
	
	public void testAllClassesDoesNotContainTermFromOtherNamespace() {
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertFalse(vocab.getAllClasses().contains(D2RConfig.Server));
	}
	
	public void testGetNamespaceEmpty() {
		assertNull(new VocabularySummarizer(Object.class).getNamespace());
	}
	
	public void testGetNamespaceD2RQ() {
		assertEquals(D2RQ.NS, new VocabularySummarizer(D2RQ.class).getNamespace());		
	}

	public void testGetNamespaceD2RConfig() {
		assertEquals(D2RConfig.NS, new VocabularySummarizer(D2RConfig.class).getNamespace());		
	}
	
	public void testNoUndefinedClassesForEmptyModel() {
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertTrue(vocab.getUndefinedClasses(ModelFactory.createDefaultModel()).isEmpty());
	}
	
	public void testNoUndefinedClassesWithoutTypeStatement() {
		Model m = D2RQTestSuite.loadTurtle("vocab/no-type.ttl");
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertTrue(vocab.getUndefinedClasses(m).isEmpty());
	}
	
	public void testNoUndefinedClassesIfAllClassesDefined() {
		Model m = D2RQTestSuite.loadTurtle("vocab/defined-types.ttl");
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertTrue(vocab.getUndefinedClasses(m).isEmpty());
	}
	
	public void testNoUndefinedClassesIfAllInOtherNamespace() {
		Model m = D2RQTestSuite.loadTurtle("vocab/other-namespace-types.ttl");
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertTrue(vocab.getUndefinedClasses(m).isEmpty());
	}
	
	public void testFindOneUndefinedClass() {
		final Model m = D2RQTestSuite.loadTurtle("vocab/one-undefined-type.ttl");
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		Collection<Resource> expected = new HashSet<Resource>() {{ 
			this.add(m.createResource(D2RQ.NS + "Pint"));
		}};
		assertEquals(expected, vocab.getUndefinedClasses(m));
	}
	
	public void testFindTwoUndefinedClasses() {
		final Model m = D2RQTestSuite.loadTurtle("vocab/two-undefined-types.ttl");
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		Collection<Resource> expected = new HashSet<Resource>() {{ 
			this.add(m.createResource(D2RQ.NS + "Pint"));
			this.add(m.createResource(D2RQ.NS + "Shot"));
		}};
		assertEquals(expected, vocab.getUndefinedClasses(m));
	}
	
	public void testNoUndefinedPropertiesForEmptyModel() {
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertTrue(vocab.getUndefinedProperties(ModelFactory.createDefaultModel()).isEmpty());
	}
	
	public void testNoUndefinedPropertiesIfAllPropertiesDefined() {
		Model m = D2RQTestSuite.loadTurtle("vocab/defined-properties.ttl");
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertTrue(vocab.getUndefinedProperties(m).isEmpty());
	}
	
	public void testNoUndefinedPropertiesIfAllInOtherNamespace() {
		Model m = D2RQTestSuite.loadTurtle("vocab/other-namespace-properties.ttl");
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		assertTrue(vocab.getUndefinedProperties(m).isEmpty());
	}
	
	public void testFindOneUndefinedProperty() {
		final Model m = D2RQTestSuite.loadTurtle("vocab/one-undefined-property.ttl");
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		Collection<Property> expected = new HashSet<Property>() {{ 
			this.add(m.createProperty(D2RQ.NS + "price"));
		}};
		assertEquals(expected, vocab.getUndefinedProperties(m));
	}
	
	public void testFindTwoUndefinedProperties() {
		final Model m = D2RQTestSuite.loadTurtle("vocab/two-undefined-properties.ttl");
		VocabularySummarizer vocab = new VocabularySummarizer(D2RQ.class);
		Collection<Property> expected = new HashSet<Property>() {{ 
			this.add(m.createProperty(D2RQ.NS + "price"));
			this.add(m.createProperty(D2RQ.NS + "parallelUniverse"));
		}};
		assertEquals(expected, vocab.getUndefinedProperties(m));
	}
}
