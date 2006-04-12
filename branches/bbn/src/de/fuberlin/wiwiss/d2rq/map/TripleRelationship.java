package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.List;

/**
 * A relationship tried triple statements together from the query being asked.
 * This allows for smart prefixing of tables, and seperation of SQL statements
 * to avoid unnecessairy joins.  
 * 
 * <p>
 * History:<br>
 * 03-24-2006: Initial version of this class.<br>
 * 
 * @author Matthew Gheen <mgheen@bbn.com>
 */

public class TripleRelationship {
    
    private String var;
    private String table;
    private String prefixedTable = null;
    private ArrayList columns = new ArrayList();
    private ArrayList keyColumns = new ArrayList();
    private ArrayList objectVars = new ArrayList();
    
    /** 
     * @param varName The variable the relationship is being created for. 
     */
    public TripleRelationship( String varName ){
        var = varName;
    }
    
    /**
     * @return Returns an <code>ArrayList</code> columns associated with this relationship.
     */
    public ArrayList getColumns() {
        return columns;
    }
    /**
     * @return Returns an <code>ArrayList</code> of the primary and foriegn keys associated 
     * with this relationship.
     */
    public ArrayList getKeyColumns() {
        return keyColumns;
    }
    /**
     * @return Returns the name of the table associated with this relationship.
     */
    public String getTable() {
        return table;
    }
    /**
     * @return Returns the varable name.
     */
    public String getVar() {
        return var;
    }
    /**
     * @param column Addsa a column from the URI passed in. 
     */
    public void addColumnURI(String column) {
        String col = column;
        int idx=col.lastIndexOf('.');
        col = col.substring( idx + 1, col.length() );
        addColumn( toUpper( col ) );
    }
    /**
     * @param column Adds a column name, (No URI)
     */
    public void addColumn(String column ) {
        columns.add(column);
    }
    
    /**
     * @param column Adds a primary or foreign key column (No URI)
     */
    public void addKeyColumn(String column ) {
        keyColumns.add(column);
    }
    
    /**
     * Sets the table associated with the variable.  
     * Once set, this value will not change.
     * @param url the URL of the table being set.
     */
    public void setTable(String url) {
        if( table == null ){
            int idx=url.lastIndexOf('#');
            table = url.substring( idx + 1, url.length() );
            table = toUpper( table );
        }
    }
    
    /**
     * Helper method to make table names and column names appear as they do
     * in the model. 
     * @param string the table or column to be changed to uppercase. 
     * Ex: exampleName becomes EXAMPLE_NAME
     * @return passed in string in upper case.
     */
    private String toUpper(String string) {
        StringBuffer retval = new StringBuffer();
        
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            if( Character.isUpperCase( ch) && i != 0 ){
                retval.append( "_" + ch );
            } else {
                retval.append( Character.toUpperCase( ch ));
            }
        }
        return retval.toString();
    }
    
    /**
     * @return a string value that represents this relationship.
     */
    public String toString(){
        String retVal = "Var: " + var + " Table: " + table + " Columns: ";
        for( int i = 0;i < columns.size();i++ ){
            retVal += (String)columns.get(i) + " ";
        }
        return retVal;
    }

    /**
     * @return the prefixed table value.
     */
    public String getPrefixedTable() {
        return prefixedTable;
    }

    /**
     * @param prefixedTable the table name prefixed.  This can only be
     * set once.  Futher calls will do nothing. 
     */
    public void setPrefixedTable(String prefixedTable) {
        if( null == this.prefixedTable){
            this.prefixedTable = prefixedTable;
        }
    }
    
    /**
     * @return true if there is a value for prefixed table, else false. 
     */
    public boolean isPrefixed(){
        return null != prefixedTable;
    }
    
    /**
     * Adds a object variable value to the relationship.  
     * These are used when splitting queries. 
     * @param var the object variable name. 
     */
    public void addColumnObjectVar( String var ){
        this.objectVars.add( var );
    }
    
    /**
     * @return a <code>List</code> of object variables associated with this relationship. 
     */
    public List getObjectVars(){
        return objectVars;
    }
    
}
