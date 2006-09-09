package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.shared.PrefixMapping;

import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * LiteralMakers transform attribute values from a result set into literals.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: LiteralMaker.java,v 1.8 2006/09/09 23:25:15 cyganiak Exp $
 */
public class LiteralMaker extends NodeMakerBase {
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
    
	public LiteralMaker(ValueSource valueSource, boolean isUnique, RDFDatatype datatype, String lang) {
		super(isUnique);
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

	public Node getNode(ResultRow row) {
		String value = this.valueSource.getValue(row);
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
		StringBuffer result = new StringBuffer("Literal(");
		result.append(this.valueSource);
		if (this.lang != null) {
			result.append("@" + this.lang);
		} else if (this.datatype != null) {
			result.append("^^");
			result.append(PrettyPrinter.toString(Node.createURI(this.datatype.getURI()), 
					PrefixMapping.Standard));
		}
		result.append(")");
		return result.toString();
	}
}