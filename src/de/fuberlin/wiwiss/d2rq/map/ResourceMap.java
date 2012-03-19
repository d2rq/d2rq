package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker.NodeType;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.parser.RelationBuilder;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.sql.SQL;
import de.fuberlin.wiwiss.d2rq.values.BlankNodeID;
import de.fuberlin.wiwiss.d2rq.values.Column;
import de.fuberlin.wiwiss.d2rq.values.Pattern;
import de.fuberlin.wiwiss.d2rq.values.SQLExpressionValueMaker;
import de.fuberlin.wiwiss.d2rq.values.ValueDecorator;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class ResourceMap extends MapObject {

	// These can be set on PropertyBridges and ClassMaps
	protected String bNodeIdColumns = null;	// comma-separated list
	protected String uriColumn = null;
	protected String uriPattern = null;
	protected RDFNode constantValue = null;
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
	protected String sqlExpression = null;
	protected String uriSqlExpression = null;
	protected String datatype = null;
	protected String lang = null;
	protected ClassMap refersToClassMap = null;
	protected Integer limit = null;
	protected Integer limitInverse = null;
	protected String order = null;
	protected Boolean orderDesc = null;

	private NodeMaker cachedNodeMaker;
	private Relation cachedRelation;
	
	Collection definitionLabels = new ArrayList();
	Collection definitionComments = new ArrayList();

	/**
	 * List of D2RQ.AdditionalProperty
	 */
	Collection additionalDefinitionProperties = new ArrayList();	

	public ResourceMap(Resource resource, boolean defaultContainsDuplicate) {
		super(resource);
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
	
	public void setConstantValue(RDFNode constantValue) {
		assertNotYetDefined(this.constantValue, D2RQ.constantValue, D2RQException.RESOURCEMAP_DUPLICATE_CONSTANTVALUE);
		this.constantValue = constantValue;
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

	private Collection aliases() {
		Set parsedAliases = new HashSet();
		Iterator it = aliases.iterator();
		while (it.hasNext()) {
			String alias = (String) it.next();
			parsedAliases.add(SQL.parseAlias(alias));
		}
		return parsedAliases;
	}
	
	public RelationBuilder relationBuilder() {
		RelationBuilder result = new RelationBuilder();
		Iterator it = SQL.parseJoins(this.joins).iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			result.addJoinCondition(join);
		}
		it = this.conditions.iterator();
		while (it.hasNext()) {
			String condition = (String) it.next();
			result.addCondition(condition);
		}
		result.addAliases(aliases());
		it = nodeMaker().projectionSpecs().iterator();
		while (it.hasNext()) {
			ProjectionSpec projection = (ProjectionSpec) it.next();
			result.addProjection(projection);
		}
		if (!containsDuplicates) {
			result.setIsUnique(true);
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
		if (this.constantValue != null) {
			return new FixedNodeMaker(this.constantValue.asNode(), 
					!this.containsDuplicates);
		}
		if (this.refersToClassMap == null) {
			return buildNodeMaker(wrapValueSource(buildValueSourceBase()), !this.containsDuplicates);
		}
		return this.refersToClassMap.buildAliasedNodeMaker(new AliasMap(aliases()), !this.containsDuplicates);
	}

	public NodeMaker buildAliasedNodeMaker(AliasMap aliases, boolean unique) {
		ValueMaker values = wrapValueSource(buildValueSourceBase()).renameAttributes(aliases);
		return buildNodeMaker(values, unique);
	}
	
	private ValueMaker buildValueSourceBase() {
		if (this.bNodeIdColumns != null) {
			return new BlankNodeID(PrettyPrinter.toString(this.resource()),
					parseColumnList(this.bNodeIdColumns));
		}
		if (this.uriColumn != null) {
			return new Column(SQL.parseAttribute(this.uriColumn));
		}
		if (this.uriPattern != null) {
			Pattern p = new Pattern(this.uriPattern);
			if (!p.literalPartsMatchRegex(MapParser.URI_CHAR_REGEX)) {
				throw new D2RQException("d2rq:uriPattern '"
						+ this.uriPattern + "' contains characters not allowed in URIs",
						D2RQException.RESOURCEMAP_ILLEGAL_URIPATTERN);
			}
			return p;
		}
		if (this.column != null) {
			return new Column(SQL.parseAttribute(this.column));
		}
		if (this.pattern != null) {
			return new Pattern(this.pattern);
		}
		if (this.sqlExpression != null) {
			return new SQLExpressionValueMaker(SQLExpression.create(sqlExpression));
		}
		if (this.uriSqlExpression != null) {
			return new SQLExpressionValueMaker(SQLExpression.create(uriSqlExpression));
		}
		throw new D2RQException(this + " needs a column/pattern/bNodeID specification");
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
		if (this.uriSqlExpression != null) {
			return TypedNodeMaker.URI;
		}
		
		// literals
		if (this.column == null && this.pattern == null && this.sqlExpression == null) {
			throw new D2RQException(this + " needs a column/pattern/bNodeID/sqlExpression/uriSqlExpression specification");
		}
		if (this.datatype != null && this.lang != null) {
			throw new D2RQException(this + " has both d2rq:lang and d2rq:datatype");
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

	private List parseColumnList(String commaSeperated) {
		List result = new ArrayList();
		Iterator it = Arrays.asList(commaSeperated.split(",")).iterator();
		while (it.hasNext()) {
			result.add(SQL.parseAttribute((String) it.next()));
		}
		return result;
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
		if (property.equals(D2RQ.sqlExpression)) return this.sqlExpression != null;
		if (property.equals(D2RQ.uriSqlExpression)) return this.uriSqlExpression != null;
		if (property.equals(D2RQ.refersToClassMap)) return this.refersToClassMap != null;
		if (property.equals(D2RQ.constantValue)) return this.constantValue != null;
		throw new D2RQException("No primary spec: " + property);
	}
	
	public Collection getDefinitionLabels() {
		return definitionLabels;
	}
	
	public Collection getDefinitionComments() {
		return definitionComments;
	}

	public Collection getAdditionalDefinitionProperties() {
		return additionalDefinitionProperties;
	}

	public void addDefinitionLabel(Literal definitionLabel) {
		definitionLabels.add(definitionLabel);
	}

	public void addDefinitionComment(Literal definitionComment) {
		definitionComments.add(definitionComment);
	}

	public void addDefinitionProperty(Resource additionalProperty) {
		additionalDefinitionProperties.add(additionalProperty);
	}		
}