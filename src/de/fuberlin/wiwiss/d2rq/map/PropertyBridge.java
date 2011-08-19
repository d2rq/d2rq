package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.parser.RelationBuilder;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.sql.SQL;
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

	public Collection properties() {
		return this.properties;
	}
	
    public ClassMap getBelongsToClassMap() {
        return belongsToClassMap;
    }

	public void setBelongsToClassMap(ClassMap classMap) {
		assertNotYetDefined(this.belongsToClassMap, D2RQ.belongsToClassMap, D2RQException.PROPERTYBRIDGE_DUPLICATE_BELONGSTOCLASSMAP);
		assertArgumentNotNull(classMap, D2RQ.belongsToClassMap, D2RQException.PROPERTYBRIDGE_INVALID_BELONGSTOCLASSMAP);
		this.belongsToClassMap = classMap;
	}
	
    public String getColumn() {
        return column;
    }

	public void setColumn(String column) {
		assertNotYetDefined(this.column, D2RQ.column, D2RQException.PROPERTYBRIDGE_DUPLICATE_COLUMN);
		this.column = column;
	}

    public String getPattern() {
        return pattern;
    }

	public void setPattern(String pattern) {
		assertNotYetDefined(this.pattern, D2RQ.pattern, D2RQException.PROPERTYBRIDGE_DUPLICATE_PATTERN);
		this.pattern = pattern;
	}

    public String getSQLExpression() {
        return sqlExpression;
    }

	public void setSQLExpression(String sqlExpression) {
		assertNotYetDefined(this.column, D2RQ.sqlExpression, D2RQException.PROPERTYBRIDGE_DUPLICATE_SQL_EXPRESSION);
		this.sqlExpression = sqlExpression;
	}

    public String getUriSQLExpression() {
        return uriSqlExpression;
    }
	
	public void setUriSQLExpression(String uriSqlExpression) {
		assertNotYetDefined(this.column, D2RQ.uriSqlExpression, D2RQException.PROPERTYBRIDGE_DUPLICATE_URI_SQL_EXPRESSION);
		this.uriSqlExpression = uriSqlExpression;
	}
	
    public String getDatatype() {
        return datatype;
    }

	public void setDatatype(String datatype) {
		assertNotYetDefined(this.datatype, D2RQ.datatype, D2RQException.PROPERTYBRIDGE_DUPLICATE_DATATYPE);
		this.datatype = datatype;
	}

    public String getLang() {
        return lang;
    }

	public void setLang(String lang) {
		assertNotYetDefined(this.lang, D2RQ.lang, D2RQException.PROPERTYBRIDGE_DUPLICATE_LANG);
		this.lang = lang;
	}

    public int getLimit() {
        return limit.intValue();
    }
	
	public void setLimit(int limit) {
	    assertNotYetDefined(this.limit, D2RQ.limit, D2RQException.PROPERTYBRIDGE_DUPLICATE_LIMIT);
	    this.limit = new Integer(limit);
	}

    public int getLimitInverse() {
        return limitInverse.intValue();
    }

	public void setLimitInverse(int limit) {
	    assertNotYetDefined(this.limitInverse, D2RQ.limitInverse, D2RQException.PROPERTYBRIDGE_DUPLICATE_LIMITINVERSE);
	    this.limitInverse = new Integer(limit);
	}

	public void setOrder(String column, boolean desc) {
	    assertNotYetDefined(this.order, (desc ? D2RQ.orderDesc : D2RQ.orderAsc), D2RQException.PROPERTYBRIDGE_DUPLICATE_ORDER);
	    this.order = column;
	    this.orderDesc = new Boolean(desc);
	}
    
    public ClassMap getRefersToClassMap() {
        return refersToClassMap;
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
	
	public void addProperty(DynamicProperty propertyMap) {
		this.properties.add(propertyMap);
	}

	public void validate() throws D2RQException {
		if (this.refersToClassMap != null) {
			if (!this.refersToClassMap.database().equals(this.belongsToClassMap.database())) {
				throw new D2RQException(toString() + 
						" links two d2rq:ClassMaps with different d2rq:dataStorages",
						D2RQException.PROPERTYBRIDGE_CONFLICTING_DATABASES);
			}
			// TODO refersToClassMap cannot be combined w/ value constraints or translation tables
		}
		assertHasPrimarySpec(new Property[]{
				D2RQ.uriColumn, D2RQ.uriPattern, D2RQ.bNodeIdColumns,
				D2RQ.column, D2RQ.pattern, D2RQ.sqlExpression, D2RQ.uriSqlExpression, D2RQ.constantValue,
				D2RQ.refersToClassMap
		});
		if (this.datatype != null && this.lang != null) {
			throw new D2RQException(toString() + " has both d2rq:datatype and d2rq:lang",
					D2RQException.PROPERTYBRIDGE_LANG_AND_DATATYPE);
		}
		if (this.datatype != null && this.column == null && this.pattern == null
				&& this.sqlExpression == null) {
			throw new D2RQException("d2rq:datatype can only be used with d2rq:column, d2rq:pattern " +
					"or d2rq:sqlExpression at " + this,
					D2RQException.PROPERTYBRIDGE_NONLITERAL_WITH_DATATYPE);
		}
		if (this.lang != null && this.column == null && this.pattern == null) {
			throw new D2RQException("d2rq:lang can only be used with d2rq:column, d2rq:pattern " +
					"or d2rq:sqlExpression at " + this,
					D2RQException.PROPERTYBRIDGE_NONLITERAL_WITH_LANG);
		}
	}

	protected Relation buildRelation() {
		RelationBuilder builder = relationBuilder();
		builder.addOther(this.belongsToClassMap.relationBuilder());
		if (this.refersToClassMap != null) {
			builder.addAliased(this.refersToClassMap.relationBuilder());
		}
		
		Iterator it = this.properties.iterator();
		while (it.hasNext()) 
		{
			Object prop = it.next();
			if(prop instanceof DynamicProperty)
			{
				DynamicProperty dynamicProperty = (DynamicProperty) prop;
				builder.addOther(dynamicProperty.relationBuilder());
			}
		}
		
		if (this.limit!=null) {
			builder.setLimit(this.limit.intValue());
		}
		if (this.limitInverse!=null) {
			builder.setLimitInverse(this.limitInverse.intValue());
		}
		if (this.order!=null) {
			builder.setOrder(SQL.parseAttribute(this.order), this.orderDesc.booleanValue());
		}

		return builder.buildRelation(this.belongsToClassMap.database().connectedDB()); 
	}

	public Collection toTripleRelations() {
		this.validate();
		Collection results = new ArrayList();
		Iterator it = this.properties.iterator();
		while (it.hasNext()) 
		{
			Object prop = it.next();
			NodeMaker s = this.belongsToClassMap.nodeMaker();
			NodeMaker o = nodeMaker();
			if(prop instanceof Property)
			{
				Property property = (Property) prop;
				NodeMaker p = new FixedNodeMaker(property.asNode(), false);
				results.add(new TripleRelation(buildRelation(), s, p, o));
			}
			else if(prop instanceof DynamicProperty)
			{
				DynamicProperty dynamicProperty = (DynamicProperty) prop;
				NodeMaker p = dynamicProperty.nodeMaker();
				results.add(new TripleRelation(buildRelation(), s, p, o));
			}
			
			
		}
		return results;
	}
	
	public String toString() {
		return "d2rq:PropertyBridge " + PrettyPrinter.toString(this.resource);		
	}
}