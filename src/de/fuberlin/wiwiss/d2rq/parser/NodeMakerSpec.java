package de.fuberlin.wiwiss.d2rq.parser;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.map.BlankNodeIdentifier;
import de.fuberlin.wiwiss.d2rq.map.BlankNodeMaker;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.ContainsRestriction;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.map.JoinNodeMaker;
import de.fuberlin.wiwiss.d2rq.map.LiteralMaker;
import de.fuberlin.wiwiss.d2rq.map.MaxLengthRestriction;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.map.Pattern;
import de.fuberlin.wiwiss.d2rq.map.RegexRestriction;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable;
import de.fuberlin.wiwiss.d2rq.map.UriMaker;
import de.fuberlin.wiwiss.d2rq.map.ValueSource;
import de.fuberlin.wiwiss.d2rq.types.DateTimeTranslator;

/**
 * Builds a {@link NodeMaker} after a specification provided
 * through calls to the setter methods.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeMakerSpec.java,v 1.2 2006/07/12 11:08:09 cyganiak Exp $
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
	private NodeMaker product = null;
	private Node fixed = null;
	private NodeMakerSpec wrapped = null;
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
	private Set joins = new HashSet();
	private Set conditions = new HashSet();
	private boolean isUnique = false;
	private Database database = null;
	
	public NodeMakerSpec(String mapName) {
		this.mapName = mapName;
	}
	
	// TODO: Should the spec really know about the database?
	public void setDatabase(Database db) {
		this.database = db;
	}
	
	public void wrapExisting(NodeMakerSpec existing) {
		this.wrapped = existing;
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
	
	public void addJoins(Set joins) {
		this.joins.addAll(joins);
	}
	
	public void addConditions(Set conditions) {
		this.conditions.addAll(conditions);
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
	
	public NodeMaker build() {
		if (this.product != null) {
			return this.product;
		}
		if (this.wrapped != null) {
			return JoinNodeMaker.create(this.wrapped.build(), 
					this.joins, this.conditions, this.isUnique);
		}
		if (this.fixed != null) {
			return new FixedNodeMaker(this.fixed);
		}
		ValueSource values = buildValueSource();
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
		this.product = buildNodeMaker(values);
		return this.product;
	}

	private ValueSource buildValueSource() {
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

	private NodeMaker buildNodeMaker(ValueSource values) {
		if (this.blankColumns != null) {
			return new BlankNodeMaker(this.mapName, values, this.joins, this.conditions, this.isUnique);
		}
		if (this.uriColumn != null || this.uriPattern != null) {
			return new UriMaker(this.mapName, values, this.joins, this.conditions, this.isUnique);
		}
		if (this.literalColumn != null || this.literalPattern != null) {
			return new LiteralMaker(this.mapName, values, this.joins, this.conditions, this.isUnique, buildDatatype(this.datatypeURI), this.lang);
		}
		throw new D2RQException(this.mapName + " needs a column/pattern/bNodeID specification");
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
		return this.product != null || this.fixed != null
				|| this.blankColumns != null
				|| this.uriColumn != null || this.uriPattern != null
				|| this.literalColumn != null || this.literalPattern != null;
	}
}