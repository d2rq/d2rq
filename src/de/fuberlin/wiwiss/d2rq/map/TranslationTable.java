package de.fuberlin.wiwiss.d2rq.map;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.csv.TranslationTableParser;
import de.fuberlin.wiwiss.d2rq.values.Translator;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * Translation table that maps a set of database values to a set of
 * RDF literals or URIs. The {@link #getTranslatingValueSource} method creates
 * a {@link ValueMaker} that can be chained with other ValueSources.
 * The RDF values can be either literals or URIs. The type of the
 * node that is actually generated is determined by a NodeMaker
 * that should sit on top of the translator. Mappings can be explicitly
 * provided, or a {@link Translator} instance can be used to map
 * values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TranslationTable.java,v 1.8 2006/09/12 12:06:18 cyganiak Exp $
 */
public class TranslationTable extends MapObject {
	private Collection translations = new ArrayList();
	private String javaClass = null;
	private String href = null;

	public TranslationTable(Resource resource) {
		super(resource);
	}

	/**
	 * Returns the number of defined mappings.
	 */
	public int size() {
		return this.translations.size();
	}

	/**
	 * Adds a translation mapping.
	 * @param dbValue the value on the database side (usually coming from a DB column)
	 * @param rdfValue the value on the RDF side (a string or URI)
	 */
	public void addTranslation(String dbValue, String rdfValue) {
		assertArgumentNotNull(dbValue, D2RQ.databaseValue, 
				D2RQException.TRANSLATION_MISSING_DBVALUE);
		assertArgumentNotNull(rdfValue, D2RQ.rdfValue, 
				D2RQException.TRANSLATION_MISSING_RDFVALUE);
		this.translations.add(new Translation(dbValue, rdfValue));
	}

	/**
	 * Sets a translation class. The translation class must implement
	 * the {@link Translator} interface. This method will take care
	 * of generating an instance of the class.
	 * @param className name of a class implementing {@link Translator}
	 */
	public void setJavaClass(String className) {
		assertNotYetDefined(this.javaClass, D2RQ.javaClass, 
				D2RQException.TRANSLATIONTABLE_DUPLICATE_JAVACLASS);
		this.javaClass = className;
	}
	
	public void setHref(String href) {
		assertNotYetDefined(this.href, D2RQ.href, 
				D2RQException.TRANSLATIONTABLE_DUPLICATE_HREF);
		this.href = href;
	}
	
	public Translator translator() {
		validate();
		if (this.javaClass != null) {
			return instantiateJavaClass();
		}
		if (this.href != null) {
			return new TableTranslator(new TranslationTableParser(href).parseTranslations());			
		}
		return new TableTranslator(this.translations);
	}

	public void validate() throws D2RQException {
		if (!this.translations.isEmpty() && this.javaClass != null) {
			throw new D2RQException("Can't combine d2rq:translation and d2rq:javaClass on " + this,
					D2RQException.TRANSLATIONTABLE_TRANSLATION_AND_JAVACLASS);
		}
		if (!this.translations.isEmpty() && this.href != null) {
			throw new D2RQException("Can't combine d2rq:translation and d2rq:href on " + this,
					D2RQException.TRANSLATIONTABLE_TRANSLATION_AND_HREF);
		}
		if (this.href != null && this.javaClass != null) {
			throw new D2RQException("Can't combine d2rq:href and d2rq:javaClass on " + this,
					D2RQException.TRANSLATIONTABLE_HREF_AND_JAVACLASS);
		}
	}
	
	public String toString() {
		return "d2rq:TranslationTable " + super.toString();
	}
	
	private Translator instantiateJavaClass() {
		try {
			Class translatorClass = Class.forName(this.javaClass);
			if (!implementsTranslator(translatorClass)) {
				throw new D2RQException("d2rq:javaClass " + this.javaClass + " must implement " + Translator.class.getName());
			}
			if (hasConstructorWithArg(translatorClass)) {
				return invokeConstructorWithArg(translatorClass, resource());
			}
			if (hasConstructorWithoutArg(translatorClass)) {
				return invokeConstructorWithoutArg(translatorClass);
			}
			throw new D2RQException("No suitable public constructor found on d2rq:javaClass " + this.javaClass);
		} catch (ClassNotFoundException e) {
			throw new D2RQException("d2rq:javaClass not on classpath: " + this.javaClass);
		}
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

	public static class Translation {
		private String dbValue;
		private String rdfValue;
		public Translation(String dbValue, String rdfValue) {
			this.dbValue = dbValue;
			this.rdfValue = rdfValue;
		}
		public String dbValue() { return this.dbValue; }
		public String rdfValue() { return this.rdfValue; }
		public int hashCode() { return this.dbValue.hashCode() ^ this.rdfValue.hashCode(); }
		public boolean equals(Object otherObject) {
			if (!(otherObject instanceof Translation)) return false;
			Translation other = (Translation) otherObject;
			return this.dbValue.equals(other.dbValue)
					&& this.rdfValue.equals(other.rdfValue);
		}
		public String toString() { 
			return "'" + this.dbValue + "'=>'" + this.rdfValue + "'";
		}
	}
	
	private class TableTranslator implements Translator {
		private Map translationsByDBValue = new HashMap();
		private Map translationsByRDFValue = new HashMap();
		TableTranslator(Collection translations) {
			Iterator it = translations.iterator();
			while (it.hasNext()) {
				Translation translation = (Translation) it.next();
				this.translationsByDBValue.put(translation.dbValue, translation);
				this.translationsByRDFValue.put(translation.rdfValue, translation);
			}
		}
		public String toDBValue(String rdfValue) {
			Translation translation = (Translation) this.translationsByRDFValue.get(rdfValue);
			return (translation == null) ? null : translation.dbValue();
		}
		public String toRDFValue(String dbValue) {
			Translation translation = (Translation) this.translationsByDBValue.get(dbValue);
			return (translation == null) ? null : translation.rdfValue();
		}
	}
}