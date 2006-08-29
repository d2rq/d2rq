package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;

/**
 * Encapsulates a query for a triple pattern on a specific
 * {@link PropertyBridge}. Determines which database columns, database joins
 * and database conditions must be used to find matching triples, and builds
 * Jena triples from SQL result rows. It also has logic to determine if
 * it can be combined with other TripleQuery instances into a single
 * SQL statement.
 * 
 * TODO: The logic for removing unnecessary joins is ugly and probably 
 *       the wrong place; especially {@link #getReplacedColumns()}
 *       seems awkward
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: PropertyBridgeQuery.java,v 1.1 2006/08/29 16:12:14 cyganiak Exp $
 */
public class PropertyBridgeQuery {
	private PropertyBridge bridge;
	private Set joins = new HashSet(2);
	private Map columnValues = new HashMap();
	private Set selectColumns = new HashSet();
	private String aTable;
	private Set subjectColumns;
	private Set objectColumns;
	private NodeMaker subjectMaker;
	private NodeMaker predicateMaker;
	private NodeMaker objectMaker;
	private Map replacedColumns = new HashMap(4);
	private AliasMap aliases;

	/**
	 * Constructs a new TripleQuery.
	 * @param bridge We look for triples matching this property bridge
	 * @param subject the subject node, may be {{Node.ANY}}
	 * @param object the object node, may be {{Node.ANY}}
	 */
	public PropertyBridgeQuery(PropertyBridge bridge, Triple query) {
		this.bridge = bridge;
		this.subjectColumns = bridge.getSubjectMaker().getColumns();
		this.objectColumns = bridge.getObjectMaker().getColumns();
	
		if (query.getSubject().isConcrete()) {
			this.columnValues.putAll(bridge.getSubjectMaker().getColumnValues(query.getSubject()));
			this.subjectMaker = new FixedNodeMaker(query.getSubject());
		} else {
			this.selectColumns.addAll(bridge.getSubjectMaker().getColumns());
			this.subjectMaker = bridge.getSubjectMaker();
		}
		if (query.getPredicate().isConcrete()) {
			this.columnValues.putAll(bridge.getPredicateMaker().getColumnValues(query.getPredicate()));
			this.predicateMaker = new FixedNodeMaker(query.getPredicate());
		} else {
			this.selectColumns.addAll(bridge.getPredicateMaker().getColumns());
			this.predicateMaker = bridge.getPredicateMaker();
		}
		if (query.getObject().isConcrete()) {
			this.columnValues.putAll(bridge.getObjectMaker().getColumnValues(query.getObject()));
			this.objectMaker = new FixedNodeMaker(query.getObject());
		} else {
			this.selectColumns.addAll(bridge.getObjectMaker().getColumns());
			this.objectMaker = bridge.getObjectMaker();
		}

		this.joins.addAll(bridge.getJoins());
		removeOptionalJoins();
		this.aliases = bridge.getAliases();

		if (!this.selectColumns.isEmpty()) {
			this.aTable = ((Column) this.selectColumns.iterator().next()).getTableName();
		} else if (!this.columnValues.isEmpty()) {
			this.aTable = ((Column) this.columnValues.keySet().iterator().next()).getTableName();
		}
	}
	
	public NodeMaker getNodeMaker(int i) {
	    if (i==0) {
	        return subjectMaker;
	    } else if (i==1) {
	        return predicateMaker;
	    } else if (i==2) {
	        return objectMaker;
	    } else 
	        return null;
	}
	
	public PropertyBridge getPropertyBridge() {
		return bridge;
	}
	public Set getJoins() {
		return this.joins;
	}
	
	public AliasMap getAliases() {
		return this.aliases;
	}
	
	public Set getConditions() {
		return this.bridge.getConditions();
	}

	public Map getColumnValues() {
		return this.columnValues;
	}
	
	public Set getSelectColumns() {
		return this.selectColumns;
	}

	public Database getDatabase() {
		return this.bridge.getDatabase();
	}

	public boolean mightContainDuplicates() {
		return this.bridge.mightContainDuplicates();
	}

	/**
	 * Returns the name of an arbitrary database table that is accessed
	 * by this query. Useful only when the query accesses
	 * only one table, that is, it has no joins.
	 * @return an arbitrary table that is accessed by this query
	 */
	public String getATable() {
		return this.aTable;
	}

	/**
	 * Checks if an other query can be combined with this one into
	 * a single SQL statement. Two queries can be combined iff
	 * they access the same database and they contain exactly the same
	 * joins and WHERE clauses. If they both contain no joins, they
	 * must contain only columns from the same table.
	 * @param other a query to be compared with this one
	 * @return <tt>true</tt> if both are combinable
	 */
	public boolean isCombinable(PropertyBridgeQuery other) {
		if (!getDatabase().equals(other.getDatabase())) {
			return false;
		}
		if (this.bridge.mightContainDuplicates() || other.bridge.mightContainDuplicates()) {
			return false;
		}
		if (!getJoins().equals(other.getJoins())) {
			return false;
		}
		if (!getConditions().equals(other.getConditions())) {
			return false;
		}
		if (!getColumnValues().equals(other.getColumnValues())) {
			return false;
		}
		if (getJoins().isEmpty() && !getATable().equals(other.getATable())) {
			return false;
		}
		if (!getAliases().equals(other.getAliases())) {
			return false;
		}
		return true;
	}

