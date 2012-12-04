package org.d2rq.lang;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.d2rq.vocab.D2RQ;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;


/**
 * A D2RQ mapping. Consists of {@link ClassMap}s,
 * {@link PropertyBridge}s, and several other classes.
 * 
 * TODO: Add getters to everything
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Mapping extends MapObject {

	private final PrefixMapping prefixes = new PrefixMappingImpl();
	private final Map<Resource,Database> databases = new HashMap<Resource,Database>();
	private Configuration configuration = new Configuration();
	private final Map<Resource,ClassMap> classMaps = new HashMap<Resource,ClassMap>();
	private final Map<Resource,TranslationTable> translationTables = new HashMap<Resource,TranslationTable>();
	private final Map<Resource,DownloadMap> downloadMaps = new HashMap<Resource,DownloadMap>();
	/** Holds descriptions of the used classes and properties */
	private final Model vocabularyModel = ModelFactory.createDefaultModel();

	public Mapping() {
		this(null);
	}

	public Mapping(String mappingURI) {
		super(mappingURI == null ? ResourceFactory.createResource() : 
			ResourceFactory.createResource(mappingURI));
	}

	public CompiledD2RQMapping compile() {
		CompiledD2RQMapping result = new D2RQCompiler(this).getResult();
		result.connect();
		return result;
	}
	
	public PrefixMapping getPrefixes() {
		return prefixes;
	}

	public Model getVocabularyModel() {
		return vocabularyModel;
	}
	
	public void accept(D2RQMappingVisitor visitor) {
		if (!visitor.visitEnter(this)) return;
		configuration.accept(visitor);
		for (Database database: databases.values()) {
			database.accept(visitor);
		}
		for (TranslationTable translationTable: translationTables.values()) {
			translationTable.accept(visitor);
		}
		for (ClassMap classMap: classMaps.values()) {
			classMap.accept(visitor);
		}
		for (DownloadMap downloadMap: downloadMaps.values()) {
			downloadMap.accept(visitor);
		}
		visitor.visitLeave(this);
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
				for (Resource property: bridge.getProperties()) {
					addDefinitions(bridge, property);
				}

				// TODO: What to do about dynamic properties?
			}
		}
	}
}