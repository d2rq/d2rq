package de.fuberlin.wiwiss.d2rq.map;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.rdql.TablePrefixer;

/**
 * A node maker that wraps another node maker and adds some joins
 * and/or conditions to the wrapped node maker.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: JoinNodeMaker.java,v 1.1 2006/07/12 11:08:09 cyganiak Exp $
 */
public class JoinNodeMaker extends NodeMakerBase {
	
	public static NodeMaker create(NodeMaker other, Set joins, Set conditions, boolean isUnique) {
		Set allJoins = new HashSet(joins);
		allJoins.addAll(other.getJoins());
		Set allConditions = new HashSet(conditions);
		allConditions.addAll(other.getConditions());
		return new JoinNodeMaker(other, allJoins, allConditions, isUnique && other.isUnique());
	}
	
	private NodeMaker other;

	private JoinNodeMaker(NodeMaker other, Set allJoins, Set allConditions, boolean isUnique) {
		super(allJoins, allConditions, isUnique);
		this.other = other;
	}
	
	public void matchConstraint(NodeConstraint c) {
		other.matchConstraint(c);
	}

	public boolean couldFit(Node node) {
		return other.couldFit(node);
	}

	public Map getColumnValues(Node node) {
		return other.getColumnValues(node);
	}

	public Set getColumns() {
		return other.getColumns();
	}

	public Node getNode(String[] row, Map columnNameNumberMap) {
		return other.getNode(row, columnNameNumberMap);
	}
	
	public void prefixTables(TablePrefixer prefixer) {
		super.prefixTables(prefixer);
		this.other = prefixer.prefixNodeMaker(this.other);
	}
}
