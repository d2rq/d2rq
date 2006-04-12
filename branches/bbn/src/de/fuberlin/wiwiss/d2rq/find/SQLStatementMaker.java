/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.hp.hpl.jena.graph.query.Pattern;

import de.fuberlin.wiwiss.d2rq.map.Alias;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.map.TripleRelationship;
import de.fuberlin.wiwiss.d2rq.utils.StringUtils;

/**
 * Collects parts of a SELECT query and delivers a corresponding SQL statement.
 * If the query can be split it will, more than one query will be returned. 
 * Used within TripleResultSets.
 *
 * <p>History:<br>
 * 06-07-2004: Initial version of this class.<br>
 * 08-03-2004: Higher-level operations added (addColumnValues etc.)
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */

public class SQLStatementMaker {
	private Database database;
	private List sqlSelect = new ArrayList();
//	private List sqlFrom = new ArrayList(5); // -> referedTables, aliasMap
	private List sqlWhere = new ArrayList();
    
    //Lists used to seperate queries.  If there is a singulr version of a plural variable
	//the contents of the plural are the split values.  
    private List columnNameNumbers = null;
    private List relationships;
    private List sqlSelects = null;
    private List sqlWheres = null;
    private List tables = null;
    private List conjunctions = null; //List of TripleQuery[]
    private List conjunction;
    private List compiled = null;
    private List compiledArray = null;
    
	/** Maps column names from the database to columns numbers in the result set. */
	private Map columnNameNumber = new HashMap(10);
	private int selectColumnCount = 0;
	private boolean eliminateDuplicates = false;
	
	// see SQLStatementMaker.sqlFromExpression(referredTables,aliasMap)
	protected Map aliasMap=new HashMap(1); // from String (aliased Table) to Alias
	protected Collection referedTables = new HashSet(5); // Strings in their alias forms	

	/**
	 * Contructor used when splitting of the SQL queries is NOT desired. 
	 * @param con an array of triple query conjunctions.
	 * @param relationships a <code>List</code> of {@link TripleRelationships}
	 */
    public SQLStatementMaker(TripleQuery[] con, List relationships ) {
        this.relationships = relationships;
        this.database = con[0].getDatabase();
        setEliminateDuplicates(database.correctlyHandlesDistinct());
        this.conjunction = new ArrayList(con.length);
        for( int i = 0;i < con.length;i++ ){
            this.conjunction.add(con[i]);
        }
    }
    
    /**
	 * Contructor used when splitting of the SQL queries is desired. 
	 * @param con an array of triple query conjunctions.
	 * @param relationships a <code>List</code> of {@link TripleRelationships}
	 * @param compiled an array of the compiled triple Patterns
	 */
	public SQLStatementMaker(TripleQuery[] con, List relationships, Pattern[] compiled ) {
	    this( con, relationships );
        this.compiled = new ArrayList();
        for( int i = 0;i < compiled.length;i++){
            this.compiled.add(compiled[i]);
        }
	}
	
	/**
	 * Accessor for the database
	 * @return the database
	 */
	public Database getDatabase() {
	    return this.database;
	}

	/**
	 * Generates and returns the SQLStatements for the query(s)
	 * @return a <code>List</code> of SQL statements. 
	 */
	public List getSQLStatements() {
        columnNameNumbers = null;
        sqlSelects = null;
        sqlWheres = null;
        tables = null;
        conjunctions = null;
        List retVal = new ArrayList();
        List seperatable = seperatable();
        //Seperate out the tables from ColumnNameNumber
        seperateColumnNameNumber( seperatable );
        for( int i = 0;i < sqlSelects.size();i++){
            retVal.add( makeSQLStatement(i) );
        }
        
		return retVal;
	}
    
