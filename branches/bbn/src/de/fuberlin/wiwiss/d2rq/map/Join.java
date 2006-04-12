/*
 * $Id: Join.java,v 1.1 2006/04/12 09:53:04 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.rdql.TablePrefixer;

/**
 * Represents an SQL join between two tables, spanning one or more columns.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class Join implements Prefixable {
	private List fromColumns = new ArrayList();
	private List toColumns = new ArrayList();
	private Map otherSide = new HashMap(); 
	private String fromTable = null;
	private String toTable = null;
	private String sqlExpression=null; // cached value
	
	public Object clone() throws CloneNotSupportedException {return super.clone();}
	public void prefixTables(TablePrefixer prefixer) {
		fromTable=prefixer.prefixAndReferTable(fromTable);
		toTable=prefixer.prefixAndReferTable(toTable);	
		if (!prefixer.mayChangeID()) {
			return;
		}
		sqlExpression=null;
		Set columnsToPrefix=otherSide.keySet(); // contains both fromColumns and toColumns
		Map oldNewMap= new HashMap(columnsToPrefix.size());
		prefixer.prefixCollectionIntoCollectionAndMap(columnsToPrefix,null,oldNewMap);
		fromColumns=(List) TablePrefixer.createCollectionFromCollectionWithMap(fromColumns,oldNewMap);
		toColumns=(List) TablePrefixer.createCollectionFromCollectionWithMap(toColumns,oldNewMap);
		Map newOther = new HashMap(fromColumns.size()+toColumns.size());
		Iterator it=otherSide.entrySet().iterator();
		while (it.hasNext()){
			Map.Entry oldEntry=(Map.Entry)it.next();
			Column oldFromColumn=(Column) oldEntry.getKey();
			Column oldToColumn=(Column)oldEntry.getValue();
			Column fromColumn=(Column)oldNewMap.get(oldFromColumn);
			Column toColumn=(Column)oldNewMap.get(oldToColumn);
			newOther.put(fromColumn,toColumn); // pair (toColumn,fromColumn) will be seen in another iteration
		}
		otherSide=newOther;
	}
    
	/**
	 * Prefix the table with the bind variable information considered
	 * @param prefixer the table prefixer
	 * @param boundVar the varaible that the table is bound to
	 */
    public void prefixTables(TablePrefixer prefixer, String boundVar ) {
        fromTable=prefixer.prefixTable(fromTable, boundVar );
        toTable=prefixer.prefixTable(toTable, boundVar );  
        if (!prefixer.mayChangeID()) {
            return;
        }
        sqlExpression=null;
        Set columnsToPrefix=otherSide.keySet(); // contains both fromColumns and toColumns
        Map oldNewMap= new HashMap(columnsToPrefix.size());
        prefixer.prefixCollectionIntoCollectionAndMap(columnsToPrefix,null,oldNewMap);
        fromColumns=(List) TablePrefixer.createCollectionFromCollectionWithMap(fromColumns,oldNewMap);
        toColumns=(List) TablePrefixer.createCollectionFromCollectionWithMap(toColumns,oldNewMap);
        Map newOther = new HashMap(fromColumns.size()+toColumns.size());
        Iterator it=otherSide.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry oldEntry=(Map.Entry)it.next();
            Column oldFromColumn=(Column) oldEntry.getKey();
            Column oldToColumn=(Column)oldEntry.getValue();
            Column fromColumn=(Column)oldNewMap.get(oldFromColumn);
            Column toColumn=(Column)oldNewMap.get(oldToColumn);
            newOther.put(fromColumn,toColumn); // pair (toColumn,fromColumn) will be seen in another iteration
        }
        otherSide=newOther;
    }
	
	public void addCondition(String joinCondition) {
		sqlExpression=null;
		Column col1 = Join.getColumn(joinCondition, true);
		Column col2 = Join.getColumn(joinCondition, false);
		if (this.fromTable == null) {
			this.fromTable = col1.getTableName();
			this.toTable = col2.getTableName();
		}
		this.fromColumns.add(col1);
		this.toColumns.add(col2);
		this.otherSide.put(col1, col2);
		this.otherSide.put(col2, col1);
	}

	public boolean containsTable(String tableName) {
		return tableName.equals(this.fromTable) ||
				tableName.equals(this.toTable);
	}

	public boolean containsColumn(Column column) {
		return this.fromColumns.contains(column) ||
				this.toColumns.contains(column);
	}

	public String getFirstTable() {
		return this.fromTable;
	}
	
	public String getSecondTable() {
		return this.toTable;
	}

	public List getFirstColumns() {
		return this.fromColumns;
	}

	public List getSecondColumns() {
		return this.toColumns;
	}

	/**
	 * Checks if one side of the join is equal to the argument set of
	 * {@link Column}s.
	 * @param columns a set of Column instances
	 * @return <tt>true</tt> if one side of the join is equal to the column set
	 */
	public boolean isOneSide(Set columns) {
		return this.fromColumns.equals(columns) || this.toColumns.equals(columns);
	}

	public Column getOtherSide(Column column) {
		return (Column) this.otherSide.get(column);
	}

	private static Column getColumn(String joinCondition, boolean first) {
		int index = joinCondition.indexOf("=");
		if (index == -1) {
			Logger.instance().error("Illegal d2rq:join: \"" + joinCondition +
					"\" (must be in Table1.Col1=Table2.Col2 form)");
			return null;
		}
		String col1 = joinCondition.substring(0, index).trim();
		String col2 = joinCondition.substring(index + 1).trim();
		boolean return1 = col1.compareTo(col2) < 0;
		if (!first) {
			return1 = !return1;
		}
		return return1 ? new Column(col1) : new Column(col2);
	}

	
	/**
	 * Builds a list of Join objects from a list of join condition
	 * strings. Groups multiple condition that connect the same
	 * two table (multi-column keys) into a single join.
	 * @param joinConditions a collection of strings
	 * @return a set of Join instances
	 */
	public static Set buildJoins(Collection joinConditions) {
		Set result = new HashSet(2);
		Iterator it = joinConditions.iterator();
		while (it.hasNext()) {
			String condition = (String) it.next();
			String table1 = Join.getColumn(condition, true).getTableName();
			String table2 = Join.getColumn(condition, false).getTableName();
			Iterator it2 = result.iterator();
			while (it2.hasNext()) {
				Join join = (Join) it2.next();
				if (join.containsTable(table1) && join.containsTable(table2)) {
					join.addCondition(condition);
					condition = null;
					break;
				}
			}
			if (condition != null) {
				Join join = new Join();
				join.addCondition(condition);
				result.add(join);
			}
		}
		return result;
	}
	public static Join buildJoin(String condition) {
		Join join = new Join();
		join.addCondition(condition);
		return join;
	}
	
	
	public String getSQLExpression() {
		if (sqlExpression!=null) 
			return sqlExpression;
		Object[] from = this.fromColumns.toArray();
		// jg was Object[] to = this.toColumns.toArray();
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < from.length; i++) {
			if (i > 0) {
				result.append(" AND ");
			}
			result.append(((Column)from[i]).getQualifiedName());
			result.append("=");
			result.append(((Column)otherSide.get(from[i])).getQualifiedName()); // jg was to[i]
		}
		sqlExpression=result.toString();
		return sqlExpression;
	}
	public String toString() { // jg: was dubious! Sets do not 
		return super.toString() + "(" + getSQLExpression() + "";
	}
		
}
