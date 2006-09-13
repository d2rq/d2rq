package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

public class ClassMap extends ResourceMap {
	private Resource resource;
	private Database database = null;
	private Collection classes = new ArrayList();
	private Collection propertyBridges = new ArrayList();
	private Collection compiledPropertyBridges = null;
	
	public ClassMap(Resource classMapResource) {
		super(classMapResource, false);
		this.resource = classMapResource;
	}
	
	public Resource resource() {
		return this.resource;
	}
	
	public void setDatabase(Database database) {
		assertNotYetDefined(this.database, D2RQ.dataStorage, D2RQException.CLASSMAP_DUPLICATE_DATABASE);
		assertArgumentNotNull(database, D2RQ.dataStorage, D2RQException.CLASSMAP_INVALID_DATABASE);
		this.database = database;
	}
	
	public Database database() {
		return this.database;
	}

	public void addClass(Resource class_) {
		this.classes.add(class_);
	}

	// TODO: Introduce an AdditionalProperty class, or delete and make d2rq:value work
	public void addAdditionalProperty(Resource property, RDFNode value) {
		PropertyBridge bridge = new PropertyBridge(this.resource);
		bridge.setBelongsToClassMap(this);
		bridge.addProperty(property);
		bridge.setValue(value);
		addPropertyBridge(bridge);
	}
	
	public void addPropertyBridge(PropertyBridge bridge) {
		this.propertyBridges.add(bridge);
	}

	public Collection propertyBridges() {
		return this.propertyBridges;
	}
	
	public void validate() throws D2RQException {
		assertHasBeenDefined(this.database, D2RQ.dataStorage, D2RQException.CLASSMAP_NO_DATABASE);
		assertHasPrimarySpec(new Property[]{
				D2RQ.uriColumn, D2RQ.uriPattern, D2RQ.bNodeIdColumns
		});
		if (this.classes.isEmpty() && this.propertyBridges.isEmpty()) {
			throw new D2RQException(toString() + 
					" has no d2rq:PropertyBridges and no d2rq:class",
					D2RQException.CLASSMAP_NO_PROPERTYBRIDGES);
		}
		Iterator it = this.propertyBridges.iterator();
		while (it.hasNext()) {
			PropertyBridge bridge = (PropertyBridge) it.next();
			bridge.validate();
		}
		// TODO
	}
	
	public Collection compiledPropertyBridges() {
		if (this.compiledPropertyBridges == null) {
			compile();
		}
		return this.compiledPropertyBridges;
	}

	private void compile() {
		this.compiledPropertyBridges = new ArrayList();
		Iterator it = this.propertyBridges.iterator();
		while (it.hasNext()) {
			PropertyBridge bridge = (PropertyBridge) it.next();
			this.compiledPropertyBridges.addAll(bridge.toRDFRelations());
		}
		it = this.classes.iterator();
		while (it.hasNext()) {
			Resource class_ = (Resource) it.next();
			PropertyBridge bridge = new PropertyBridge(this.resource);
			bridge.setBelongsToClassMap(this);
			bridge.addProperty(RDF.type);
			bridge.setValue(class_);
			this.compiledPropertyBridges.addAll(bridge.toRDFRelations());
		}
	}
	
	protected Relation buildRelation() {
		return this.relationBuilder().buildRelation(this.database);
	}
	
	public String toString() {
		return "d2rq:ClassMap " + PrettyPrinter.toString(this.resource);		
	}
}