	/**
	 * Seperates everything that needs to be seperated so that the SQL query result
	 * sets can be reconstructed and returned.
	 * @param seperatables the <code>List</code> of seperatable triple statements. 
	 */
    private void seperateColumnNameNumber( List seperatables ){
        columnNameNumbers = new ArrayList();
        sqlSelects = new ArrayList();
        sqlWheres = new ArrayList();
        tables = new ArrayList();
        conjunctions = new ArrayList();
        compiledArray = new ArrayList();
        if( seperatables.size() == 0 ){
            //No splitting necessairy
            columnNameNumbers.add(columnNameNumber);
            sqlSelects.add( sqlSelect );
            sqlWheres.add(sqlWhere);
            tables.add( referedTables );
            conjunctions.add(conjunction);
            if( null != compiled ){
                compiledArray.add(compiled);
            }
        } else {
            for( int i = 0;i < seperatables.size();i++ ){
                //Seperate out the selects
                List ss = new ArrayList();
                Map cnn = new HashMap();
                int conCount = 1;
                String prefixedTable = (String)seperatables.get(i);
                for( int j =0;j < sqlSelect.size();j++ ){
                    String select = (String)sqlSelect.get(j); 
                    if( select.indexOf(prefixedTable) == 0 ){
                        ss.add(select);
                        sqlSelect.remove(j);
                        cnn.put( select, new Integer(conCount) );
                        columnNameNumber.remove(select);
                        conCount++;
                        j--;
                    }
                }
                sqlSelects.add(ss);
                columnNameNumbers.add(cnn);
                
                List cons = new ArrayList();
                List comp = new ArrayList();
                for( int j = 0;j < conjunction.size();j++ ){
                    if( ((TripleQuery)conjunction.get(j)).getATable().equals(prefixedTable) ){
                        cons.add((TripleQuery)conjunction.get(j));
                        comp.add(compiled.get(j));
                        conjunction.remove(j);
                        compiled.remove(j);
                        j--;
                    }
                }
                conjunctions.add(cons);
                compiledArray.add(comp);
                
                //Seperate out the table
                Collection t = new HashSet(1);
                t.add(prefixedTable);
                referedTables.remove(prefixedTable);
                tables.add(t);
                //Seperate out there wheres
                List sw = new ArrayList();
                for( int j =0;j < sqlWhere.size();j++ ){
                    if( ((String)sqlWhere.get(j)).indexOf(prefixedTable) == 0 ){
                        sw.add((String)sqlWhere.get(j));
                        sqlWhere.remove(j);
                    }
                }
                sqlWheres.add(sw);
            } //for
            if( sqlSelect.size() > 0 ){
                sqlSelects.add(sqlSelect);
            }
            if( referedTables.size() > 0 ){
                tables.add(referedTables);
            }
            if( sqlWhere.size() > 0 ){
                sqlWheres.add(sqlWhere);
            }
            if( columnNameNumber.size() > 0 ){
                Map cnn = new HashMap();
                int conCount = 1;
                for( int j =0;j < sqlSelect.size();j++ ){
                    String select = (String)sqlSelect.get(j); 
                    cnn.put( select, new Integer(conCount) );
                    conCount++;
                }
                columnNameNumbers.add(cnn);
            }
            if( compiled.size() > 0 ){
                compiledArray.add(compiled);
            }
            if( conjunction.size() > 0 ){
                conjunctions.add(conjunction);
            }
        }
    }
    
    /**
     * Constructs the SQL statement
     * @param statement the number of the statement to construct. 
     * @return the SQL statement
     */
    private String makeSQLStatement( int statement ){
        StringBuffer result = new StringBuffer("SELECT ");
        if (this.eliminateDuplicates) {
            result.append("DISTINCT ");
        }
        Iterator it = ((List)sqlSelects.get(statement)).iterator();
        if (!it.hasNext()) {
            result.append("1");
        }
        while (it.hasNext()) {
            Object obj = it.next();
            //System.out.println( "Class: " + obj.getClass() );
            String columnname = (String)obj; 
            result.append(columnname);
            if (it.hasNext()) {
                result.append(", ");
            }
        }
        result.append(" FROM ");
        result.append(sqlFromExpression((Collection)tables.get(statement),aliasMap));
//      it = this.sqlFrom.iterator();
//      while (it.hasNext()) {
//          result.append(it.next());
//          if (it.hasNext()) {
//              result.append(", ");
//          }
//      }
        it = ((List)sqlWheres.get(statement)).iterator();
        if (it.hasNext()) {
            result.append(" WHERE ");
            while (it.hasNext()) {
                result.append(it.next());
                if (it.hasNext()) {
                    result.append(" AND ");
                }
            }
        }
        if(database.getJdbcDriver().indexOf("oracle")==-1){
            result.append(";");
        }
        return result.toString();
    }
    
