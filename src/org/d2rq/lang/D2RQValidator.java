package org.d2rq.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.d2rq.D2RQException;
import org.d2rq.pp.PrettyPrinter;
import org.d2rq.validation.Message;
import org.d2rq.validation.Message.Problem;
import org.d2rq.validation.Report;
import org.d2rq.values.TemplateValueMaker;
import org.d2rq.vocab.D2RQ;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * TODO: Add database validation (Connection details valid? All columns exist and have useable types? All expressions and conditions syntactically valid?). Some of that is currently done in the compiler. Make consistent!    
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class D2RQValidator implements D2RQMappingVisitor {
	private static Log log = LogFactory.getLog(D2RQValidator.class);
	
	private final Mapping mapping;
	private final Collection<Resource> classMapsWithoutProperties = new ArrayList<Resource>();
	private Report report = new Report();

	public D2RQValidator(Mapping mapping) {
		this.mapping = mapping;
		report.setThrowExceptionOnError(true);
		report.setWarningLogger(log);
	}
	
	public void setReport(Report report) {
		this.report = report;
	}
	
	public Report getReport() {
		return report;
	}
	
	public void run() {
		mapping.accept(this);
	}
	
	public boolean visitEnter(Mapping mapping) {
		if (mapping.databases().isEmpty()) {
			error("No d2rq:Database defined in the mapping", 
					D2RQException.MAPPING_NO_DATABASE);
		}
		classMapsWithoutProperties.addAll(mapping.classMapResources());
		return true;
	}

	public void visitLeave(Mapping mapping) {
		if (!classMapsWithoutProperties.isEmpty()) {
			error("d2rq:ClassMap " + 
					classMapsWithoutProperties.iterator().next() + 
					" has no associated d2rq:PropertyBridges and no d2rq:class",
					D2RQException.CLASSMAP_NO_PROPERTYBRIDGES);
		}
	}

	public void visit(Configuration configuration) {
		/* All settings are optional */
	}

	public void visit(Database database) {
		// TODO
	}

	public boolean visitEnter(ClassMap classMap) {
		visitResourceMap(classMap);
		if (!classMap.getClasses().isEmpty()) {
			classMapsWithoutProperties.remove(classMap.resource());
		}
		assertNotNull(classMap.getDatabase(), D2RQ.dataStorage, D2RQException.CLASSMAP_NO_DATABASE);
		assertHasPrimarySpec(classMap, new Property[]{
				D2RQ.uriColumn, D2RQ.uriPattern, D2RQ.uriSqlExpression, D2RQ.bNodeIdColumns, D2RQ.constantValue
		});
		if (classMap.getConstantValue() != null && !classMap.getConstantValue().isURIResource()) {
			error("d2rq:constantValue for class map " + classMap + " must be a URI", 
					D2RQException.RESOURCEMAP_INVALID_CONSTANTVALUE);
		}
		// TODO
		return true;
	}

	public void visitLeave(ClassMap classMap) {
		// Do nothing
	}

	public void visit(PropertyBridge propertyBridge) {
		visitResourceMap(propertyBridge);
		classMapsWithoutProperties.remove(propertyBridge.getBelongsToClassMap().resource());
		if (propertyBridge.getRefersToClassMap() != null) {
			classMapsWithoutProperties.remove(propertyBridge.getRefersToClassMap().resource());
		}
		if (propertyBridge.getRefersToClassMap() != null &&
				!propertyBridge.getRefersToClassMap().getDatabase().equals(propertyBridge.getBelongsToClassMap().getDatabase())) {
			error(propertyBridge + 
					" links two d2rq:ClassMaps with different d2rq:dataStorages",
					D2RQException.PROPERTYBRIDGE_CONFLICTING_DATABASES);
			// TODO refersToClassMap cannot be combined w/ value constraints or translation tables
		}
		if (propertyBridge.getProperties().isEmpty() && 
				propertyBridge.getDynamicPropertyPatterns().isEmpty()) {
			error(propertyBridge + 
					" needs a d2rq:property or d2rq:dynamicProperty",
					D2RQException.PROPERTYBRIDGE_MISSING_PREDICATESPEC);
		}
		if (propertyBridge.getConstantValue() != null && propertyBridge.getConstantValue().isAnon()) {
			error("d2rq:constantValue for property bridge " + propertyBridge + " must be a URI or literal", 
					D2RQException.RESOURCEMAP_INVALID_CONSTANTVALUE);
		}
		assertHasPrimarySpec(propertyBridge, new Property[]{
				D2RQ.uriColumn, D2RQ.uriPattern, D2RQ.bNodeIdColumns,
				D2RQ.column, D2RQ.pattern, D2RQ.sqlExpression, D2RQ.uriSqlExpression,
				D2RQ.constantValue, D2RQ.refersToClassMap
		});
		if (propertyBridge.getDatatype() != null && propertyBridge.getLang() != null) {
			error(propertyBridge + 
					" has both d2rq:datatype and d2rq:lang",
					D2RQException.PROPERTYBRIDGE_LANG_AND_DATATYPE);
		}
		if (propertyBridge.getColumn() == null && propertyBridge.getPattern() == null && 
				propertyBridge.getSQLExpression() == null) {
			if (propertyBridge.getDatatype() != null) {
				error("d2rq:datatype can only be used with d2rq:column, d2rq:pattern " +
						"or d2rq:sqlExpression at " + propertyBridge,
						D2RQException.PROPERTYBRIDGE_NONLITERAL_WITH_DATATYPE);
			}
			if (propertyBridge.getLang() != null) {
				error("d2rq:lang can only be used with d2rq:column, d2rq:pattern " +
						"or d2rq:sqlExpression at " + propertyBridge,
						D2RQException.PROPERTYBRIDGE_NONLITERAL_WITH_LANG);
			}
		}
	}
	
	public void visit(TranslationTable translationTable) {
		if (!translationTable.getTranslations().isEmpty() && 
				translationTable.getJavaClass() != null) {
			error("Can't combine d2rq:translation and d2rq:javaClass on " + translationTable,
					D2RQException.TRANSLATIONTABLE_TRANSLATION_AND_JAVACLASS);
		}
		if (!translationTable.getTranslations().isEmpty() && 
				translationTable.getHref() != null) {
			error("Can't combine d2rq:translation and d2rq:href on " + translationTable,
					D2RQException.TRANSLATIONTABLE_TRANSLATION_AND_HREF);
		}
		if (translationTable.getHref() != null && 
				translationTable.getJavaClass() != null) {
			error("Can't combine d2rq:href and d2rq:javaClass on " + translationTable,
					D2RQException.TRANSLATIONTABLE_HREF_AND_JAVACLASS);
		}
	}

	public void visit(DownloadMap downloadMap) {
		visitResourceMap(downloadMap);
		assertHasPrimarySpec(downloadMap, new Property[]{
				D2RQ.uriColumn, D2RQ.uriPattern, D2RQ.uriSqlExpression, D2RQ.constantValue
		});
		if (downloadMap.getDatabase() == null && downloadMap.getBelongsToClassMap() == null) {
			error("Download map " + downloadMap + " needs a d2rq:dataStorage (or d2rq:belongsToClassMap)", 
					D2RQException.DOWNLOADMAP_NO_DATASTORAGE);
		}
		assertNotNull(downloadMap.getContentDownloadColumn(), D2RQ.contentDownloadColumn, 
				D2RQException.DOWNLOADMAP_NO_CONTENTCOLUMN);
		if (downloadMap.getConstantValue() != null && !downloadMap.getConstantValue().isURIResource()) {
			error("d2rq:constantValue for download map " + downloadMap + " must be a URI", 
					D2RQException.RESOURCEMAP_INVALID_CONSTANTVALUE);
		}
		// Validate media type?
	}
	
	public void visitResourceMap(ResourceMap resourceMap) {
		if (resourceMap.getURIPattern() != null) {
			TemplateValueMaker parsed = Microsyntax.parsePattern(resourceMap.getURIPattern());
			if (parsed.columns().length == 0) {
				warn(resourceMap + " has a d2rq:uriPattern without any column specifications (" +
				resourceMap.getURIPattern() + "). " +
				"This usually happens when no primary keys are defined for a table. " +
				"If the configuration is left as is, all table rows will be mapped to a single instance. " +
				"If this is not what you want, please define the keys in the database " + 
				"and re-run the mapping generator, or edit the mapping to provide the relevant keys.");
			}
			for (String s: parsed.literalParts()) {
				if (!s.matches(D2RQReader.IRI_CHAR_REGEX)) {
					error("d2rq:uriPattern '"
							+ resourceMap.getURIPattern() + "' contains characters not allowed in URIs",
							D2RQException.RESOURCEMAP_ILLEGAL_URIPATTERN);
				}
			}
		}
	}

	protected void assertNotNull(Object object, Property property, int errorCode) {
		if (object != null) return;
		error("Missing " + PrettyPrinter.toString(property) + 
				" for " + this, errorCode);
	}
	
	protected void assertHasPrimarySpec(ResourceMap resourceMap, Property[] allowedSpecs) {
		List<Property> definedSpecs = new ArrayList<Property>();
		for (Property allowedProperty: Arrays.asList(allowedSpecs)) {
			if (hasPrimarySpec(resourceMap, allowedProperty)) {
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
			error(error.toString(), D2RQException.RESOURCEMAP_MISSING_PRIMARYSPEC);
		}
		if (definedSpecs.size() > 1) {
			error(resourceMap + " can't have both " +
					PrettyPrinter.toString((Property) definedSpecs.get(0)) +
					" and " +
					PrettyPrinter.toString((Property) definedSpecs.get(1)),
					D2RQException.RESOURCEMAP_DUPLICATE_PRIMARYSPEC);
		}
	}
	
	private boolean hasPrimarySpec(ResourceMap map, Property property) {
		if (property.equals(D2RQ.bNodeIdColumns)) return !map.getBNodeIdColumnsParsed().isEmpty();
		if (property.equals(D2RQ.uriColumn)) return map.getURIColumn() != null;
		if (property.equals(D2RQ.uriPattern)) return map.getURIPattern() != null;
		if (property.equals(D2RQ.uriSqlExpression)) return map.getUriSQLExpression() != null;
		if (property.equals(D2RQ.column)) return ((PropertyBridge) map).getColumn() != null;
		if (property.equals(D2RQ.pattern)) return ((PropertyBridge) map).getPattern() != null;
		if (property.equals(D2RQ.sqlExpression)) return ((PropertyBridge) map).getSQLExpression() != null;
		if (property.equals(D2RQ.refersToClassMap)) return ((PropertyBridge) map).getRefersToClassMap() != null;
		if (property.equals(D2RQ.constantValue)) return map.getConstantValue() != null;
		throw new IllegalArgumentException("Not a primary spec: " + property);
	}
	
	private void warn(String message) {
		report.report(new Message(Problem.GENERIC_WARNING, message));
	}
	
	private void error(String message, int code) {
		report.report(new Message(Problem.GENERIC_ERROR, message, code));
	}
}
