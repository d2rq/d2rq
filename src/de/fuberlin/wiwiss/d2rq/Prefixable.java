/*
 * Created on Feb 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.fuberlin.wiwiss.d2rq;

/**
 * @author jg
 *
 * Classes that implement this interface do therby allow for a combination of
 * c=obj.clone(); 
 * c.prefixTables(prefixer) 
 */
public interface Prefixable extends Cloneable {
	//	 do not overwrite clone()! prefixTables works best with shallow copy 
	//   (which Object implements) .
	public Object clone() throws CloneNotSupportedException;
	void prefixTables(TablePrefixer prefixer);	
}
