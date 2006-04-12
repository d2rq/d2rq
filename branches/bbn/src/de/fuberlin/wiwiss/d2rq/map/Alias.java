/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import de.fuberlin.wiwiss.d2rq.helpers.Logger;

/**
 * An Alias represents an SQL alias for a physical database table. 
 * The <code>sqlExpression</code> is of the form <code>databaseTable AS aliasTable</code>
 * 
 * @author Joerg Garbers
 * @since 0.3
 */
public class Alias {
	private final String databaseTable;
	private final String aliasTable;
	private final String sqlExpression;
	
	/**
	 * Class constructor in memorizable <code>databaseTable AS aliasTable</code> form.
	 * @param databaseTable
	 * @param aliasTable
	 */
	public Alias (String databaseTable, String aliasTable) {
		this.databaseTable=databaseTable;
		this.aliasTable=aliasTable;
		this.sqlExpression=Alias.sqlExpression(databaseTable,aliasTable);
	}
	public String aliasTable() {
		return aliasTable;
	}
	public String databaseTable() {
		return databaseTable;
	}
	
	public String sqlExpression() { 
		return sqlExpression;
	}

	public String toString() { 
		return super.toString()+"("+sqlExpression+")";
	}
	//TODO: Make this SQL and Oracle Friendly... 
	//For use with SQL leave as is, to use with Oracle set to " "
	public static String asConstructor = " AS ";
	
	public static String sqlExpression(String databaseTable, String aliasTable) { 
		return databaseTable + asConstructor + aliasTable;
	}


	/**
	 * Builds an Alias object from a textual SQL alias expression.
	 * Currently we use simple String functions to extract the parts from an expression like
	 * "Table AS Alias". All characters are deliberately converted to upper case.
	 * @param aliasExpression the string to create the Alias from
	 * @return the created Alias instance.
	 */
	private static Alias buildAlias(String aliasExpression) { 
		// TODO: use PatternMatcher
		boolean illegal=false;
		String upper=aliasExpression.toUpperCase();
		int index = upper.indexOf(asConstructor); 
		illegal = (index == -1); 
		String table = null;
		String alias = null;
		if (!illegal) {
			table = aliasExpression.substring(0, index).trim();
			alias = aliasExpression.substring(index + 1).trim();
			illegal = (table.length()==0) || (alias.length()==0);
		}
		if (illegal) {
			Logger.instance().error("Illegal d2rq:alias: \"" + aliasExpression +
									"\" (must be in 'Table AS Alias' form)");
			return null;
		} 
		return new Alias(table,alias);
	}
	
	/**
	 * Builds a Map of Alias objects from a list of alias expressions.
	 * Each expression is converted to an Alias instance. The instance is put into
	 * the map as the value of its aliasTable name.
	 * 
	 * @param aliasStatements a Collection of strings
	 * @return the Map
	 * @see #buildAlias(String)
	 */	
	public static HashMap buildAliases(Collection aliasStatements) {
		HashMap result = new HashMap(2);
		Iterator it = aliasStatements.iterator();
		while (it.hasNext()) {
			String aliasStatement = (String) it.next();
			Alias alias=buildAlias(aliasStatement);
			String aliasTab = alias.aliasTable();
			result.put(aliasTab, alias);
		}
		return result;
	}
	
		
}	
