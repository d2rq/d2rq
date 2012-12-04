package org.d2rq.lang;

import java.util.Collection;
import java.util.HashSet;

import org.d2rq.D2RQException;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.pp.PrettyPrinter;
import org.d2rq.vocab.D2RQ;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


/**
 * Java object corresponding to d2rq:PropertyBridge.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class PropertyBridge extends ResourceMap {
	
	/**
	 * Convenience method for creating property bridges.
	 * 
	 * @param id Identifier for the bridge; may be null
	 * @param property Property URI
	 * @param classMap Class map the bridge belongs to
	 * @return A new property bridge
	 */
	public static PropertyBridge create(Resource id, Resource property, ClassMap classMap) {
		PropertyBridge result = new PropertyBridge(id == null ? ResourceFactory.createResource() : id);
		result.addProperty(property);
		result.setBelongsToClassMap(classMap);
		return result;
	}
	
	private ClassMap belongsToClassMap = null;
	private Collection<Resource> properties = new HashSet<Resource>();
	private Collection<String> dynamicPropertyPatterns = new HashSet<String>();
	private ColumnName column = null;
	private String pattern = null;
	private String sqlExpression = null;
	private String datatype = null;
	private String lang = null;
	private ClassMap refersToClassMap = null;
	private Integer limit = null;
	private Integer limitInverse = null;
	private String order = null;
	private Boolean orderDesc = null;
	
	public PropertyBridge(Resource resource) {
		super(resource, true);
	}
	
	public Database getDatabase() {
		if (belongsToClassMap == null) return null;
		return belongsToClassMap.getDatabase();
	}
	
	public Collection<Resource> getProperties() {
		return this.properties;
	}
	
	public Collection<String> getDynamicPropertyPatterns() {
		return dynamicPropertyPatterns;
	}
	
    public ClassMap getBelongsToClassMap() {
        return belongsToClassMap;
    }

    /**
     * Also adds the property bridge to the class map's list of bridges.
     */
	public void setBelongsToClassMap(ClassMap classMap) {
		assertNotYetDefined(this.belongsToClassMap, D2RQ.belongsToClassMap, D2RQException.PROPERTYBRIDGE_DUPLICATE_BELONGSTOCLASSMAP);
		assertArgumentNotNull(classMap, D2RQ.belongsToClassMap, D2RQException.PROPERTYBRIDGE_INVALID_BELONGSTOCLASSMAP);
		this.belongsToClassMap = classMap;
		classMap.addPropertyBridge(this);
	}
	
    public ColumnName getColumn() {
        return column;
    }

	public void setColumn(String column) {
		assertNotYetDefined(this.column, D2RQ.column, D2RQException.PROPERTYBRIDGE_DUPLICATE_COLUMN);
		this.column = Microsyntax.parseColumn(column);
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
        return limit == null ? LimitOp.NO_LIMIT : limit.intValue();
    }
	
	public void setLimit(int limit) {
	    assertNotYetDefined(this.limit, D2RQ.limit, D2RQException.PROPERTYBRIDGE_DUPLICATE_LIMIT);
	    this.limit = new Integer(limit);
	}

    public int getLimitInverse() {
        return limitInverse == null ? LimitOp.NO_LIMIT : limitInverse.intValue();
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
    
	public String getOrder() {
		return order;
	}
	
	public Boolean getOrderDesc() {
		return orderDesc;
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
		this.properties.add(property);
	}
	
	public void addDynamicProperty(String dynamicPropertyPattern) {
		this.dynamicPropertyPatterns.add(dynamicPropertyPattern);
	}
	
	public void accept(D2RQMappingVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "d2rq:PropertyBridge " + PrettyPrinter.toString(resource());		
	}
}