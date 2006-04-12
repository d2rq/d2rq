/*
 (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
 */

package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.helpers.Logger;
import de.fuberlin.wiwiss.d2rq.map.Alias;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.map.Prefixable;
import de.fuberlin.wiwiss.d2rq.map.TripleRelationship;

/** 
 * A class for collecting database table information and for creating table aliases.
 *  
 * @author jgarbers
 *
 */
public class TablePrefixer {
    private static java.util.regex.Pattern allowedTablePattern = java.util.regex.Pattern.compile("\\w+"); 
    public static boolean mayPrefixPrefixedString=true; // set in runtime variables to false during testing!
    private static final String prefixSeparator = "_";
    private static int prefixStringContinuation=2; // see 
    
    private static final String triplePrefix = "T"; 
    /** 
     * Creates a filtered collection that contains the mapped values of <code>collection</code>.
     * @param collection provides the keys
     * @param map provides the values
     * @return a new collection of same type as <code>collection</code>
     */
    public static Collection createCollectionFromCollectionWithMap(Collection collection, Map map) {
        Collection results=newEmptyCollection(collection);
        Iterator it=collection.iterator();
        while (it.hasNext()){
            Object entry=it.next();
            Object result=map.get(entry);
            results.add(result);
        }
        return results;
    } 
    /** 
     * Creates a collection of same type as <code>oldType</code>
     * @param oldType a collection exemplar
     * @return the newly created collection
     */
    public static Collection newEmptyCollection(Collection oldType) {
        Class cls=oldType.getClass();
        Collection inst=null;
        // Constructor cons=cls.getDeclaredConstructor(intParameterTypes);
        try {
            inst=(Collection)cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("TablePrefixer: class "+cls+" has no () constructor or is no collection");
        }
        return inst;
    }
    // prefixing and getting the table references and aliases right 
    // see SQLStatementMaker.sqlFromExpression(referredTables,aliasMap)
    /** given alias map from String (aliased Table) to Alias */
    private Map aliasMap;
    
    /** new aliases. created during prefixing */
    private Map prefixedAliasMap;
    /** Database table names (Strings) in their alias form. */
    private Set referedTables = new HashSet(5);
    private ArrayList relationships;
    /** 
     * The common prefix to use in aliasing.
     * If null, collects info only but leave everything identical.
     */
    private String tablePrefix;
    
    //private int tablePrefixLength; // kept for performance reasons (in sync with tablePrefix)
    
    /** use without tablePrefixing */
    public TablePrefixer() {
    }
    /** set relationships for prefixing */
    public TablePrefixer( ArrayList rel ) {
        relationships = rel;
    }
    /** produce a uniform tablePrefix based on a number.
     * the number probably corresponds to the index of a triple in a RDQL query.
     */
    public TablePrefixer(int tripleNumber) {
        this.setTablePrefixToTripleNumber(tripleNumber);
    }
    /** set a arbitrary tablePrefix */
    public TablePrefixer(String prefix) {
        this.setTablePrefix(prefix);
    }
    
    public Map getAliasMap() {
        return aliasMap;
    }
    public Map getPrefixedAliasMap() {
        return prefixedAliasMap;
    }
    
    /**
     * Retreives the prefixed table value
     * @param tableName the name of the table who's prefix is being searched for.
     * @param boundVar the variable that the table is boudn to.
     * @return the prefix value if found else null.
     */
    private String getTablePrefixValue( String tableName, String boundVar ){
        String retVal = null;
        for( int i = 0; i < relationships.size();i++ ){
            TripleRelationship tr = (TripleRelationship)relationships.get(i);
            if( tr.isPrefixed() && tr.getVar().equals(boundVar) ){
                retVal = tr.getPrefixedTable();
                break;
            }
        }
        return retVal;
    }
    /** optimization information for Prefixable objects. */
    public boolean mayChangeID() {
        return tablePrefix!=null;
    }
    
    /** 
     * Prefixes an object based on its interface declarations.
     */
    public Object prefix(Object obj) {
        Object retVal = null;
        if (obj instanceof Prefixable){
             retVal = prefixPrefixable((Prefixable)obj);
        } else if (obj instanceof Collection){
            retVal = prefixCollection((Collection)obj);
        } else {
            Logger.instance().error("Object: " + obj.toString() + " is Not Prefixable!");
        }
        return retVal;
    }
    
