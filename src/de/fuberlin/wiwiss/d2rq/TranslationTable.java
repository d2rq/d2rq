/*
 * $Id: TranslationTable.java,v 1.1 2004/08/02 22:48:44 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Translation table that maps a set of database values to a set of
 * RDF literals or URIs. The {@link #getTranslatingValueSource} method creates
 * a {@link ValueSource} that can be chained with other ValueSources.
 * The RDF values can be either literals or URIs. The type of the
 * node that is actually generated is determined by a NodeMaker
 * that should sit on top of the translator. Mappings can be explicitly
 * provided, or a {@link Translator} instance can be used to map
 * values.
 * 
 * TODO: The two cases (addTranslation calls and setTranslator) should
 * probalby be in separate classes.
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 */
class TranslationTable implements Translator {
	private Map db2rdf = new HashMap();
	private Map rdf2db = new HashMap();
	private Translator translatorInstance;

	public TranslationTable() {
		this.translatorInstance = this;
	}

	/**
	 * Returns the number of defined mappings.
	 */
	public int size() {
		return this.db2rdf.size();
	}

	/**
	 * Adds a translation mapping.
	 * @param dbValue the value on the database side (usually coming from a DB column)
	 * @param rdfValue the value on the RDF side (a string or URI)
	 */
	public void addTranslation(String dbValue, String rdfValue) {
		this.db2rdf.put(dbValue, rdfValue);
		this.rdf2db.put(rdfValue, dbValue);
	}

	/**
	 * Sets a translation class. The translation class must implement
	 * the {@link Translator} interface. This method will take care
	 * of generating an instance of the class.
	 * @param className name of a class implementing {@link Translator}
	 * @param resource the node in the D2RQ map where the d2rq:javaClass
	 * 		statement was found
	 */
	public void setTranslatorClass(String className, Resource resource) {
		try {
			Class translatorClass = Class.forName(className);
			if (!implementsTranslator(translatorClass)) {
				Logger.instance().error("d2rq:javaClass " + className + " must implement " + Translator.class.getName());
				return;
			}
			if (hasConstructorWithArg(translatorClass)) {
				setTranslator(invokeConstructorWithArg(translatorClass, resource));
				return;
			}
			if (hasConstructorWithoutArg(translatorClass)) {
				setTranslator(invokeConstructorWithoutArg(translatorClass));
				return;
			}
			Logger.instance().error("No suitable public constructor found on d2rq:javaClass " + className);
		} catch (ClassNotFoundException e) {
			Logger.instance().error("d2rq:javaClass not on classpath: " + className);
		}
	}

	/**
	 * Sets a Translator.
	 */
	public void setTranslator(Translator translator) {
		this.translatorInstance = translator;
	}

	private boolean implementsTranslator(Class aClass) {
		for (int i = 0; i < aClass.getInterfaces().length; i++) {
			if (aClass.getInterfaces()[i].equals(Translator.class)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasConstructorWithArg(Class aClass) {
		try {
			aClass.getConstructor(new Class[]{Resource.class});
			return true;
		} catch (NoSuchMethodException nsmex) {
			return false;
		}
	}
	
	private Translator invokeConstructorWithArg(Class aClass, Resource r) {
		try {
			Constructor c = aClass.getConstructor(new Class[]{Resource.class});
			return (Translator) c.newInstance(new Object[]{r});
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private boolean hasConstructorWithoutArg(Class aClass) {
		try {
			aClass.getConstructor(new Class[]{});
			return true;
		} catch (NoSuchMethodException nsmex) {
			return false;
		}
	}
	
	private Translator invokeConstructorWithoutArg(Class aClass) {
		try {
			Constructor c = aClass.getConstructor(new Class[]{});
			return (Translator) c.newInstance(new Object[]{});
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Adds multiple translation mappings.
	 * @param translationMap a map of DB => RDF mappings (both strings)
	 */
	public void addAll(Map translationMap) {
		Iterator it = translationMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String value = (String) translationMap.get(key);
			addTranslation(key, value);
		}
	}

	/**
	 * Translates an RDF value to a database value.
	 */
	public String toDBValue(String rdfValue) {
		return (String) this.rdf2db.get(rdfValue);
	}
	
	/**
	 * Translates a database value to a RDF value. The RDF value is
	 * returned as a String. A NodeMaker should be used to create
	 * an RDF node from it. 
	 */
	public String toRDFValue(String dbValue) {
		return (String) this.db2rdf.get(dbValue);
	}
	
	/**
	 * Creates a new ValueSource that translates values from the
	 * argument ValueSource according to the mappings in this
	 * TranslationTable.
	 * @param valueSource the ValueSource whose values should be translated
	 * @return a new ValueSource that delivers the mapped values
	 */
	public ValueSource getTranslatingValueSource(ValueSource valueSource) {
		return new TranslatingValueSource(valueSource, this.translatorInstance);
	}

	private class TranslatingValueSource implements ValueSource {
		private ValueSource valueSource;
		private Translator translator;

		private TranslatingValueSource(ValueSource valueSource,
				Translator translator) {
			this.valueSource = valueSource;
			this.translator = translator;
		}

		public boolean couldFit(String value) {
			String dbValue = this.translator.toDBValue(value);
			return dbValue != null && this.valueSource.couldFit(dbValue);
		}

		public Set getColumns() {
			return this.valueSource.getColumns();
		}

		public Map getColumnValues(String value) {
			return this.valueSource.getColumnValues(
					this.translator.toDBValue(value));
		}

		public String getValue(String[] row, Map columnNames) {
			return this.translator.toRDFValue(
					this.valueSource.getValue(row, columnNames));
		}
	}
}