	/**
	 * Returns a map from old {@link Column}s to new columns. The {@link #makeTriple}
	 * method expects to find the old column names in the
	 * <tt>columnNameNumberMap</tt>, but the new columns are returned by
	 * the {@link #getSelectColumns()} methods. Clients of this class
	 * must account for this when creating the <tt>columnNameNumberMap</tt>.
	 * 
	 * @return a map from Columns to Columns
	 */
	public Map getReplacedColumns() {
		return this.replacedColumns;
	}

	/**
	 * Creates a triple from a database result row.
	 * @param row a database result row
	 * @param columnNameNumberMap a map from column names to Integer indices into the row array
	 * @return a triple extracted from the row
	 */
	public Triple makeTriple(String[] row, Map columnNameNumberMap) {
		Node s = this.subjectMaker.getNode(row, columnNameNumberMap );
		Node p = this.predicateMaker.getNode(row, columnNameNumberMap);
		Node o = this.objectMaker.getNode(row, columnNameNumberMap);
		if (s == null || p == null || o == null) {
			return null;
		}
		return new Triple(s, p, o);
	}

	private void removeOptionalJoins() {
		if (!this.bridge.getConditions().isEmpty()) {
			return;
		}
		Iterator it = getAllJoinTables().iterator();
		while (it.hasNext()) {
			String tableName = (String) it.next();
			Join singleReferencingJoin =
					getSingleJoinReferencingTable(tableName);
			if (singleReferencingJoin == null) {
				continue;
			}
			if (!isOptionalTable(tableName, singleReferencingJoin)) {
				continue;
			}
			this.joins.remove(singleReferencingJoin);
			eliminateColumns(singleReferencingJoin, tableName);
		}		
	}

	private Set getAllJoinTables() {
		Set result = new HashSet(this.joins.size() + 1);
		Iterator it = this.joins.iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			result.add(join.getFirstTable());
			result.add(join.getSecondTable());
		}
		return result;
	}

	/**
	 * Gets a join that references a table, but only if there is exactly
	 * one join that references that table. If there is none or there are
	 * more than one, null will be returned. We use this to find joins
	 * that can be ignored. Joins can't be ignored if more than
	 * one join reference a particular table.
	 */
	private Join getSingleJoinReferencingTable(String table) {
		Join result = null;
		Iterator it = this.joins.iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			if (!join.containsTable(table)) {
				continue;
			}
			if (result != null) {
				return null;
			}
			result = join;
		}
		return result;
	}

	/**
	 * Checks if a table can be dropped. Only tables that
	 * are referenced in exactly one join can ever be dropped;
	 * this has to be checked before calling this method. A table
	 * can be dropped if the join covers exactly the columns
	 * that we need from this table, because in this case the
	 * same values are, by definition, present in the other
	 * table. There's an exception: If the join covers exactly
	 * all columns that we need from <em>both</em> tables
	 * that it connects, then we don't drop the table because
	 * of some reason I'm too tired to remember right now. Has
	 * something to do with 1:1 joins, I believe. If you read
	 * this, you may send me flame mail.
	 * 
	 * @param table a table name
	 * @param join the only join referencing this table
	 * @return <tt>true</tt> if the join can be dropped
	 */
	private boolean isOptionalTable(String table, Join join) {
		if (join.getFirstTable().equals(table)) {
			return (join.getFirstColumns().equals(this.subjectColumns) &&
					!join.getSecondColumns().equals(this.objectColumns)) ||
					(join.getFirstColumns().equals(this.objectColumns) &&
							!join.getSecondTable().equals(this.subjectColumns));
		} else if (join.getSecondTable().equals(table)) {
			return (join.getSecondColumns().equals(this.subjectColumns) &&
					!join.getFirstColumns().equals(this.objectColumns)) ||
					(join.getSecondColumns().equals(this.objectColumns) &&
							!join.getFirstTable().equals(this.subjectColumns));			
		}
		return false;
	}
	
	/**
	 * If we have determined that a table can be dropped, then
	 * we have to rewrite all references to columns in that table
	 * to the columns on the other side of the join.
	 * @param join
	 * @param tableName
	 */
	private void eliminateColumns(Join join, String tableName) {
		Iterator it = this.selectColumns.iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			if (!column.getTableName().equals(tableName)) {
				continue;
			}
			if (!join.containsColumn(column)) {
				continue;
			}
			this.replacedColumns.put(column, join.getOtherSide(column));
		}
		this.selectColumns.removeAll(this.replacedColumns.keySet());
		this.selectColumns.addAll(this.replacedColumns.values());
		List oldColumns = new ArrayList(3);
		Map newColumnsAndValues = new HashMap(3);
		it = this.columnValues.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			Column column = (Column) entry.getKey();
			if (!column.getTableName().equals(tableName)) {
				continue;
			}
			if (!join.containsColumn(column)) {
				continue;
			}
			oldColumns.add(column);
			newColumnsAndValues.put(join.getOtherSide(column), entry.getValue());
		}
		this.columnValues.keySet().removeAll(oldColumns);
		this.columnValues.putAll(newColumnsAndValues);
	}
}
