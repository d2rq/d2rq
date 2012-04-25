package de.fuberlin.wiwiss.d2rq.vocab;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;

/**
 * Lists all the classes and properties in a schemagen-generated
 * vocabulary class, such as the {@link D2RQ} class.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class VocabularySummarizer {
	private final Class<? extends Object> vocabularyJavaClass;
	private final Set<Property> properties;
	private final Set<Resource> classes;
	
	public VocabularySummarizer(Class<? extends Object> vocabularyJavaClass) {
		this.vocabularyJavaClass = vocabularyJavaClass;
		properties = findAllProperties();
		classes = findAllClasses();
	}

	public Set<Property> getAllProperties() {
		return properties;
	}
	
	private Set<Property> findAllProperties() {
		Set<Property> results = new HashSet<Property>();
		for (int i = 0; i < vocabularyJavaClass.getFields().length; i++) {
			Field field = vocabularyJavaClass.getFields()[i];
			if (!Modifier.isStatic(field.getModifiers())) continue;
			if (!Property.class.isAssignableFrom(field.getType())) continue;
			try {
				results.add((Property) field.get(null));
			} catch (IllegalAccessException ex) {
				throw new D2RQException(ex);
			}
		}
		return results;
	}
	
	public Set<Resource> getAllClasses() {
		return classes;
	}
	
	private Set<Resource> findAllClasses() {
		Set<Resource> results = new HashSet<Resource>();
		for (int i = 0; i < vocabularyJavaClass.getFields().length; i++) {
			Field field = vocabularyJavaClass.getFields()[i];
			if (!Modifier.isStatic(field.getModifiers())) continue;
			if (!Resource.class.isAssignableFrom(field.getType())) continue;
			if (Property.class.isAssignableFrom(field.getType())) continue;
			try {
				results.add((Resource) field.get(null));
			} catch (IllegalAccessException ex) {
				throw new D2RQException(ex);
			}
		}
		return results;
	}
}
