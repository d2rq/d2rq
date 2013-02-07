package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * A D2RQ mapping. Consists of {@link ClassMap}s,
 * {@link PropertyBridge}s, and several other classes.
 * 
 * TODO: Add getters to everything
 * TODO: Move TripleRelation/NodeMaker building and ConnectedDB to a separate class (MappingRunner?)
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Mapping {
	private static final Log log = LogFactory.getLog(Mapping.class);
	
	private final Model model = ModelFactory.createDefaultModel();
	
	/**
	 * Holds descriptions of the used classes and properties
	 */
	private final Model vocabularyModel = ModelFactory.createDefaultModel();

	private Resource mappingResource;

	private final Map<Resource,Database> databases = new HashMap<Resource,Database>();
	private Configuration configuration = new Configuration();
	private final Map<Resource,ClassMap> classMaps = new HashMap<Resource,ClassMap>();
	private final Map<Resource,TranslationTable> translationTables = new HashMap<Resource,TranslationTable>();
	private final Map<Resource,DownloadMap> downloadMaps = new HashMap<Resource,DownloadMap>();
	private final PrefixMapping prefixes = new PrefixMappingImpl();
	private Collection<TripleRelation> compiledPropertyBridges;
	
	public Mapping() {
		this(null);
	}
	
	public Mapping(String mappingURI) {
		if (mappingURI == null) {
			this.mappingResource = this.model.createResource();
		} else {
			this.mappingResource = this.model.createResource(mappingURI);
		}
	}
	
	public Resource resource() {
		return this.mappingResource;
	}

	public Model getVocabularyModel() {
		return vocabularyModel;
	}
	
	public void validate() throws D2RQException {
		if (this.databases.isEmpty()) {
			throw new D2RQException("No d2rq:Database defined in the mapping", 
					D2RQException.MAPPING_NO_DATABASE);
		}
		for (Database db: databases.values()) {
			db.validate();
		}
		for (TranslationTable table: translationTables.values()) {
			table.validate();
		}
		Collection<ClassMap> classMapsWithoutProperties = new ArrayList<ClassMap>(classMaps.values());
		for (ClassMap classMap: classMaps.values()) {
			classMap.validate();	// Also validates attached bridges
			if (classMap.hasProperties()) {
				classMapsWithoutProperties.remove(classMap);
			}
			for (PropertyBridge bridge: classMap.propertyBridges()) {
				if (bridge.refersToClassMap() != null) {
					classMapsWithoutProperties.remove(bridge.refersToClassMap());
				}
			}
		}
		if (!classMapsWithoutProperties.isEmpty()) {
			throw new D2RQException(classMapsWithoutProperties.iterator().next().toString() + 
					" has no d2rq:PropertyBridges and no d2rq:class",
					D2RQException.CLASSMAP_NO_PROPERTYBRIDGES);
		}
		for (DownloadMap dlm: downloadMaps.values()) {
			dlm.validate();
		}
		for (TripleRelation bridge: compiledPropertyBridges()) {
			new AttributeTypeValidator(bridge).validate();
		}
	}

	/**
	 * Connects all databases. This is done automatically if
	 * needed. The method can be used to test the connections
	 * earlier.
	 * 
	 * @throws D2RQException on connection failure
	 */
	public void connect() {
		if (connected) return;
		connected = true;
		for (Database db: databases()) {
			db.connectedDB().connection();
		}
		validate();
	}
	private boolean connected = false;

	public void close() {
		for (Database db: databases()) {
			db.connectedDB().close();
		}
	}
	
	public void addDatabase(Database database) {
		this.databases.put(database.resource(), database);
	}
	
	public Collection<Database> databases() {
		return this.databases.values();
	}
	
	public Database database(Resource name) {
		return databases.get(name);
	}
	
	public Configuration configuration() {
		return this.configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public void addClassMap(ClassMap classMap) {
		this.classMaps.put(classMap.resource(), classMap);
	}
	
	public Collection<Resource> classMapResources() {
		return this.classMaps.keySet();
	}
	
	public ClassMap classMap(Resource name) {
		return (ClassMap) this.classMaps.get(name);
	}
	
	public void addTranslationTable(TranslationTable table) {
		this.translationTables.put(table.resource(), table);
	}
	
	public TranslationTable translationTable(Resource name) {
		return (TranslationTable) this.translationTables.get(name);
	}
	
	public void addDownloadMap(DownloadMap downloadMap) {
		downloadMaps.put(downloadMap.resource(), downloadMap);
	}
	
	public Collection<Resource> downloadMapResources() {
		return downloadMaps.keySet();
	}
	
	public DownloadMap downloadMap(Resource name) {
		return downloadMaps.get(name);
	}
	
	/**
	 * @return A collection of {@link TripleRelation}s corresponding to each
	 * 		of the property bridges
	 */
	public synchronized Collection<TripleRelation> compiledPropertyBridges() {
		if (this.compiledPropertyBridges == null) {
			compilePropertyBridges();
		}
		return this.compiledPropertyBridges;
	}

	private void compilePropertyBridges() {
		/**
		 * validate temporarily disabled, see bug
		 * https://github.com/d2rq/d2rq/issues/194
		 *
		 * Not adding tests since new development in other branch
		 * but this patch reduces test errors from 92 to 38

		 validate();

		 */
		compiledPropertyBridges = new ArrayList<TripleRelation>();
		for (ClassMap classMap: classMaps.values()) {
			this.compiledPropertyBridges.addAll(classMap.compiledPropertyBridges());
		}
		log.info("Compiled " + compiledPropertyBridges.size() + " property bridges");
		if (log.isDebugEnabled()) {
			for (TripleRelation rel: compiledPropertyBridges) {
				log.debug(rel);
			}
		}
	}
	
	public PrefixMapping getPrefixMapping() {
		return prefixes;
	}

	private class AttributeTypeValidator {
		private final Relation relation;
		AttributeTypeValidator(TripleRelation relation) {
			this.relation = relation.baseRelation();
		}
		void validate() {
			for (Attribute attribute: relation.allKnownAttributes()) {
				DataType dataType = relation.database().columnType(
						relation.aliases().originalOf(attribute));
				if (dataType == null) {
					throw new D2RQException("Column " + relation.aliases().originalOf(attribute) +
							" has a datatype that is unknown to D2RQ; override it with d2rq:xxxColumn in the mapping file",
							D2RQException.DATATYPE_UNKNOWN);
				}
				if (dataType.isUnsupported()) {
					throw new D2RQException("Column " + 
							relation.aliases().originalOf(attribute) +
							" has a datatype that D2RQ cannot express in RDF: " + dataType, 
							D2RQException.DATATYPE_UNMAPPABLE);
				}
			}
		}
	}
	
	/**
	 * Helper method to add definitions from a ResourceMap to its underlying resource
	 * @param map
	 * @param targetResource
	 */
	private void addDefinitions(ResourceMap map, Resource targetResource) {
		/* Infer rdfs:Class or rdf:Property type */
		Statement s = vocabularyModel.createStatement(targetResource, RDF.type, map instanceof ClassMap ? RDFS.Class : RDF.Property);
		if (!this.vocabularyModel.contains(s))
			this.vocabularyModel.add(s);
		
		/* Apply labels */
		for (Literal propertyLabel: map.getDefinitionLabels()) {
			s = vocabularyModel.createStatement(targetResource, RDFS.label, propertyLabel);
			if (!this.vocabularyModel.contains(s))
				this.vocabularyModel.add(s);
		}

		/* Apply comments */
		for (Literal propertyComment: map.getDefinitionComments()) {
			s = vocabularyModel.createStatement(targetResource, RDFS.comment, propertyComment);
			if (!this.vocabularyModel.contains(s))
				this.vocabularyModel.add(s);
		}
		
		/* Apply additional properties */
		for (Resource additionalProperty: map.getAdditionalDefinitionProperties()) {
			s = vocabularyModel.createStatement(targetResource, 
						(Property)(additionalProperty.getProperty(D2RQ.propertyName).getResource().as(Property.class)),
						additionalProperty.getProperty(D2RQ.propertyValue).getObject());
			if (!this.vocabularyModel.contains(s))
				this.vocabularyModel.add(s);				
		}
	}
	
	/**
	 * Loads labels, comments and additional properties for referenced
	 * classes and properties and infers types
	 * Must be called after all classes and property bridges are loaded
	 */
	public void buildVocabularyModel() {
		for (ClassMap classMap: classMaps.values()) {
			
			/* Loop through referenced classes */
			for (Resource class_: classMap.getClasses()) {
				addDefinitions(classMap, class_);
			}

			/* Loop through property bridges */
			for (PropertyBridge bridge: classMap.propertyBridges()) {
				
				/* Loop through referenced properties */				
				for (Resource property: bridge.properties()) {
					addDefinitions(bridge, property);
				}
				
				// TODO: What to do about dynamic properties?
			}
		}
	}
}
