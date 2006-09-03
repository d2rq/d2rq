package de.fuberlin.wiwiss.d2rq.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraintWrapper;

/**
 * Wraps another {@link NodeMaker} and presents a view of that NodeMaker
 * where its database tables are renamed according to an alias map.
 * Used in conjunction with <tt>d2rq:alias</tt>.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TableRenamingNodeMaker.java,v 1.2 2006/09/03 17:22:49 cyganiak Exp $
 */
public class TableRenamingNodeMaker extends WrappingNodeMaker {

	public static NodeMaker prefix(NodeMaker base, int index) {
		Set tables = new HashSet();
		Iterator it = base.getColumns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			tables.add(column.getTableName());
		}
		it = base.getJoins().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			tables.add(join.getFirstTable());
			tables.add(join.getSecondTable());
		}
		it = base.condition().columns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			tables.add(column.getTableName());
		}
		Map prefixRenames = new HashMap();
		it = tables.iterator();
		while (it.hasNext()) {
			String tableName = (String) it.next();
			prefixRenames.put("T" + index + "_" + tableName, tableName);
		}
		return new TableRenamingNodeMaker(base, new AliasMap(prefixRenames));
	}
	
	private AliasMap renames;
	private Set columns;
	private Set joins;
	private Expression expression;
	private AliasMap aliases;

	public TableRenamingNodeMaker(NodeMaker base, AliasMap renames) {
		super(base);
		this.renames = renames;
		this.columns = this.renames.applyToColumnSet(this.base.getColumns());
		this.joins = this.renames.applyToJoinSet(this.base.getJoins());
		this.expression = this.renames.applyTo(this.base.condition());
		this.aliases = this.renames.applyTo(this.base.getAliases());
	}
	
	public Map getColumnValues(Node node) {
		return this.renames.applyToMapKeys(this.base.getColumnValues(node));
	}

	public Set getColumns() {
		return this.columns;
	}

	public Set getJoins() {
		return this.joins;
	}

	public Expression condition() {
		return this.expression;
	}

	public AliasMap getAliases() {
		return this.aliases;
	}
	
	public Node getNode(String[] row, Map columnNameNumberMap) {
		return this.base.getNode(row, this.renames.withOriginalKeys(columnNameNumberMap));
	}

	public void matchConstraint(NodeConstraint c) {
		this.base.matchConstraint(new NodeConstraintWrapper(c, this.renames));
	}
}