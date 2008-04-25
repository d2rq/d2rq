package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * A D2RQ mapping. Consists of {@link ClassMap}s,
 * {@link PropertyBridge}s, and several other classes.
 * 
 * TODO: Add getters to everything and move Relation/NodeMaker building to a separate class
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Mapping.java,v 1.12 2008/04/25 11:25:05 cyganiak Exp $
 */
public class Mapping {
	private final Model model = ModelFactory.createDefaultModel();
	private Resource mappingResource;

	private final Map databases = new HashMap();
	private final Map classMaps = new HashMap();
	private final Map translationTables = new HashMap();
	private final PrefixMapping prefixes = new PrefixMappingImpl();
	private Collection compiledPropertyBridges;
	
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
		private final TripleRelation relation;
		AttributeTypeValidator(TripleRelation relation) {
			this.relation = relation;
		}
		void validate() {
			ConnectedDB db = relation.baseRelation().database();
			Iterator it = relation.projectionSpecs().iterator();
			while (it.hasNext()) {
				validateAttributes(((ProjectionSpec) it.next()).requiredAttributes(), db);
			}
			validateAttributes(relation.baseRelation().allKnownAttributes(), db);
		}
		private void validateAttributes(Collection attributes, ConnectedDB db) {
			Iterator it = attributes.iterator();
			while (it.hasNext()) {
				Attribute attribute = (Attribute) it.next();
				db.columnType(relation.baseRelation().aliases().originalOf(attribute));
			}
		}
	}
}
