package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.rdql.GraphUtils;

/**
 * A D2RQ mapping. Consists of {@link ClassMap}s,
 * {@link PropertyBridge}s, and several other classes.
 * 
 * TODO: Add getters to everything and move Relation/NodeMaker building to a separate class
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Mapping.java,v 1.4 2006/09/13 06:37:07 cyganiak Exp $
 */
public class Mapping {
	private Model model = ModelFactory.createDefaultModel();
	private Resource mappingResource;

	private Map databases = new HashMap();
	private Map classMaps = new HashMap();
	private Map translationTables = new HashMap();
	private Map processingInstructions = new HashMap();
	private Collection compiledPropertyBridges;
	private Map compiledPropertyBridgesByDatabase;
	
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
		it = this.compiledPropertyBridges.iterator();
		while (it.hasNext()) {
			RDFRelation bridge = (RDFRelation) it.next();
			assertHasColumnTypes(bridge);
		}
	}

	private void assertHasColumnTypes(RDFRelation relation) {
		Iterator it = relation.projectionColumns().iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			relation.baseRelation().database().assertHasType(
					relation.baseRelation().aliases().originalOf(column));			
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
	
	public void setProcessingInstruction(Property key, String value) {
		this.processingInstructions.put(key, value);
	}
	
	public String processingInstruction(Property property) {
		return (String) this.processingInstructions.get(property);
	}
	
	/**
	 * @return A collection of {@link RDFRelation}s corresponding to each
	 * 		of the property bridges
	 */
	public Collection compiledPropertyBridges() {
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
		this.compiledPropertyBridgesByDatabase = 
				GraphUtils.makeDatabaseMapFromPropertyBridges(
						this.compiledPropertyBridges);
	}

	public Map compiledPropertyBridgesByDatabase() {
		if (this.compiledPropertyBridgesByDatabase == null) {
			compilePropertyBridges();
		}
		return this.compiledPropertyBridgesByDatabase;
	}
}