    /**
     * Determines which tables are seperatable, and groups the statements based 
     * on which one goes with each SQL statement.
     * @return a list of tables that are seperatable
     */
    private List seperatable(){
        List retVal = new ArrayList();
        //Make sure there is more than one table in the query.
        if( referedTables.size() > 1 && null != compiled ){
            //Make sure there is a where statement, if not, then its seperatable.. move on. 
            if( sqlWhere.size() < 1 ){
                retVal.addAll(referedTables);
            } else {
                //Check each table's columns with every other table to see which tables are seperatable
                for( int i = 0;i < relationships.size();i++ ){
                    boolean contains = false;
                    int j = i + 1;
                    for( ;j < relationships.size() && !contains;j++ ){
                        Iterator tr1iter = ((TripleRelationship)relationships.get(i)).getObjectVars().iterator();
                        List tr2Cols = ((TripleRelationship)relationships.get(j)).getObjectVars();
                        while( tr1iter.hasNext() ){
                            if( tr2Cols.contains(tr1iter.next()) ){
                                contains = true;
                                break;
                            }
                        }
                    }
                    if( !contains && i + 1 < relationships.size() ){
                        retVal.add(((TripleRelationship)relationships.get(i)).getPrefixedTable());
                    }
                }
            }
        }
        return retVal;
    }    
	
	public void addAliasMap(Map m) {
	    HashMap hm = new HashMap( m );
	    Iterator iter = hm.values().iterator();
	    Iterator aliasIter = aliasMap.values().iterator();
	    String dbTable = null;
	    while( iter.hasNext() ){
	        dbTable = ((Alias)iter.next()).databaseTable();
	        if( aliasIter.hasNext() ){
	            while( aliasIter.hasNext() ){
	                if( !((Alias)aliasIter.next()).databaseTable().equals( dbTable ) ){
	                    aliasMap.putAll(m);
	                }
	            }
	        } else {
	            aliasMap.putAll(m);
	        }
        }
	}
	
	private void referTable(String tableName) {
		if (!referedTables.contains(tableName)) {
			referedTables.add(tableName);
		}
	}
	private void referColumn(Column c) {
		String tableName=c.getTableName();
		referTable(tableName);
	}

	/**
	 * Adds a column to the SELECT part of the query.
	 * @param column the column
	 */
	public void addSelectColumn(Column column) {
		String qualName=column.getQualifiedName();
		if (this.sqlSelect.contains(qualName)) {
			return;
		}
		this.sqlSelect.add(qualName);
		this.selectColumnCount++;
		this.columnNameNumber.put(qualName,
				new Integer(this.selectColumnCount));		
		referColumn(column); // jg
	}

