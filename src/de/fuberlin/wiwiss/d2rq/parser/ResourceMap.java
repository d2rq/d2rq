package de.fuberlin.wiwiss.d2rq.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.map.ClassMap;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker.NodeType;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.values.BlankNodeID;
import de.fuberlin.wiwiss.d2rq.values.Column;
import de.fuberlin.wiwiss.d2rq.values.Pattern;
import de.fuberlin.wiwiss.d2rq.values.ValueDecorator;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ResourceMap.java,v 1.2 2006/09/11 23:02:48 cyganiak Exp $
 */
public abstract class ResourceMap {
	protected static final Property valueProperty = 
		D2RQ.ClassMap.getModel().createProperty(D2RQ.NS + "x-value");
	
	private String mapName;

	// These can be set on PropertyBridges and ClassMaps
	protected String bNodeIdColumns = null;	// comma-separated list
	protected String uriColumn = null;
	protected String uriPattern = null;
	protected Collection valueRegexes = new ArrayList();
	protected Collection valueContainses = new ArrayList();
	protected int valueMaxLength = Integer.MAX_VALUE;
	protected Collection joins = new ArrayList();
	protected Collection conditions = new ArrayList();
	protected Collection aliases = new ArrayList();
	protected boolean containsDuplicates;
	protected TranslationTable translateWith = null;

	// These can be set only on a PropertyBridge
	protected String column = null;
	protected String pattern = null;
	protected String datatype = null;
	protected String lang = null;
	protected RDFNode value = null;
	protected ClassMap refersToClassMap = null;

	private NodeMaker cachedNodeMaker;
	private Relation cachedRelation;
	
	public ResourceMap(String mapName, boolean defaultContainsDuplicate) {
		this.mapName = mapName;
		this.containsDuplicates = defaultContainsDuplicate;
	}
	
	public void setBNodeIdColumns(String columns) {
		assertNotYetDefined(this.bNodeIdColumns, D2RQ.bNodeIdColumns, D2RQException.RESOURCEMAP_DUPLICATE_BNODEIDCOLUMNS);
		this.bNodeIdColumns = columns;
	}

	public void setURIColumn(String column) {
		assertNotYetDefined(this.uriColumn, D2RQ.uriColumn, D2RQException.RESOURCEMAP_DUPLICATE_URICOLUMN);
		this.uriColumn = column;
	}

	public void setURIPattern(String pattern) {
		assertNotYetDefined(this.uriColumn, D2RQ.uriPattern, D2RQException.RESOURCEMAP_DUPLICATE_URIPATTERN);
		this.uriPattern = pattern;
	}
	
	public void addValueRegex(String regex) {
		this.valueRegexes.add(regex);
	}
	
	public void addValueContains(String contains) {
		this.valueContainses.add(contains);
	}
	
	public void setValueMaxLength(int maxLength) {
		if (this.valueMaxLength != Integer.MAX_VALUE) {
			// always fails
			assertNotYetDefined(this, D2RQ.valueMaxLength, 
					D2RQException.PROPERTYBRIDGE_DUPLICATE_VALUEMAXLENGTH);
		}
		this.valueMaxLength = maxLength;
	}

	public void setTranslateWith(TranslationTable table) {
		assertNotYetDefined(this.translateWith, D2RQ.translateWith, 
				D2RQException.RESOURCEMAP_DUPLICATE_TRANSLATEWITH);
		assertArgumentNotNull(table, D2RQ.translateWith, D2RQException.RESOURCEMAP_INVALID_TRANSLATEWITH);
		this.translateWith = table;
	}
	
	public void addJoin(String join) {
		this.joins.add(join);
	}
	
	public void addCondition(String condition) {
		this.conditions.add(condition);
	}
	
	public void addAlias(String alias) {
		this.aliases.add(alias);
	}
	
	public void setContainsDuplicates(boolean b) {
		this.containsDuplicates = b;
	}
	
	public RelationBuilder relationBuilder() {
		RelationBuilder result = new RelationBuilder();
		Iterator it = this.joins.iterator();
		while (it.hasNext()) {
			String join = (String) it.next();
			result.addJoinCondition(join);
		}
		it = this.conditions.iterator();
		while (it.hasNext()) {
			String condition = (String) it.next();
			result.addCondition(condition);
		}
		it = this.aliases.iterator();
		while (it.hasNext()) {
			String alias = (String) it.next();
			result.addAlias(alias);
		}
		return result;
	}
	
	public Relation relation() {
		if (this.cachedRelation == null) {
			this.cachedRelation = buildRelation();
		}
		return this.cachedRelation;
	}

	protected abstract Relation buildRelation();
	
	public NodeMaker nodeMaker() {
		if (this.cachedNodeMaker == null) {
			this.cachedNodeMaker = buildNodeMaker();
		}
		return this.cachedNodeMaker;
	}
	
	private NodeMaker buildNodeMaker() {
		if (this.value != null) {
			return new FixedNodeMaker(this.value.asNode(), 
					!this.containsDuplicates);
		}
		if (this.refersToClassMap == null) {
			return buildNodeMaker(wrapValueSource(buildValueSourceBase()), !this.containsDuplicates);
		}
		return this.refersToClassMap.buildNodeMakerForReferringPropertyBridge(this, !this.containsDuplicates);
	}

