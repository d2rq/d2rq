/*
 * $Id: RegexRestriction.java,v 1.2 2005/04/13 17:17:42 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.map;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.rdql.TablePrefixer;

/**
 * Restriction which can be chained with another {@link ValueSource} to state
 * that all its values match a certain regular expression. This is useful because the
 * query engine can exclude sources if a value doesn't match the expression.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class RegexRestriction implements ValueSource, Prefixable {
	private ValueSource valueSource;
	private Pattern regex;

	public Object clone() throws CloneNotSupportedException {return super.clone();}
	public void prefixTables(TablePrefixer prefixer) {
		valueSource=prefixer.prefixValueSource(valueSource);
	}

	public RegexRestriction(ValueSource valueSource, String regex) {
		this.valueSource = valueSource;
		this.regex = Pattern.compile(regex);
	}

	public boolean couldFit(String value) {
		if (value == null) {
			return true;
		}
		return this.regex.matcher(value).matches() && this.valueSource.couldFit(value);
	}

	public Set getColumns() {
		return this.valueSource.getColumns();
	}

	public Map getColumnValues(String value) {
		return this.valueSource.getColumnValues(value);
	}

	public String getValue(String[] row, Map columnNameNumberMap) {
		return this.valueSource.getValue(row, columnNameNumberMap);
	}
}
