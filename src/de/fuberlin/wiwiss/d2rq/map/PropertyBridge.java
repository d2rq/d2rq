package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.parser.RelationBuilder;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

public class PropertyBridge extends ResourceMap {
	private Resource resource;
	private ClassMap belongsToClassMap = null;
	private Collection properties = new HashSet();
	
	public PropertyBridge(Resource resource) {
		super(resource, true);
		this.resource = resource;
	}
	
	public Resource resource() {
		return this.resource;
	}

	public void setBelongsToClassMap(ClassMap classMap) {
		assertNotYetDefined(this.belongsToClassMap, D2RQ.belongsToClassMap, D2RQException.PROPERTYBRIDGE_DUPLICATE_BELONGSTOCLASSMAP);
		assertArgumentNotNull(classMap, D2RQ.belongsToClassMap, D2RQException.PROPERTYBRIDGE_INVALID_BELONGSTOCLASSMAP);
		this.belongsToClassMap = classMap;
	}
	
	public void setColumn(String column) {
		assertNotYetDefined(this.column, D2RQ.column, D2RQException.PROPERTYBRIDGE_DUPLICATE_COLUMN);
		this.column = column;
	}

	public void setPattern(String pattern) {
		assertNotYetDefined(this.pattern, D2RQ.pattern, D2RQException.PROPERTYBRIDGE_DUPLICATE_PATTERN);
		this.pattern = pattern;
	}

	public void setDatatype(String datatype) {
		assertNotYetDefined(this.datatype, D2RQ.datatype, D2RQException.PROPERTYBRIDGE_DUPLICATE_DATATYPE);
		this.datatype = datatype;
	}

	public void setLang(String lang) {
		assertNotYetDefined(this.lang, D2RQ.lang, D2RQException.PROPERTYBRIDGE_DUPLICATE_LANG);
		this.lang = lang;
	}
	
	public void setValue(RDFNode value) {
		assertNotYetDefined(this.value, valueProperty, D2RQException.PROPERTYBRIDGE_DUPLICATE_VALUE);
		this.value = value;
	}
	
	public void setRefersToClassMap(ClassMap classMap) {
		assertNotYetDefined(this.refersToClassMap, D2RQ.refersToClassMap, 
				D2RQException.PROPERTYBRIDGE_DUPLICATE_REFERSTOCLASSMAP);
		assertArgumentNotNull(classMap, D2RQ.refersToClassMap, D2RQException.PROPERTYBRIDGE_INVALID_REFERSTOCLASSMAP);
		this.refersToClassMap = classMap;
	}

	public void addProperty(Resource property) {
		this.properties.add(property.as(Property.class));
	}

	public void validate() throws D2RQException {
		if (this.refersToClassMap != null) {
			if (!this.refersToClassMap.database().equals(this.belongsToClassMap.database())) {
				throw new D2RQException(toString() + 
						" links two d2rq:ClassMaps with different d2rq:dataStorages",
						D2RQException.PROPERTYBRIDGE_CONFLICTING_DATABASES);
			}
		}
		assertHasPrimarySpec(new Property[]{
				D2RQ.uriColumn, D2RQ.uriPattern, D2RQ.bNodeIdColumns,
				D2RQ.column, D2RQ.pattern, valueProperty,
				D2RQ.refersToClassMap
		});
		if (this.datatype != null && this.lang != null) {
			throw new D2RQException(toString() + " has both d2rq:datatype and d2rq:lang",
					D2RQException.PROPERTYBRIDGE_LANG_AND_DATATYPE);
		}
		if (this.datatype != null && this.column == null && this.pattern == null) {
			throw new D2RQException("d2rq:datatype can only be used with d2rq:column and d2rq:pattern at " + this,
					D2RQException.PROPERTYBRIDGE_NONLITERAL_WITH_DATATYPE);
		}
		if (this.lang != null && this.column == null && this.pattern == null) {
			throw new D2RQException("d2rq:lang can only be used with d2rq:column and d2rq:pattern at " + this,
					D2RQException.PROPERTYBRIDGE_NONLITERAL_WITH_LANG);
		}
	}

	protected Relation buildRelation() {
		RelationBuilder builder = relationBuilder();
		builder.addOther(this.belongsToClassMap.relationBuilder());
		if (this.refersToClassMap != null) {
			builder.addAliased(this.refersToClassMap.relationBuilder());
		}
		return builder.buildRelation(this.belongsToClassMap.database().connectedDB()); 
	}

	public NodeMaker nodeMaker() {
		// TODO: MySQL DateTime hack -- how to do this properly?
		if (this.belongsToClassMap.database() != null
				&& this.translateWith == null
				&& this.datatype != null
				&& this.datatype.equals(XSDDatatype.XSDdateTime.getURI())
				&& this.column != null
				&& this.belongsToClassMap.database().connectedDB().columnType(this.column) == ConnectedDB.DATE_COLUMN) {
//			this.translateWith = new TranslationTable();
//			this.translateWith.setTranslator(new DateTimeTranslator());
		}
		return super.nodeMaker();
	}
	
	public Collection toRDFRelations() {
		this.validate();
		Collection results = new ArrayList();
		Iterator it = this.properties.iterator();
		while (it.hasNext()) {
			Property property = (Property) it.next();
			results.add(new RDFRelationImpl(
					buildRelation(),
					this.belongsToClassMap.nodeMaker(),
					new FixedNodeMaker(property.asNode(), false),
					nodeMaker()));
		}
		return results;
	}
	
	public String toString() {
		return "d2rq:PropertyBridge " + PrettyPrinter.toString(this.resource);		
	}
}