    /**
     * Prefixes a table if it hasn't been prefixed before, if it has, 
     * return the prefixed value of the original prefixing. 
     * @param tableName the name of the table to be prefixed
     * @param boundVar the variable that the table is bound to (can be "")
     * @return the prefixed table string
     */
    public String prefixTable(String tableName, String boundVar){
        //this.addPossibleColumnsToRelationships(tableName, columns);
        String prefixedTable = this.getTablePrefixValue( tableName, boundVar );
        if( null == prefixedTable ){
            prefixedTable = prefixAndReferTable( tableName );
            for( int i = 0;i<relationships.size();i++){
                TripleRelationship tr = (TripleRelationship)relationships.get(i);
                if( tr.getVar().equals(boundVar) ){
                    tr.setPrefixedTable(prefixedTable);
                    relationships.set( i, tr );
                    break;
                }
            }
        }
        return prefixedTable;
    }
    
    /** 
     * Prefixes a table name and makes sure a FROM-Term exists.
     * @return tableName resp. its substitution
     */
    
    public String prefixAndReferTable(String tableName) {
        // figure out tableName version including alias and prefix information
        String prefixedTable = null;
        
        if (tablePrefix==null) {
            referedTables.add(tableName);
            return tableName;
        }
        Alias mapVal=null;
        String dbTable=tableName; // name of table in DB
        prefixedTable=tableName;
        if (aliasMap!=null){
            mapVal = (Alias)aliasMap.get(tableName);
        }
        boolean isAlias=(mapVal!=null);
        if (isAlias){
            dbTable=mapVal.databaseTable();
        }
        boolean newAlias=false;
        if (tablePrefix!=null) {
            prefixedTable=prefixString(tableName);
            isAlias=true;
            newAlias=true;
        }
        prefixedTable = prefixedTable.replace('.', 'P');
        if( prefixedTable.length() > 30 ){
            prefixedTable= prefixedTable.substring(0,29);
        }
        referedTables.add(prefixedTable);	
        if (newAlias){
            prefixedAliasMap.put(prefixedTable,new Alias(dbTable,prefixedTable));
        }
        
        return prefixedTable;
    }
    
    // construct a collection instance of same class as collection
    public Collection prefixCollection(Collection collection) {
        return prefixCollectionAndMap(collection,null);
    }
    
    /**
     * puts previous and result into map while prefixing a collection.
     */ 
    public Collection prefixCollectionAndMap(Collection collection, Map map) {
        if (tablePrefix==null) {
            return prefixCollectionIntoCollectionAndMap(collection, null, map);
        }
        Collection inst=newEmptyCollection(collection);
        return prefixCollectionIntoCollectionAndMap(collection,(Collection)inst,map);		
    }		
    
    /** 
     * Iterates over a collection and stores the results into both a collection
     * and a map (unprefixed -> prefixed).
     * Helper method. Both results and map may be null
     * 
     * @param collection input collection
     * @param results store collection
     * @param map store map
     * @return <code>results</code>
     */
    public Collection prefixCollectionIntoCollectionAndMap(Collection collection, Collection results, Map map) {
        Iterator it=collection.iterator();
        while (it.hasNext()){
            Object entry=it.next();
            Object result=prefix(entry);
            if (results!=null)
                results.add(result);
            if (map!=null)
                map.put(entry,result);
        }
        return results;
    }
    
    public Collection prefixCollection(Collection collection, String boundVar ) {
        Iterator it=collection.iterator();
        Collection retVal = TablePrefixer.newEmptyCollection( collection );
        while (it.hasNext()){
            Object result = it.next();
            if( result instanceof Prefixable ){
                result=prefixPrefixable((Prefixable)result, boundVar);
            }
            if (retVal!=null){
                retVal.add(result);
            }
        }
        return retVal;
    }
    
    public Set prefixConditions(Set conditions) {
        if (tablePrefix==null) {
            return conditions;
        }
        Set results=new HashSet();
        Iterator it=conditions.iterator();
        while (it.hasNext()){
            String condition=(String)it.next();
            results.add(this.replaceTablesInExpression(condition));
        }
        return results;
    }
    
    /**
     * this is used in handling NodeMaker and ValueSource classes
     * some of which do not implement Prefixable
     */
    public Object prefixIfPrefixable(Object obj ) {
        if (obj instanceof Prefixable){
            return prefixPrefixable((Prefixable)obj);
        }
        return obj;
    }
    
    public NodeMaker prefixNodeMaker(NodeMaker obj, String boundVar ) {
        if( obj instanceof Prefixable ){
            return (NodeMaker)prefixPrefixable((Prefixable)obj, boundVar);
        }
        return obj;
    }
    
