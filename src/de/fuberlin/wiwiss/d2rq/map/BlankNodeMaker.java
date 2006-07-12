/*
 (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
 */
package de.fuberlin.wiwiss.d2rq.map;

import java.util.*;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.AnonId;

import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;
import de.fuberlin.wiwiss.d2rq.rdql.TablePrefixer;

/**
 * BlankNodeMakers transform attribute values from a result set into blank nodes.
 *
 * <p>History:<br>
 * 06-21-2004: Initial version of this class.<br>
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class BlankNodeMaker extends NodeMakerBase {
	private String id;
	private ValueSource valueSource;
	
	public void matchConstraint(NodeConstraint c) {
        c.matchNodeType(NodeConstraint.BlankNodeType);
        c.matchValueSource(valueSource);
	}       
	
	public BlankNodeMaker(String id, ValueSource valueSource, Set joins, Set conditions, boolean isUnique) {
		super(joins, conditions, isUnique);
		this.id = id;
		this.valueSource = valueSource;
	}

	public boolean couldFit(Node node) {
		if (Node.ANY.equals(node)) {
			return true;
		}
		return node.isBlank() &&
				this.valueSource.couldFit(node.getBlankNodeId().toString());
	}

	public Set getColumns() {
		return this.valueSource.getColumns();
	}

	public Map getColumnValues(Node node) {
		return this.valueSource.getColumnValues(node.getBlankNodeId().toString());
	}

	/**
	 * Creates a new blank node based on the current row of the result set
	 * and the mapping of database column names to elements of the array.
	 * Returns null if a NULL value was retrieved from the database.
	 */
	public Node getNode(String[] row, Map columnNameNumberMap) {
		String value = this.valueSource.getValue(row, columnNameNumberMap);
		if (value == null) {
			return null;
		}		
		return Node.createAnon(new AnonId(value));
	}
	
	public String toString() {
		return "BlankNodeMaker@" + this.id;
	}
	
	public void prefixTables(TablePrefixer prefixer) {
		super.prefixTables(prefixer);
		this.valueSource = prefixer.prefixValueSource(this.valueSource);
	}	
}