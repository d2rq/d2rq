/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.map;

import de.fuberlin.wiwiss.d2rq.rdql.TablePrefixer;

/**
 * Implementing instances provide a uniform way of creating a copy of themselves
 * in which physical database table names are replaced by aliases.
 * Classes that implement this interface are called with a combination of
 * c=obj.clone(); 
 * c.prefixTables(prefixer) 
 *
 * @author jg
 * @see TablePrefixer
 */
public interface Prefixable extends Cloneable {
	/**	 
	 * Make a shallow copy of <code>this</code>.
	 * General advice: Do not overwrite clone(). 
	 * prefixTables() works best with shallow copy which Object implements.
	 * Issues: I have not found a (preferred) static equivalent of clone().
	 * Since Objects clone() method is protected, we cannot just declare 
	 * {@link Cloneable}, if we want to call clone() from outside of that
	 * class hierarchy. Instead it must be public and implemented in each
	 * Prefixable class by a call to super.clone().
	 * 
	 */
	public Object clone() throws CloneNotSupportedException;

	/**
	 * Changes the fields in a newly created clone that are to be modified
	 * when making aliasses. 
	 * 
	 * @param prefixer does the actual prefixing of simple and complex types.
	 */
	void prefixTables(TablePrefixer prefixer);
	/**
	 * Prefix the table with the bind variable information considered
	 * @param prefixer the table prefixer
	 * @param boundVar the varaible that the table is bound to
	 */
    void prefixTables(TablePrefixer prefixer, String boundVar );  
}
