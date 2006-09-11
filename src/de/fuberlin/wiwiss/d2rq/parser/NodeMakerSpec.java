package de.fuberlin.wiwiss.d2rq.parser;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.map.BlankNodeIdentifier;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.ContainsRestriction;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.MaxLengthRestriction;
import de.fuberlin.wiwiss.d2rq.map.Pattern;
import de.fuberlin.wiwiss.d2rq.map.RegexRestriction;
import de.fuberlin.wiwiss.d2rq.map.RenamingValueSource;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.map.ValueSource;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.TypedNodeMaker.NodeType;
import de.fuberlin.wiwiss.d2rq.types.DateTimeTranslator;

/**
 * Builds a {@link NodeMaker} after a specification provided
 * through calls to the setter methods.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeMakerSpec.java,v 1.8 2006/09/11 06:21:17 cyganiak Exp $
 */
public class NodeMakerSpec {
	
	public static NodeMakerSpec createFixed(String mapName, Node fixedResource) {
		NodeMakerSpec result = new NodeMakerSpec(mapName);
		result.setFixedNode(fixedResource);
		return result;
	}
	
	public static NodeMakerSpec createFixed(Node fixedResource) {
		return NodeMakerSpec.createFixed("Fixed(" + fixedResource + ")", fixedResource);
	}
	
	private String mapName;
	private NodeMaker producedNodeMaker;
	private Relation producedRelation;
	private Node fixed = null;
	private NodeMakerSpec refersToClassMap = null;
	private String blankColumns = null;	// comma-separated list
	private String uriColumn = null;
	private String uriPattern = null;
	private String literalColumn = null;
	private String literalPattern = null;
	private String datatypeURI = null;
	private String lang = null;
	private String regexHint = null;
	private String containsHint = null;
	private int maxLengthHint = Integer.MAX_VALUE;
	private TranslationTable translationTable = null;
	private RelationBuilder relationBuilder;
	
	private boolean isUnique = false;
	private Database database = null;
	
	public NodeMakerSpec(String mapName) {
		this.mapName = mapName;
		this.relationBuilder = new RelationBuilder();
	}
	
	public NodeMakerSpec(String mapName, NodeMakerSpec belongsToClassMap) {
		this.mapName = mapName;
		this.setDatabase(belongsToClassMap.database());
		this.relationBuilder = new RelationBuilder(belongsToClassMap.relationBuilder());
	}
	
	public void setDatabase(Database db) {
		if (db == null) {
			throw new D2RQException("Unknown d2rq:dataStorage for d2rq:ClassMap " + this.mapName);
		}
		if (this.database != null) {
			throw new D2RQException("Multiple d2rq:dataStorages for d2rq:ClassMap " + this.mapName);
		}
		this.database = db;
	}
	
	public void setRefersToClassMap(NodeMakerSpec otherSpec) {
		this.refersToClassMap = otherSpec;
		if (database != null && !database.equals(otherSpec.database())) {
			throw new D2RQException("d2rq:dataStorages for " + this.mapName + " don't match");
		}
	}
	
	public void setFixedNode(Node node) {
		assertNoPrimarySpec();
		this.fixed = node;
	}

	public void setBlankColumns(String columns) {
		assertNoPrimarySpec();
		this.blankColumns = columns;
	}

	public void setURIColumn(String column) {
		assertNoPrimarySpec();
		this.uriColumn = column;
	}

	public void setURIPattern(String pattern) {
		assertNoPrimarySpec();
		this.uriPattern = pattern;
	}

	public void setLiteralColumn(String column) {
		assertNoPrimarySpec();
		this.literalColumn = column;
	}

	public void setLiteralPattern(String pattern) {
		assertNoPrimarySpec();
		this.literalPattern = pattern;
	}

