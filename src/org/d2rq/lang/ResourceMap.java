package org.d2rq.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.d2rq.D2RQException;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.vocab.D2RQ;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * Getters and setters for everything that's common to class maps,
 * property bridges and download maps
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class ResourceMap extends MapObject {
	private List<ColumnName> bNodeIdColumns = null;
	private ColumnName uriColumn = null;
	private String uriPattern = null;
	private String uriSqlExpression = null;
	private RDFNode constantValue = null;
	private Collection<String> valueRegexes = new ArrayList<String>();
	private Collection<String> valueContainses = new ArrayList<String>();
	private int valueMaxLength = Integer.MAX_VALUE;
	private Collection<String> joins = new ArrayList<String>();
	private Collection<String> conditions = new ArrayList<String>();
	private Set<AliasDeclaration> aliases = new HashSet<AliasDeclaration>();
	private boolean containsDuplicates;
	private TranslationTable translateWith = null;
	private Collection<Literal> definitionLabels = new ArrayList<Literal>();
	private Collection<Literal> definitionComments = new ArrayList<Literal>();
	private Collection<Resource> additionalDefinitionProperties = new ArrayList<Resource>();	

	public ResourceMap(Resource resource, boolean defaultContainsDuplicate) {
		super(resource);
		this.containsDuplicates = defaultContainsDuplicate;
	}
	
	public abstract Database getDatabase();
		
	public void setBNodeIdColumns(String columns) {
		assertNotYetDefined(this.bNodeIdColumns, D2RQ.bNodeIdColumns, D2RQException.RESOURCEMAP_DUPLICATE_BNODEIDCOLUMNS);
		bNodeIdColumns = Microsyntax.parseColumnList(columns);
	}

	public List<ColumnName> getBNodeIdColumnsParsed() {
		return bNodeIdColumns == null ? Collections.<ColumnName>emptyList() : bNodeIdColumns;
	}
	
	public String getBNodeIdColumns() {
		return Microsyntax.toString(bNodeIdColumns);
	}
	
	public void setURIColumn(String column) {
		assertNotYetDefined(this.uriColumn, D2RQ.uriColumn, D2RQException.RESOURCEMAP_DUPLICATE_URICOLUMN);
		uriColumn = Microsyntax.parseColumn(column);
	}

	public ColumnName getURIColumn() {
		return uriColumn;
	}
	
	public void setURIPattern(String pattern) {
		assertNotYetDefined(this.uriPattern, D2RQ.uriPattern, D2RQException.RESOURCEMAP_DUPLICATE_URIPATTERN);
		this.uriPattern = pattern;
	}
	
	public String getURIPattern() {
		return uriPattern;
	}
	
	public void setUriSQLExpression(String uriSqlExpression) {
		assertNotYetDefined(this.uriSqlExpression, D2RQ.uriSqlExpression, D2RQException.PROPERTYBRIDGE_DUPLICATE_URI_SQL_EXPRESSION);
		this.uriSqlExpression = uriSqlExpression;
	}
	
    public String getUriSQLExpression() {
        return uriSqlExpression;
    }
	
	public void setConstantValue(RDFNode constantValue) {
		assertNotYetDefined(this.constantValue, D2RQ.constantValue, D2RQException.RESOURCEMAP_DUPLICATE_CONSTANTVALUE);
		this.constantValue = constantValue;
	}
	
	public RDFNode getConstantValue() {
		return constantValue;
	}
	
	public void addValueRegex(String regex) {
		this.valueRegexes.add(regex);
	}
	
	public Collection<String> getValueRegexes() {
		return valueRegexes;
	}
	
	public void addValueContains(String contains) {
		this.valueContainses.add(contains);
	}
	
	public Collection<String> getValueContainses() {
		return valueContainses;
	}
	
	public void setValueMaxLength(int maxLength) {
		if (this.valueMaxLength != Integer.MAX_VALUE) {
			// always fails
			assertNotYetDefined(this, D2RQ.valueMaxLength, 
					D2RQException.PROPERTYBRIDGE_DUPLICATE_VALUEMAXLENGTH);
		}
		this.valueMaxLength = maxLength;
	}

	public int getValueMaxLength() {
		return valueMaxLength;
	}
	
	public void setTranslateWith(TranslationTable table) {
		assertNotYetDefined(this.translateWith, D2RQ.translateWith, 
				D2RQException.RESOURCEMAP_DUPLICATE_TRANSLATEWITH);
		assertArgumentNotNull(table, D2RQ.translateWith, D2RQException.RESOURCEMAP_INVALID_TRANSLATEWITH);
		this.translateWith = table;
	}
	
	public TranslationTable getTranslateWith() {
		return translateWith;
	}
	
	public void addJoin(String join) {
		joins.add(join);
	}
	
	public Collection<String> getJoins() {
		return joins;
	}
	
	public void addCondition(String condition) {
		this.conditions.add(condition);
	}
	
	public Collection<String> getConditions() {
		return conditions;
	}
	
	public void addAlias(String alias) {
		aliases.add(Microsyntax.parseAlias(alias));
	}
	
	public Set<AliasDeclaration> getAliasesParsed() {
		return aliases;
	}

	public Set<String> getAliases() {
		Set<String> result = new HashSet<String>();
		for (AliasDeclaration alias: aliases) {
			result.add(Microsyntax.toString(alias));
		}
		return result;
	}
	
	public void setContainsDuplicates(boolean b) {
		this.containsDuplicates = b;
	}

	public boolean getContainsDuplicates() {
		return containsDuplicates;
	}
	
	public Collection<Literal> getDefinitionLabels() {
		return definitionLabels;
	}
	
	public Collection<Literal> getDefinitionComments() {
		return definitionComments;
	}

	public Collection<Resource> getAdditionalDefinitionProperties() {
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