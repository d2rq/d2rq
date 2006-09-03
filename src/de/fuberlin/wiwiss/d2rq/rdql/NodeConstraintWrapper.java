package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.List;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.BlankNodeIdentifier;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.LiteralMaker;
import de.fuberlin.wiwiss.d2rq.map.Pattern;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/**
 * A {@link NodeConstraint} that wraps another NodeConstraint and
 * presents a view where all tables are renamed according to an
 * {@link AliasMap}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NodeConstraintWrapper.java,v 1.3 2006/09/03 00:08:11 cyganiak Exp $
 */
public class NodeConstraintWrapper implements NodeConstraint {
	private NodeConstraint base;
	private AliasMap aliases;
	
	public NodeConstraintWrapper(NodeConstraint base, AliasMap aliases) {
		this.base = base;
		this.aliases = aliases;
	}
	
	public boolean isPossible() {
		return this.base.isPossible();
	}

	public void matchImpossible() {
		this.base.matchImpossible();
	}
	
	public void addEqualColumn(Column c1, Column c2) {
		this.base.addEqualColumn(
				this.aliases.applyTo(c1), this.aliases.applyTo(c2));
	}

	public void matchFixedNode(Node node) {
		this.base.matchFixedNode(node);
	}

	public void matchNodeType(int t) {
		this.base.matchNodeType(t);
	}

	public void matchLiteralMaker(LiteralMaker m) {
		this.base.matchLiteralMaker(m);
	}
	
	public void matchColumn(Column c) {
		this.base.matchColumn(this.aliases.applyTo(c));
	}

	public void matchPattern(Pattern p, List columns) {
		this.base.matchPattern(p, this.aliases.applyToColumnList(columns));
	}

    public void matchBlankNodeIdentifier(BlankNodeIdentifier id, List columns) {
    	this.base.matchBlankNodeIdentifier(id, this.aliases.applyToColumnList(columns));
    }

    public void addConstraintsToSQL(SelectStatementBuilder sql) {
		this.base.addConstraintsToSQL(sql);
	}
}