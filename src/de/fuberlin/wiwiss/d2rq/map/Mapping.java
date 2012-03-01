package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * A D2RQ mapping. Consists of {@link ClassMap}s,
 * {@link PropertyBridge}s, and several other classes.
 * 
 * TODO: Add getters to everything and move Relation/NodeMaker building to a separate class
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Mapping {
	private final Model model = ModelFactory.createDefaultModel();
	
	/**
	 * Holds descriptions of the used classes and properties
	 */
	private final Model vocabularyModel = ModelFactory.createDefaultModel();

	private Resource mappingResource;

	private final Map databases = new HashMap();
	private Configuration configuration = new Configuration();
	private final Map classMaps = new HashMap();
	private final Map translationTables = new HashMap();
	private final Map<Resource,DownloadMap> downloadMaps = new HashMap<Resource,DownloadMap>();
	private final PrefixMapping prefixes = new PrefixMappingImpl();
	private Collection compiledPropertyBridges;
	private boolean hasDynamicProperties = false;
	
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
		Iterator it = this.databases.values().iterator();
		while (it.hasNext()) {
			Database db = (Database) it.next();
			db.validate();
		}
		it = this.translationTables.values().iterator();
		while (it.hasNext()) {
			TranslationTable table = (TranslationTable) it.next();
			table.validate();
		}
		it = this.classMaps.values().iterator();
		while (it.hasNext()) {
			ClassMap classMap = (ClassMap) it.next();
			classMap.validate();	// Also validates attached bridges
		}
		for (DownloadMap dlm : downloadMaps.values()) {
			dlm.validate();
		}
		it = compiledPropertyBridges().iterator();
		while (it.hasNext()) {
			TripleRelation bridge = (TripleRelation) it.next();
			new AttributeTypeValidator(bridge).validate();
		}
	}

	public void addDatabase(Database database) {
		this.databases.put(database.resource(), database);
	}
	
	public Collection databases() {
		return this.databases.values();
	}
	
	public Database database(Resource name) {
		return (Database) this.databases.get(name);
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
	
	public Collection classMapResources() {
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
	public synchronized Collection compiledPropertyBridges() {
		if (this.compiledPropertyBridges == null) {
			compilePropertyBridges();
		}
		return this.compiledPropertyBridges;
	}

	private void compilePropertyBridges() {
		this.compiledPropertyBridges = new ArrayList();
		Iterator it = this.classMaps.values().iterator();
		while (it.hasNext()) {
			ClassMap classMap = (ClassMap) it.next();
			this.compiledPropertyBridges.addAll(classMap.compiledPropertyBridges());
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
			Iterator it = relation.allKnownAttributes().iterator();
			while (it.hasNext()) {
				Attribute attribute = (Attribute) it.next();
				relation.database().columnType(relation.aliases().originalOf(attribute));
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
		Iterator it = map.getDefinitionLabels().iterator();
		while (it.hasNext()) {
			Literal propertyLabel = (Literal) it.next();
			s = vocabularyModel.createStatement(targetResource, RDFS.label, propertyLabel);
			if (!this.vocabularyModel.contains(s))
				this.vocabularyModel.add(s);
		}

		/* Apply comments */
		it = map.getDefinitionComments().iterator();
		while (it.hasNext()) {
			Literal propertyComment = (Literal) it.next();
			s = vocabularyModel.createStatement(targetResource, RDFS.comment, propertyComment);
			if (!this.vocabularyModel.contains(s))
				this.vocabularyModel.add(s);
		}
		
		/* Apply additional properties */
		it = map.getAdditionalDefinitionProperties().iterator();
		while (it.hasNext()) {
			Resource additionalProperty = (Resource) it.next();
			s = vocabularyModel.createStatement(targetResource, 
						(Property)(additionalProperty.getProperty(D2RQ.propertyName).getResource().as(Property.class)),
						additionalProperty.getProperty(D2RQ.propertyValue).getObject());
			if (!this.vocabularyModel.contains(s))
				this.vocabularyModel.add(s);				
		}	}
	
	/**
	 * Loads labels, comments and additional properties for referenced
	 * classes and properties and infers types
	 * Must be called after all classes and property bridges are loaded
	 */
	public void buildVocabularyModel() {
		Iterator itClassMaps = this.classMaps.values().iterator();
		while (itClassMaps.hasNext()) {
			ClassMap classMap = (ClassMap) itClassMaps.next();
			
			/* Loop through referenced classes */
			Iterator itClasses = classMap.getClasses().iterator();
			while (itClasses.hasNext()) {
				Resource class_ = (Resource) itClasses.next();
				addDefinitions(classMap, class_);
			}

			/* Loop through property bridges */
			Iterator itPropertyBridges = classMap.propertyBridges().iterator();
			while (itPropertyBridges.hasNext()) {
				PropertyBridge bridge = (PropertyBridge) itPropertyBridges.next();
				
				/* Loop through referenced properties */				
				Iterator itProperties = bridge.properties().iterator();
				while (itProperties.hasNext()) {
					Object prop = itProperties.next();
					if(prop instanceof Property)
					{
						Resource property = (Resource) prop;
						addDefinitions(bridge, property);
					}
				}
			}
		}
	}

	public boolean getHasDynamicProperties() {
		return hasDynamicProperties;
	}

	public void setHasDynamicProperties(boolean hasDynamicProperties) {
		this.hasDynamicProperties = hasDynamicProperties;
	}
}