    /**
     * Adds a list of {@link Column}s to the SELECT part of the query
     * @param columns
     */
	public void addSelectColumns(Set columns) {
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			addSelectColumn((Column) it.next());
		}
	}

    /**
     * Adds a WHERE clause to the query. Only records are selected
     * where the column given as the first argument has the value
     * given as second argument.
     * @param column the column whose values are to be restricted
     * @param value the value the column must have
     */
	public void addColumnValue(Column column, String value) {
		String whereClause = column.getQualifiedName() + "=" + correctlyQuotedColumnValue(column,value); 
		if (this.sqlWhere.contains(whereClause)) {
			return;
		}
		this.sqlWhere.add(whereClause);
		referColumn(column);
	}
	
	public String correctlyQuotedColumnValue(Column column, String value) {
	    return getQuotedColumnValue(value, columnType(column));
	}
	
	public int columnType(Column column) {
	    String databaseColumn=column.getQualifiedName(aliasMap);
	    return this.database.getColumnType(databaseColumn);
	}
	
	/**
	 * Adds multiple WHERE clauses from a map. The map keys are
	 * {@link Column} instances. The map values are the values
	 * for those columns.
	 * @param columnsAndValues a map containing columns and their values
	 */
	public void addColumnValues(Map columnsAndValues) {
		Iterator it = columnsAndValues.keySet().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			String value = (String) columnsAndValues.get(column);
			addColumnValue(column, value);
		}	
	}
	
	/**
	 * Adds multiple WHERE clauses to the query.
	 * @param conditions a set of Strings containing SQL WHERE clauses
	 */
	public void addConditions(Set conditions) {
		Iterator it = conditions.iterator();
		while (it.hasNext()) {
			String condition = (String) it.next(); 
			if (this.sqlWhere.contains(condition)) {
				continue;
			}
			this.sqlWhere.add(condition);			
		}
	}

	public void addJoins(Set joins) {
		Iterator it = joins.iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			String expression=join.getSQLExpression();
			if (this.sqlWhere.contains(expression)) {
				continue;
			}
			this.sqlWhere.add(expression);
			referTable(join.getFirstTable());
			referTable(join.getSecondTable());
        }
    }

	/**
	 * Make columns accessible through their old, pre-renaming names.
	 * @param renamedColumns
	 */
	public void addColumnRenames(Map renamedColumns) { 
		Iterator it = renamedColumns.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			String oldName = ((Column) entry.getKey()).getQualifiedName();
			String newName = ((Column) entry.getValue()).getQualifiedName();
			this.columnNameNumber.put(oldName, this.columnNameNumber.get(newName));
		}
	}

	/**
	 * Sets if the SQL statement should eliminate duplicate rows
	 * ("SELECT DISTINCT").
	 * @param eliminateDuplicates enable DISTINCT?
	 */
	public void setEliminateDuplicates(boolean eliminateDuplicates) {
		this.eliminateDuplicates = eliminateDuplicates;
	}

	/**
	 * Accessor for the split ColumnNameNumberMap
	 * @param statementNum the SQL statement being reconstructed
	 * @return the ColumnNameNumberMap for the SQL statement being reconstructed. 
	 */
	public Map getColumnNameNumberMap( int statementNum ) {
        if( null == columnNameNumbers ){
            return this.columnNameNumber;
        } else {
            return (Map)columnNameNumbers.get( statementNum );
        }
	}
    
	/**
	 * Accessor for the split conjunctions
	 * @param statement the SQL statement being reconstructed
	 * @return the conjunctions that are associated with the sql statement
	 */
    public Object[] getConjunction( int statement ){
        if( null != conjunctions){
            return ((ArrayList)conjunctions.get(statement)).toArray();
        } else {
            return conjunction.toArray();
        }  
    }
    
    /**
     * Accessor for the split compiled patterns
     * @param statement the SQL statement being reconstructed
     * @return the compiled patterns that are associated with the SQL statement
     */
    public Object[] getCompiled( int statement){
        List retVal = (List)compiledArray.get(statement);
        return (Object[])retVal.toArray();
    }

	private static String getQuotedColumnValue(String value, int columnType) {
		if (Database.numericColumnType==columnType) {
			// convert to number and back to String to avoid SQL injection
			try {
				return Integer.toString(Integer.parseInt(value));
			} catch (NumberFormatException nfex) {
				try {
					return Double.toString(Double.parseDouble(value));
				} catch (NumberFormatException nfex2) {
					return "NULL";
				}
			}
		} else if (Database.dateColumnType==columnType) {
			return "#" + value + "#";
		}
		return "'" + StringUtils.escape(value) + "'";
	}
	
	// jg
	private static String sqlFromExpression(Collection referedTables, Map aliasMap) {
		StringBuffer result = new StringBuffer();
		Iterator it=referedTables.iterator();
		int i=0;
		while (it.hasNext()) {			
			if (i > 0) {
				result.append(" , ");
			}
			String tableName=(String)it.next();
			String expression=tableName;
			if (aliasMap!=null) {
				Alias mapVal=(Alias)aliasMap.get(tableName);
				
				if (mapVal!=null) {
					expression=mapVal.sqlExpression();
				} 
			}
			result.append(expression);
			i++;
		}
		return result.toString();
	}

	
}