    /** 
     * Prefixes an object that adheres to {@link Prefixable} interface.
     */
    private Object prefixPrefixable(Prefixable obj) {
        if (tablePrefix==null) {
            obj.prefixTables(this);
            return obj;
        }
        Prefixable clon=null;
        Exception x=null;
        try {
            clon=(Prefixable)obj.clone();
        } catch (Exception e) {
            x=e;
        }
        if (x==null) {
            clon.prefixTables(this);
            return clon;        
        } else
            throw new RuntimeException(x);
    }
    
    /** 
     * Prefixes an object that adheres to {@link Prefixable} interface.
     */
    public Object prefixPrefixable(Prefixable obj, String boundVar ) {
        if (tablePrefix==null) {
            obj.prefixTables(this);
            return obj;
        }
        Prefixable clon=null;
        Exception x=null;
        try {
            clon=(Prefixable)obj.clone();
        } catch (Exception e) {
            x=e;
        }
        if (x==null) {
            clon.prefixTables(this, boundVar);
            return clon;        
        } else
            throw new RuntimeException(x);
    }
    
    
    /**
     * Creates a new HashSet of prefixed values.
     */ 
    public Set prefixSet(Set collection) {
        if (tablePrefix==null) {
            prefixCollectionIntoCollectionAndMap(collection,null,null);
            return collection;
        }
        return (Set)prefixCollectionIntoCollectionAndMap(collection,new HashSet(collection.size()),null);
    }
    
    /** 
     * Actual prefixing method with some plausibility checks.
     * @param table
     * @return prefixed string
     */
    public String prefixString(String table) {
        if (tablePrefix==null)
            return table;
        else if (mayPrefixPrefixedString || !table.startsWith(tablePrefix))
            return tablePrefix + table;
        else {
            String msg="String "+table+" already prefixed and TablePrefixer.mayPrefixPrefixedString is false.";
            if ((prefixStringContinuation & 2) != 0)
                System.out.println(msg);
            if ((prefixStringContinuation & 4) != 0)	
                throw new RuntimeException(msg);
            if ((prefixStringContinuation & 1) != 0)	
                return tablePrefix + table;
            return table; // case wrongContinuation==0
        }
    }
	public String unprefixString(String table) {
		if (tablePrefix==null)
			return table;
		if (table.startsWith(tablePrefix))
			return table.substring(tablePrefix.length());
		return null;
	}
    
    /** 
     * Tries to prefix table names in <code>expression</code>.
     * Issues: The code may fail, if there are strings in the expressions
     * that match with table names. Generally we would be better off,
     * to have not unparsed expressions floating around.
     * @param expression an SQL expression like "Tab.Col = 2"
     * @return a renamed expression
     */
    private String replaceTablesInExpression(String expression) {
        // TODO parse expression. Option: use mmbase.org code
        if (tablePrefix==null)
            return expression;
        int expressionLength=expression.length();
        java.util.regex.Matcher m = allowedTablePattern.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int start=m.start();
            String alphanum=m.group();
            boolean prevIsDot = (start>0) && ('.' == expression.charAt(start-1));
            if (!prevIsDot) {
                int end=m.end();
                boolean nextIsDot = (end<expressionLength) && ('.' == expression.charAt(end));
                if (nextIsDot) 
                    alphanum=substituteIfTable(alphanum);
            } 
            m.appendReplacement(sb, alphanum);				
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    public void setAliasMap(Map aliasMap) {
        this.aliasMap = aliasMap;
    }
    
    /**
     * sets up tablePrefix including prefixSeparator and initializes prefixedAliasMap.
     */ 
    private void setTablePrefix(String newPrefix) {
        tablePrefix=newPrefix + prefixSeparator;
        //tablePrefixLength=tablePrefix.length();
        if (newPrefix==null){
            prefixedAliasMap=null;
        } else if( null==prefixedAliasMap ) {
            prefixedAliasMap=new HashMap(5);
        }
    }
    
    public void setTablePrefixToTripleNumber(int n) {
        setTablePrefix(triplePrefix+n);
    }
    
    /** 
     * Just substitutes argument if it is known to be a table name (seen before).
     */
    private String substituteIfTable(String identifier) {
        // figure out tableName version including alias and prefix information
        // needed in guessing if s.th. is a table in an expression (e.g. condition)
        if (tablePrefix==null)
            return identifier;
        String prefixed=prefixString(identifier);
        if (referedTables.contains(prefixed))
            return prefixed;
        return identifier;
    }
    
}