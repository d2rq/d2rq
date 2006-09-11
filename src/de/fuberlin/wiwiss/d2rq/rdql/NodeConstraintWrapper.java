package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.List;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;
import de.fuberlin.wiwiss.d2rq.values.BlankNodeID;
import de.fuberlin.wiwiss.d2rq.values.Pattern;

/**
 * A {@link NodeConstraint} that wraps another NodeConstraint and
 * presents a view where all tables are renamed according to a
 * {@link ColumnRenamer}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeConstraintWrapper.java,v 1.8 2006/09/11 23:22:27 cyganiak Exp $
 */
public class NodeConstraintWrapper implements NodeConstraint {
	private NodeConstraint base;
	private ColumnRenamer renames;
	
	public NodeConstraintWrapper(NodeConstraint base, ColumnRenamer renames) {
		this.base = base;
		this.renames = renames;
	}
	
	public boolean isPossible() {
		return this.base.isPossible();
	}

	public void matchImpossible() {
		this.base.matchImpossible();
	}
	
	public void addEqualColumn(Attribute c1, Attribute c2) {
		this.base.addEqualColumn(
				this.renames.applyTo(c1), this.renames.applyTo(c2));
	}

	public void matchFixedNode(Node node) {
		this.base.matchFixedNode(node);
	}

	public void matchNodeType(int t) {
		this.base.matchNodeType(t);
	}

	public void matchLiteralType(String language, RDFDatatype datatype) {
		this.base.matchLiteralType(language, datatype);
	}
	
	public void matchColumn(Attribute c) {
		this.base.matchColumn(this.renames.applyTo(c));
	}

	public void matchPattern(Pattern p, List columns) {
		this.base.matchPattern(p, this.renames.applyToColumnList(columns));
	}

    public void matchBlankNodeIdentifier(BlankNodeID id, List columns) {
    	this.base.matchBlankNodeIdentifier(id, this.renames.applyToColumnList(columns));
    }

    public void addConstraintsToSQL(SelectStatementBuilder sql) {
		this.base.addConstraintsToSQL(sql);
	}
}