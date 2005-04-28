/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.map.Alias;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.map.NodeMaker;
import de.fuberlin.wiwiss.d2rq.map.Prefixable;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;
import de.fuberlin.wiwiss.d2rq.map.ValueSource;

/** 
 * A class for collecting database table information and for creating table aliases.
 *  
 * @author jgarbers
 *
 */
public class TablePrefixer {
    /** 
     * The common prefix to use in aliasing.
     * If null, collects info only but leave everything identical.
     */
	protected String tablePrefix; 
	protected int tablePrefixLength; // kept for performance reasons (in sync with tablePrefix)
	protected static final String prefixSeparator = "_";
	protected static final String triplePrefix = "T";
	
	// prefixing and getting the table references and aliases right 
	// see SQLStatementMaker.sqlFromExpression(referredTables,aliasMap)
	/** given alias map from String (aliased Table) to Alias */
	protected Map aliasMap; 
	/** Database table names (Strings) in their alias form. */
	protected Set referedTables = new HashSet(5); 
	/** new aliases. created during prefixing */
	protected Map prefixedAliasMap; 
	
	
	/** use without tablePrefixing */
	public TablePrefixer() {
	}
	/** set a arbitrary tablePrefix */
	public TablePrefixer(String prefix) {
		this.setTablePrefix(prefix);
	}
	/** produce a uniform tablePrefix based on a number.
	 * the number probably corresponds to the index of a triple in a RDQL query.
	 */
	public TablePrefixer(int tripleNumber) {
		this.setTablePrefixToTripleNumber(tripleNumber);
	}
	
	/** optimization information for Prefixable objects. */
	public boolean mayChangeID() {
		return tablePrefix!=null;
	}
//	public boolean didChangeID() {
//		return didChangeID;
//	}
	
	// instance variable access methods
	/**
	 * sets up tablePrefix including prefixSeparator and initializes prefixedAliasMap.
	 */ 
	public void setTablePrefix(String newPrefix) {
		tablePrefix=newPrefix + prefixSeparator;
		tablePrefixLength=tablePrefix.length();
		if (newPrefix==null)
			prefixedAliasMap=null;
		else
			prefixedAliasMap=new HashMap(5);
	}
	public void setTablePrefixToTripleNumber(int n) {
		setTablePrefix(triplePrefix+n);
	}
	public Map getAliasMap() {
		return aliasMap;
	}
	public void setAliasMap(Map aliasMap) {
		this.aliasMap = aliasMap;
	}
	public Set getReferedTables() {
		return referedTables;
	}
	public void setReferedTables(Set referedTables) {
		this.referedTables = referedTables;
	}
	
	public Map getPrefixedAliasMap() {
		return prefixedAliasMap;
	}
	
	//  prefixing methods

