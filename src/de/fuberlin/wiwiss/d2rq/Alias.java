/*
 * $Id: Alias.java,v 1.3 2005/03/07 17:38:44 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Represents an SQL join between two tables, spanning one or more columns.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */

class Alias {
	private final String databaseTable;
	private final String aliasTable;
	private final String sqlExpression;
	
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

	public static String asConstructor = " AS ";
	
	public static String sqlExpression(String databaseTable, String aliasTable) { 
		return databaseTable + asConstructor + aliasTable;
	}


	// TODO: use PatternMatcher
	private static Alias buildAlias(String aliasExpression) { 
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
	 * Builds a Map of Alias objects from a list of alias statement
	 * strings.
	 * @param joinConditions a collection of strings
	 * @return a set of Join instances
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