	public void setDatatypeURI(String datatypeURI) {
		this.datatypeURI = datatypeURI;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public void setRegexHint(String regex) {
		this.regexHint = regex;
	}
	
	public void setContainsHint(String contains) {
		this.containsHint = contains;
	}
	
	public void setMaxLengthHint(int maxLength) {
		this.maxLengthHint = maxLength;
	}
	
	public void setTranslationTable(TranslationTable table) {
		this.translationTable = table;
	}
	
	public void addJoin(String join) {
		this.relationBuilder.addJoinCondition(join);
	}
	
	public void addCondition(String condition) {
		this.relationBuilder.addCondition(condition);
	}
	
	public void addAlias(String alias) {
		this.relationBuilder.addAlias(alias);
	}
	
	public void setIsUnique() {
		this.isUnique = true;
	}
	
	public boolean isURIColumnSpec() {
		return this.uriColumn != null;
	}

	public boolean isURIPatternSpec() {
		return this.uriPattern != null;
	}

	public Database database() {
		return this.database;
	}
	
	public RelationBuilder relationBuilder() {
		return this.relationBuilder;
	}
	
	public Relation buildRelation() {
		if (this.producedRelation == null) {
			this.producedRelation = buildRelationWithoutCaching();
		}
		return this.producedRelation;
	}

	private Relation buildRelationWithoutCaching() {
		if (this.refersToClassMap != null) {
			this.relationBuilder.addOther(this.refersToClassMap.relationBuilder());
		}
		if (this.database == null) {
			throw new D2RQException("No d2rq:dataStorage for " + this.mapName);
		}
		return this.relationBuilder.buildRelation(this.database); 
	}
	
	public NodeMaker buildNodeMaker() {
		if (this.producedNodeMaker == null) {
			this.producedNodeMaker = buildNodeMakerWithoutCaching();
		}
		return this.producedNodeMaker;
	}
	
	private NodeMaker buildNodeMakerWithoutCaching() {
		if (this.fixed != null) {
			return new FixedNodeMaker(this.fixed, false);
		}
		if (this.refersToClassMap == null) {
			return buildNodeMaker(wrapValueSource(buildValueSourceBase()), this.isUnique);
		}
		return this.refersToClassMap.buildNodeMakerForReferringPropertyBridge(this, this.isUnique);
	}

	public NodeMaker buildNodeMakerForReferringPropertyBridge(NodeMakerSpec other, boolean unique) {
		ValueSource values = new RenamingValueSource(
				other.wrapValueSource(wrapValueSource(buildValueSourceBase())),
				other.relationBuilder().aliases());
		return buildNodeMaker(values, unique); 
	}
	
	private ValueSource buildValueSourceBase() {
		if (this.blankColumns != null) {
			return new BlankNodeIdentifier(this.blankColumns, this.mapName);
		}
		if (this.uriColumn != null) {
			return new Column(this.uriColumn);
		}
		if (this.uriPattern != null) {
			return new Pattern(this.uriPattern);
		}
		if (this.literalColumn != null) {
			return new Column(this.literalColumn);
		}
		if (this.literalPattern != null) {
			return new Pattern(this.literalPattern);
		}
		throw new D2RQException(this.mapName + " needs a column/pattern/bNodeID specification");
	}

	public ValueSource wrapValueSource(ValueSource values) {
		if (this.regexHint != null) {
			values = new RegexRestriction(values, this.regexHint);
		}
		if (this.containsHint != null) {
			values = new ContainsRestriction(values, this.containsHint);
		}
		if (this.maxLengthHint != Integer.MAX_VALUE) {
			values = new MaxLengthRestriction(values, this.maxLengthHint);
		}
		
		// TODO: MySQL DateTime hack -- how to do this properly?
		if (this.database != null
				&& this.translationTable == null
				&& this.datatypeURI != null
				&& this.datatypeURI.equals(XSDDatatype.XSDdateTime.getURI())
				&& this.literalColumn != null
				&& this.database.getColumnType(this.literalColumn) == Database.dateColumnType) {
			this.translationTable = new TranslationTable();
			this.translationTable.setTranslator(new DateTimeTranslator());
		}
		if (this.translationTable != null) {
			values = this.translationTable.getTranslatingValueSource(values);
		}
		return values;
	}
	
	private NodeMaker buildNodeMaker(ValueSource values, boolean isUnique) {
		return new TypedNodeMaker(nodeType(), values, isUnique);
	}
	
	private NodeType nodeType() {
		if (this.blankColumns != null) {
			return TypedNodeMaker.BLANK;
		}
		if (this.uriColumn != null || this.uriPattern != null) {
			return TypedNodeMaker.URI;
		}
		if (this.literalColumn == null && this.literalPattern == null) {
			throw new D2RQException(this.mapName + " needs a column/pattern/bNodeID specification");
		}
		if (this.datatypeURI != null && this.lang != null) {
			throw new D2RQException(this.mapName + " has both d2rq:lang and d2rq:datatype");
		}
		if (this.datatypeURI != null) {
			return TypedNodeMaker.typedLiteral(buildDatatype(this.datatypeURI));
		}
		if (this.lang != null) {
			return TypedNodeMaker.languageLiteral(this.lang);
		}
		return TypedNodeMaker.PLAIN_LITERAL;
	}
	
	private RDFDatatype buildDatatype(String datatypeURI) {
		return TypeMapper.getInstance().getSafeTypeByName(datatypeURI);		
	}
	
	private void assertNoPrimarySpec() {
		if (hasPrimarySpec()) {
			throw new D2RQException("Cannot combine multiple column/pattern/bNodeID specifications on " + this.mapName);
		}
	}
	
	private boolean hasPrimarySpec() {
		return this.producedNodeMaker != null || this.refersToClassMap != null || this.fixed != null
				|| this.blankColumns != null
				|| this.uriColumn != null || this.uriPattern != null
				|| this.literalColumn != null || this.literalPattern != null;
	}
}