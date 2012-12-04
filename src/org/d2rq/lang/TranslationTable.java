package org.d2rq.lang;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.d2rq.D2RQException;
import org.d2rq.csv.TranslationTableParser;
import org.d2rq.values.Translator;
import org.d2rq.vocab.D2RQ;

import com.hp.hpl.jena.rdf.model.Resource;


/**
 * Represents a d2rq:TranslationTable.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author zazi (http://github.com/zazi)
 */
public class TranslationTable extends MapObject {
	private Collection<Translation> translations = new ArrayList<Translation>();
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

	public Collection<Translation> getTranslations() {
		return translations;
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
	
	public String getJavaClass() {
		return javaClass;
	}
	
	public void setHref(String href) {
		assertNotYetDefined(this.href, D2RQ.href, 
				D2RQException.TRANSLATIONTABLE_DUPLICATE_HREF);
		this.href = href;
	}
	
	public String getHref() {
		return href;
	}
	
	public Translator translator() {
		if (this.javaClass != null) {
			return instantiateJavaClass();
		}
		if (this.href != null) {
			return new TableTranslator(new TranslationTableParser(href).parseTranslations());			
		}
		return new TableTranslator(this.translations);
	}

	public void accept(D2RQMappingVisitor visitor) {
		visitor.visit(this);
	}

	public String toString() {
		return "d2rq:TranslationTable " + super.toString();
	}
	
	private Translator instantiateJavaClass() {
		try {
			Class<?> translatorClass = Class.forName(this.javaClass);
			if (!checkTranslatorClassImplementation(translatorClass)) {
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

	/**
	 * Checks whether the Translator class or a super class of it implements the
	 * Translator class interface.
	 * 
	 * @param translatorClass
	 *		a specific translator class or a more generic parent
	 * @return true, if the currently checked translator class implements the
	 *	 Translator class interface
	 */
	private boolean checkTranslatorClassImplementation(Class<?> translatorClass) {
		if (implementsTranslator(translatorClass)) {
			return true;
		}
		if (translatorClass.getSuperclass() == null) {
			return false;
		}
		return this.checkTranslatorClassImplementation(translatorClass
				.getSuperclass());
	}

	private boolean implementsTranslator(Class<?> aClass) {
		for (int i = 0; i < aClass.getInterfaces().length; i++) {
			if (aClass.getInterfaces()[i].equals(Translator.class)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasConstructorWithArg(Class<?> aClass) {
		try {
			aClass.getConstructor(new Class[]{Resource.class});
			return true;
		} catch (NoSuchMethodException nsmex) {
			return false;
		}
	}
	
	private Translator invokeConstructorWithArg(Class<?> aClass, Resource r) {
		try {
			Constructor<?> c = aClass.getConstructor(new Class[]{Resource.class});
			return (Translator) c.newInstance(new Object[]{r});
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private boolean hasConstructorWithoutArg(Class<?> aClass) {
		try {
			aClass.getConstructor(new Class[]{});
			return true;
		} catch (NoSuchMethodException nsmex) {
			return false;
		}
	}
	
	private Translator invokeConstructorWithoutArg(Class<?> aClass) {
		try {
			Constructor<?> c = aClass.getConstructor(new Class[]{});
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
		private Map<String,Translation> translationsByDBValue = new HashMap<String,Translation>();
		private Map<String,Translation> translationsByRDFValue = new HashMap<String,Translation>();
		TableTranslator(Collection<Translation> translations) {
			for (Translation translation: translations) {
				translationsByDBValue.put(translation.dbValue, translation);
				translationsByRDFValue.put(translation.rdfValue, translation);
			}
		}
		public String toDBValue(String rdfValue) {
			Translation translation = translationsByRDFValue.get(rdfValue);
			return (translation == null) ? null : translation.dbValue();
		}
		public String toRDFValue(String dbValue) {
			Translation translation = translationsByDBValue.get(dbValue);
			return (translation == null) ? null : translation.rdfValue();
		}
	}
}
