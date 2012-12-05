package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.values.Pattern;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

public class ClassMap extends ResourceMap {
	private Resource resource;
	private Database database = null;
	private Collection<Resource> classes = new ArrayList<Resource>();
	private Collection<PropertyBridge> propertyBridges = new ArrayList<PropertyBridge>();
	private Collection<TripleRelation> compiledPropertyBridges = null;
	private Log log = LogFactory.getLog(ClassMap.class);
	
	public ClassMap(Resource classMapResource) {
		super(classMapResource, false);
		this.resource = classMapResource;
	}
	
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
	
	public Database database() {
		return this.database;
	}

	public void addClass(Resource class_) {
		this.classes.add(class_);
	}

	public void addPropertyBridge(PropertyBridge bridge) {
		this.propertyBridges.add(bridge);
	}

	public Collection<PropertyBridge> propertyBridges() {
		return this.propertyBridges;
	}
	
	public void validate() throws D2RQException {
		assertHasBeenDefined(this.database, D2RQ.dataStorage, D2RQException.CLASSMAP_NO_DATABASE);
		assertHasPrimarySpec(new Property[]{
				D2RQ.uriColumn, D2RQ.uriPattern, D2RQ.uriSqlExpression, D2RQ.bNodeIdColumns, D2RQ.constantValue
		});
		if (this.constantValue != null && this.constantValue.isLiteral()) {
			throw new D2RQException(
					"d2rq:constantValue for class map " + toString() + " must be a URI or blank node", 
					D2RQException.CLASSMAP_INVALID_CONSTANTVALUE);
		}
		if (this.uriPattern != null && new Pattern(uriPattern).attributes().size() == 0) {
			this.log.warn(toString() + " has an uriPattern without any column specifications. This usually happens when no primary keys are defined for a table. If the configuration is left as is, all table rows will be mapped to a single instance. " +
					"If this is not what you want, please define the keys in the database and re-run the mapping generator, or edit the mapping to provide the relevant keys.");
		}
		for (PropertyBridge bridge: propertyBridges) {
			bridge.validate();
		}
		// TODO
	}
	
	public boolean hasProperties() {
		return (!this.classes.isEmpty() || !this.propertyBridges.isEmpty());
	}
	
	public Collection<TripleRelation> compiledPropertyBridges() {
		if (this.compiledPropertyBridges == null) {
			compile();
		}
		return this.compiledPropertyBridges;
	}

	private void compile() {
		this.compiledPropertyBridges = new ArrayList<TripleRelation>();
		for (PropertyBridge bridge: propertyBridges) {
			this.compiledPropertyBridges.addAll(bridge.toTripleRelations());
		}
		for (Resource class_: classes) {
			PropertyBridge bridge = new PropertyBridge(this.resource);
			bridge.setBelongsToClassMap(this);
			bridge.addProperty(RDF.type);
			bridge.setConstantValue(class_);
			this.compiledPropertyBridges.addAll(bridge.toTripleRelations());
		}
	}
	
	protected Relation buildRelation() {
		return this.relationBuilder(database.connectedDB()).buildRelation();
	}
	
	public String toString() {
		return "d2rq:ClassMap " + PrettyPrinter.toString(this.resource);
	}
}
