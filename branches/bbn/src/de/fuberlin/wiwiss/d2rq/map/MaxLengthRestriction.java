/*
 * $Id: MaxLengthRestriction.java,v 1.1 2006/04/12 09:53:04 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.map;

import java.util.List;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.rdql.TablePrefixer;

/**
 * Restriction which can be chained with another {@link ValueSource} to limit the
 * length of its values. This is useful because the query engine can exclude sources
 * if a value is longer.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class MaxLengthRestriction implements ValueSource, Prefixable {
	private ValueSource valueSource;
	private int maxLength;
	
	public Object clone() throws CloneNotSupportedException {return super.clone();}
	public void prefixTables(TablePrefixer prefixer) {
		valueSource=(ValueSource)prefixer.prefixIfPrefixable(valueSource);
	}
    
	/**
	 * Prefix the table with the bind variable information considered
	 * @param prefixer the table prefixer
	 * @param boundVar the varaible that the table is bound to
	 */
    public void prefixTables(TablePrefixer prefixer, String boundVar) {
        prefixTables( prefixer );
    }

	public MaxLengthRestriction(ValueSource valueSource, int maxLength) {
		this.valueSource = valueSource;
		this.maxLength = maxLength;
	}

	public boolean couldFit(String value) {
		if (value == null) {
			return true;
		}
		return value.length() <= this.maxLength && this.valueSource.couldFit(value);
	}

	public List getColumns() {
		return this.valueSource.getColumns();
	}

	public Map getColumnValues(String value) {
		return this.valueSource.getColumnValues(value);
	}

	public String getValue(String[] row, Map columnNameNumberMap) {
		return this.valueSource.getValue(row, columnNameNumberMap);
	}
}
