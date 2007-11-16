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
 * @version $Id: VocabularySummarizer.java,v 1.1 2007/11/16 12:10:16 cyganiak Exp $
 */
public class VocabularySummarizer {
	private final Class vocabularyJavaClass;
	private final Set properties;
	private final Set classes;
	
	public VocabularySummarizer(Class vocabularyJavaClass) {
		this.vocabularyJavaClass = vocabularyJavaClass;
		properties = findAllProperties();
		classes = findAllClasses();
	}

	public Set getAllProperties() {
		return properties;
	}
	
	private Set findAllProperties() {
		Set results = new HashSet();
		for (int i = 0; i < vocabularyJavaClass.getFields().length; i++) {
			Field field = vocabularyJavaClass.getFields()[i];
			if (!Modifier.isStatic(field.getModifiers())) continue;
			if (!Property.class.isAssignableFrom(field.getType())) continue;
			try {
				results.add(field.get(null));
			} catch (IllegalAccessException ex) {
				throw new D2RQException(ex);
			}
		}
		return results;
	}
	
	public Set getAllClasses() {
		return classes;
	}
	
	private Set findAllClasses() {
		Set results = new HashSet();
		for (int i = 0; i < vocabularyJavaClass.getFields().length; i++) {
			Field field = vocabularyJavaClass.getFields()[i];
			if (!Modifier.isStatic(field.getModifiers())) continue;
			if (!Resource.class.isAssignableFrom(field.getType())) continue;
			if (Property.class.isAssignableFrom(field.getType())) continue;
			try {
				results.add(field.get(null));
			} catch (IllegalAccessException ex) {
				throw new D2RQException(ex);
			}
		}
		return results;
	}
}
