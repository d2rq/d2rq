package org.d2rq.lang;

import java.util.ArrayList;
import java.util.Collection;

import org.d2rq.D2RQException;
import org.d2rq.pp.PrettyPrinter;
import org.d2rq.vocab.D2RQ;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


/**
 * Java class corresponding to d2rq:ClassMap.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ClassMap extends ResourceMap {
	
	/**
	 * Convenience method for creating class maps.
	 * 
	 * @param id Identifier for the class map; may be <code>null</code>
	 * @param uriPattern URI pattern
	 * @param mapping Mapping the class map belongs to; if it has exactly one database, that will be used too
	 */
	public static ClassMap create(Resource id, String uriPattern, Mapping mapping) {
		ClassMap result = new ClassMap(id == null ? ResourceFactory.createResource() : id);
		result.setURIPattern(uriPattern);
		if (mapping.databases().size() == 1) {
			result.setDatabase(mapping.databases().iterator().next());
		}
		mapping.addClassMap(result);
		return result;
	}
	
	private Resource resource;
	private Database database = null;
	private Collection<Resource> classes = new ArrayList<Resource>();
	private Collection<PropertyBridge> propertyBridges = new ArrayList<PropertyBridge>();
	
	public ClassMap(Resource classMapResource) {
		super(classMapResource, false);
		this.resource = classMapResource;
	}
	
	@Override
	public Resource resource() {
		return this.resource;
	}
	
	public Collection<Resource> getClasses() {
		return classes;
	}
	
	public void setDatabase(Database database) {
		assertNotYetDefined(this.database, D2RQ.dataStorage, D2RQException.CLASSMAP_DUPLICATE_DATABASE);
		assertArgumentNotNull(database, D2RQ.dataStorage, D2RQException.CLASSMAP_INVALID_DATABASE);
		this.database = database;
	}
	
	public Database getDatabase() {
		return this.database;
	}

	public void addClass(Resource class_) {
		this.classes.add(class_);
	}

	/**
	 * Adds a property bridge to this class map. Usually not invoked directly
	 * but through {@link PropertyBridge#setBelongsToClassMap(ClassMap)}.
	 */
	public void addPropertyBridge(PropertyBridge bridge) {
		this.propertyBridges.add(bridge);
	}

	public Collection<PropertyBridge> propertyBridges() {
		return this.propertyBridges;
	}
	
	public void accept(D2RQMappingVisitor visitor) {
		if (!visitor.visitEnter(this)) return;
		for (PropertyBridge propertyBridge: propertyBridges) {
			propertyBridge.accept(visitor);
		}
		visitor.visitLeave(this);
	}
	
	@Override
	public String toString() {
		return "d2rq:ClassMap " + PrettyPrinter.toString(this.resource);
	}
}
