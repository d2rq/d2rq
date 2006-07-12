package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.rdql.TablePrefixer;

public abstract class NodeMakerBase implements NodeMaker, Prefixable {
	private Set joins;
	private Set conditions;
	private boolean isUnique;
	
	public NodeMakerBase(Set joins, Set conditions, boolean isUnique) {
		this.joins = joins;
		this.conditions = conditions;
		this.isUnique = isUnique;
	}
	
	public abstract void matchConstraint(NodeConstraint c);

	public abstract boolean couldFit(Node node);

	public abstract Map getColumnValues(Node node);

	public abstract Set getColumns();

	public Set getJoins() {
		return this.joins;
	}

	public Set getConditions() {
		return this.conditions;
	}
	
	public abstract Node getNode(String[] row, Map columnNameNumberMap);

	public boolean isUnique() {
		return this.isUnique;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void prefixTables(TablePrefixer prefixer) {
		this.conditions = prefixer.prefixConditions(this.conditions);
		this.joins = prefixer.prefixSet(this.joins);
	}
}
