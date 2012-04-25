package de.fuberlin.wiwiss.d2rq.values;

/**
 * Custom translator between database values and RDF values.
 * Implementations of this interface can be used within
 * d2rq:TranslationTables.
 * <p>
 * A Translator defines a 1:1
 * mapping between database and RDF values. Mappings that
 * are not 1:1 in both directions are not supported.
 * <p>
 * The type
 * of the RDF node (URI, blank node, literal) is not
 * specified by the translator, but by the d2rq:ClassMap
 * or d2rq:PropertyBridge that uses the d2rq:TranslationTable.
 * <p>
 * Translator implementations can have two kinds of constructors:
 * <ol>
 * <li>A constructor that takes a single argument, a Jena
 *   <a href="http://www.hpl.hp.com/semweb/javadoc/com/hp/hpl/mesa/rdf/jena/model/Resource.htmlhttp://www.hpl.hp.com/semweb/javadoc/com/hp/hpl/mesa/rdf/jena/model/Resource.html">Resource</a>.
 *   A resource representing the d2rq:TranslationTable will be passed to the
 *   constructor and can be used to retrieve further setup arguments from
 *   the mapping file.</li>
 * <li>A constructor that takes no arguments.</li>  
 * Translators are instantiated at startup time, not at query time. Performance
 * is not critical.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface Translator {

	public static Translator IDENTITY = new Translator() {
		public String toRDFValue(String dbValue) { return dbValue; }
		public String toDBValue(String rdfValue) { return rdfValue; }
		public String toString() { return "identity"; }
	};
	
	/**
	 * Translates a value that comes from the database to an RDF value
	 * (URI, literal label, or blank node ID). The mapping must be unique.
	 * 
	 * @param dbValue a value coming from the database
	 * @return the corresponding RDF value, or <tt>null</tt> if no
	 *     RDF statements should be created from the database value
	 */
	public String toRDFValue(String dbValue);
	
	/**
	 * Translates a value that comes from an RDF source (for example a query)
	 * to a database value. The mapping must be unique.
	 *  
	 * @param rdfValue a value coming from an RDF source
	 * @return the corresponding database value, or <tt>null</tt> if the
	 * 		RDF value cannot be mapped to a database value
	 */
	public String toDBValue(String rdfValue);
}