	public NodeMaker buildNodeMakerForReferringPropertyBridge(ResourceMap other, boolean unique) {
		ValueMaker values = other.wrapValueSource(
				wrapValueSource(buildValueSourceBase())).replaceColumns(
						other.relationBuilder().aliases());
		return buildNodeMaker(values, unique); 
	}
	
	private ValueMaker buildValueSourceBase() {
		if (this.bNodeIdColumns != null) {
			return new BlankNodeID(this.bNodeIdColumns, this.mapName);
		}
		if (this.uriColumn != null) {
			return new Column(this.uriColumn);
		}
		if (this.uriPattern != null) {
			return new Pattern(this.uriPattern);
		}
		if (this.column != null) {
			return new Column(this.column);
		}
		if (this.pattern != null) {
			return new Pattern(this.pattern);
		}
		throw new D2RQException(this.mapName + " needs a column/pattern/bNodeID specification");
	}

	public ValueMaker wrapValueSource(ValueMaker values) {
		List constraints = new ArrayList();
		if (this.valueMaxLength != Integer.MAX_VALUE) {
			constraints.add(ValueDecorator.maxLengthConstraint(this.valueMaxLength));
		}
		Iterator it = this.valueContainses.iterator();
		while (it.hasNext()) {
			String contains = (String) it.next();
			constraints.add(ValueDecorator.containsConstraint(contains));
		}
		it = this.valueRegexes.iterator();
		while (it.hasNext()) {
			String regex = (String) it.next();
			constraints.add(ValueDecorator.regexConstraint(regex));
		}
		if (this.translateWith == null) {
			if (constraints.isEmpty()) {
				return values;
			}
			return new ValueDecorator(values, constraints);
		}
		return new ValueDecorator(values, constraints, this.translateWith.translator());
	}
	
	private NodeMaker buildNodeMaker(ValueMaker values, boolean isUnique) {
		return new TypedNodeMaker(nodeType(), values, isUnique);
	}
	
	private NodeType nodeType() {
		if (this.bNodeIdColumns != null) {
			return TypedNodeMaker.BLANK;
		}
		if (this.uriColumn != null || this.uriPattern != null) {
			return TypedNodeMaker.URI;
		}
		if (this.column == null && this.pattern == null) {
			throw new D2RQException(this.mapName + " needs a column/pattern/bNodeID specification");
		}
		if (this.datatype != null && this.lang != null) {
			throw new D2RQException(this.mapName + " has both d2rq:lang and d2rq:datatype");
		}
		if (this.datatype != null) {
			return TypedNodeMaker.typedLiteral(buildDatatype(this.datatype));
		}
		if (this.lang != null) {
			return TypedNodeMaker.languageLiteral(this.lang);
		}
		return TypedNodeMaker.PLAIN_LITERAL;
	}
	
	private RDFDatatype buildDatatype(String datatypeURI) {
		return TypeMapper.getInstance().getSafeTypeByName(datatypeURI);		
	}

	protected void assertHasPrimarySpec(Property[] allowedSpecs) {
		List definedSpecs = new ArrayList();
		Iterator it = Arrays.asList(allowedSpecs).iterator();
		while (it.hasNext()) {
			Property allowedProperty = (Property) it.next();
			if (hasPrimarySpec(allowedProperty)) {
				definedSpecs.add(allowedProperty);
			}
		}
		if (definedSpecs.isEmpty()) {
			StringBuffer error = new StringBuffer(toString());
			error.append(" needs one of ");
			for (int i = 0; i < allowedSpecs.length; i++) {
				if (i > 0) {
					error.append(", ");
				}
				error.append(PrettyPrinter.toString(allowedSpecs[i]));
			}
			throw new D2RQException(error.toString(), D2RQException.RESOURCEMAP_MISSING_PRIMARYSPEC);
		}
		if (definedSpecs.size() > 1) {
			throw new D2RQException(toString() + " can't have both " +
					PrettyPrinter.toString((Property) definedSpecs.get(0)) +
					" and " +
					PrettyPrinter.toString((Property) definedSpecs.get(1)));
		}
	}
	
	private boolean hasPrimarySpec(Property property) {
		if (property.equals(D2RQ.bNodeIdColumns)) return this.bNodeIdColumns != null;
		if (property.equals(D2RQ.uriColumn)) return this.uriColumn != null;
		if (property.equals(D2RQ.uriPattern)) return this.uriPattern != null;
		if (property.equals(D2RQ.column)) return this.column != null;
		if (property.equals(D2RQ.pattern)) return this.pattern != null;
		if (property.equals(D2RQ.refersToClassMap)) return this.refersToClassMap != null;
		if (property.equals(valueProperty)) return this.value != null;
		throw new D2RQException("No primary spec: " + property);
	}

	protected void assertNotYetDefined(Object object, Property property, int errorCode) {
		if (object == null) {
			return;
		}
		throw new D2RQException("Duplicate " + PrettyPrinter.toString(property) + 
				" for " + this, errorCode);
	}
	
	protected void assertHasBeenDefined(Object object, Property property, int errorCode) {
		if (object != null) {
			return;
		}
		throw new D2RQException("Missing " + PrettyPrinter.toString(property) + 
				" for " + this, errorCode);
	}
	
	protected void assertArgumentNotNull(Object object, Property property, int errorCode) {
		if (object != null) {
			return;
		}
		throw new D2RQException("Object for " + PrettyPrinter.toString(property) + 
				" not found at " + this, errorCode);
	}
	
	public void validate() {
		// Nothing to flag here yet ... subclasses do it
	}	
}