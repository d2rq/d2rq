/*
 (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
 */
package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.impl.LiteralLabel;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;

/**
 * LiteralMakers transform attribute values from a result set into literals.
 *
 * <p>History:<br>
 * 06-21-2004: Initial version of this class.<br>
 * 08-03-2004: Extended with couldFit, getColumns, getColumnValues
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class LiteralMaker extends NodeMakerBase {
	private String id;
	private ValueSource valueSource;
	private RDFDatatype datatype;
	private String lang;
	
    public void matchConstraint(NodeConstraint c) {
        c.matchNodeType(NodeConstraint.LiteralNodeType);
        c.matchLiteralMaker(this);  
        this.valueSource.matchConstraint(c);
    }

    // jg
    public boolean matchesOtherLiteralMaker(LiteralMaker other) {
        boolean b1,b2;
        b1=(datatype==null && other.datatype==null) || datatype.equals(other.datatype);
        b2=(lang==null && other.lang==null) || lang.equals(other.lang);        	
        return b1 && b2;
    }        	
    
	public LiteralMaker(String id, ValueSource valueSource, boolean isUnique, RDFDatatype datatype, String lang) {
		super(isUnique);
		this.id = id;
		this.valueSource = valueSource;
		this.datatype = datatype;
		this.lang = lang;
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#couldFit(com.hp.hpl.jena.graph.Node)
	 */
	public boolean couldFit(Node node) {
		if (node.equals(Node.ANY)) {
			return true;
		}
		if (!node.isLiteral()) {
			return false;
		}
		LiteralLabel label = node.getLiteral();
		if (!areCompatibleDatatypes(this.datatype, label.getDatatype())) {
			return false;
		}
		String nodeLang = label.language();
		if ("".equals(nodeLang)) {
			nodeLang = null;
		}
		if (!areCompatibleLanguages(this.lang, nodeLang)) {
			return false;
		}
		return this.valueSource.couldFit(label.getLexicalForm());
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#getColumns()
	 */
	public Set getColumns() {
		return this.valueSource.getColumns();
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#getColumnValues(com.hp.hpl.jena.graph.Node)
	 */
	public Map getColumnValues(Node node) {
		return this.valueSource.getColumnValues(node.getLiteral().getLexicalForm());
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#getNode(java.lang.String[], java.util.Map)
	 */
	public Node getNode(String[] row, Map columnNameNumberMap) {
		String value = this.valueSource.getValue(row, columnNameNumberMap);
		if (value == null) {
			return null;
		}
		return Node.createLiteral(value, this.lang, this.datatype);
	}

	private boolean areCompatibleDatatypes(RDFDatatype dt1, RDFDatatype dt2) {
		if (dt1 == null) {
			return dt2 == null;
		}
		return dt1.equals(dt2);
	}

	private boolean areCompatibleLanguages(String lang1, String lang2) {
		if (lang1 == null) {
			return lang2 == null;
		}
		return lang1.equals(lang2);
	}
	
	public String toString() {
		return "LiteralMaker@" + this.id;
	}
}