	public static boolean mayPrefixPrefixedString=true; // set in runtime variables to false during testing!
	public static int prefixStringContinuation=2; // see 
	
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
			return table.substring(tablePrefixLength);
		return null;
	}
	/** 
	 * Extracts keys matching this tablePrefix and renames them.
	 * @param columnNameNumber keys are prefixed.
	 * @return a new map
	 */
	public Map unprefixedColumnNameNumberMap(Map columnNameNumber) {
		if (tablePrefix==null)
			return columnNameNumber;
		Map result=new HashMap(10);
		Iterator keys=columnNameNumber.keySet().iterator();
		while (keys.hasNext()) {
			String prefixedCol=(String)keys.next();
			String unprefixed=unprefixString(prefixedCol);
			if (unprefixed!=null) {
				Object val=columnNameNumber.get(prefixedCol);
				result.put(unprefixed,val);
			}
		}
		return result;
	}				
	
	protected static java.util.regex.Pattern allowedTablePattern = java.util.regex.Pattern.compile("\\w+");

	/** 
	 * Tries to prefix table names in <code>expression</code>.
	 * Issues: The code may fail, if there are strings in the expressions
	 * that match with table names. Generally we would be better off,
	 * to have not unparsed expressions floating around.
	 * @param expression an SQL expression like "Tab.Col = 2"
	 * @return a renamed expression
	 */
	protected String replaceTablesInExpression(String expression) {
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
	
	
	// methods for prefixing and getting the table references and aliases right 
	
	/** 
	 * Just substitutes argument if it is known to be a table name (seen before).
	 */
	protected String substituteIfTable(String identifier) {
		// figure out tableName version including alias and prefix information
		// needed in guessing if s.th. is a table in an expression (e.g. condition)
		if (tablePrefix==null)
			return identifier;
		String prefixed=prefixString(identifier);
		if (referedTables.contains(prefixed))
			return prefixed;
		return identifier;
	}		
		
	/** 
	 * Prefixes a table name and makes sure a FROM-Term exists.
	 * @return tableName resp. its substitution
	 */
	protected String prefixAndReferTable(String tableName) {
		// figure out tableName version including alias and prefix information
		if (tablePrefix==null) {
			referedTables.add(tableName);
			return tableName;
		}
		Alias mapVal=null;
		String dbTable=tableName; // name of table in DB
		String prefixedTable=tableName;
		if (aliasMap!=null)
			mapVal = (Alias)aliasMap.get(tableName);
		boolean isAlias=(mapVal!=null);
		if (isAlias)
			dbTable=mapVal.databaseTable();
		boolean newAlias=false;
		if (tablePrefix!=null) {
			prefixedTable=prefixString(tableName);
			isAlias=true;
			newAlias=true;
		}
		referedTables.add(prefixedTable);
		if (newAlias)
			prefixedAliasMap.put(prefixedTable,new Alias(dbTable,prefixedTable));
		
		return prefixedTable;
	}
		
	/**
	 * this is used in handling NodeMaker and ValueSource classes
	 * some of which do not implement Prefixable
	 */
	public Object prefixIfPrefixable(Object obj) {
		if (obj instanceof Prefixable)
			return prefixPrefixable((Prefixable)obj);
		return obj;
	}
	
	//////////////////////////////////
	// correctly typed methods grouped with their prefix() variant
	// prefix() is a polymorph version of less polymorph methods
	// that is needed for uniform collection handling

	/** 
	 * Prefixes an object based on its interface declarations.
	 */
	public Object prefix(Object obj) {
		if (obj instanceof Prefixable) 
			return prefixPrefixable((Prefixable) obj);
		if (obj instanceof NodeMaker)  
			return prefixIfPrefixable((NodeMaker)obj);
		if (obj instanceof ValueSource)
			return prefixIfPrefixable((ValueSource)obj);
		if (obj instanceof Collection)
			return prefixCollection((Collection)obj);
		throw new RuntimeException("unrecognized argument " + obj.toString() + "to TablePrefixer.prefix().");
	}
	
	/** 
	 * Prefixes an object that adheres to {@link Prefixable} interface.
	 */
	public Prefixable prefixPrefixable(Prefixable obj) {
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
	
	public NodeMaker prefixNodeMaker(NodeMaker obj) {
		return (NodeMaker)prefixIfPrefixable(obj);
	}
	
	public ValueSource prefixValueSource(ValueSource obj) {
		return (ValueSource)prefixIfPrefixable(obj);
	}
	
	////////////////////////////////////////
	// handling Collections
	////////////////////////////////////////
	
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
	
	private static Class[] intParameterTypes=new Class[]{int.class};
	private static Class[] noneParameterTypes=new Class[]{};

	public Object prefix(Collection obj) {
		return prefixCollection(obj);
	}
	// construct a collection instance of same class as collection
	public Collection prefixCollection(Collection collection) {
		return prefixCollectionAndMap(collection,null);
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
	
	/**
	 * puts previous and result into map while prefixing a collection.
	 */ 
	public Collection prefixCollectionAndMap(Collection collection, Map map) {
		if (tablePrefix==null) {
			prefixCollectionIntoCollectionAndMap(collection, null, map);
			return collection;
		}
		Collection inst=newEmptyCollection(collection);
		return prefixCollectionIntoCollectionAndMap(collection,(Collection)inst,map);		
	}

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
	 * Creates a new HashSet of prefixed values.
	 */ 
	public Set prefixSet(Set collection) {
		if (tablePrefix==null) {
			prefixCollectionIntoCollectionAndMap(collection,null,null);
			return collection;
		}
		return (Set)prefixCollectionIntoCollectionAndMap(collection,new HashSet(collection.size()),null);
	}

	/** Strings are assumed to be tables. */
	public Object prefix(String obj) {
	    // TODO better check, if there is a Dot within?
		return prefixTable(obj);
	}
	/** Tables are refered to and prefixed */
	public String prefixTable(String table) {
		return prefixAndReferTable(table);
	}

	public Object prefix(Column obj) {
		return prefixPrefixable(obj);
	}
	// return column resp. its substitution and make sure a FROM-Term exists
	public Column prefixColumn(Column column) {
		return (Column)prefixPrefixable(column);
	}
	public Object prefix(Join obj) {
		return prefixPrefixable(obj);
	}
	public Join prefixJoin(Join join) {
		return (Join)prefixPrefixable(join);
	}
	public Object prefix(PropertyBridge obj) {
		return prefixPrefixable(obj);
	}
	public PropertyBridge prefixPropertyBridge(PropertyBridge propertyBridge) {
		return (PropertyBridge)prefixPrefixable(propertyBridge);
	}

	// convenience Methods (remove!)
	
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

	
	// public ArrayList
	
	public Map prefixColumnColumnMap(Map map) {
		Map results= (tablePrefix==null) ? map : new HashMap();
		Iterator it=map.keySet().iterator();
		while (it.hasNext()){
			Column fromColumn=(Column)it.next();
			Column toColumn=(Column)map.get(fromColumn);
			Column fromP=prefixColumn(fromColumn);
			Column toP=prefixColumn(toColumn);
			if (tablePrefix!=null)
				results.put(fromP, toP);
		}
		return results;
	}	

	

}