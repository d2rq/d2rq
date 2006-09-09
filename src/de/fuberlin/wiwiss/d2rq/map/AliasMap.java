package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.D2RQException;

/**
 * A map from table names to aliases. A table must have at most one alias. Can be applied
 * to various objects and will replace all mentions of a table with its alias. For some
 * kinds of objects, the inverse operation is available as well. 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: AliasMap.java,v 1.4 2006/09/09 15:40:02 cyganiak Exp $
 */
public class AliasMap extends ColumnRenamer {
	public static final AliasMap NO_ALIASES = new AliasMap(Collections.EMPTY_MAP);
	private static final Pattern aliasPattern = 
			Pattern.compile("(.+)\\s+AS\\s+(.+)", Pattern.CASE_INSENSITIVE);
	
	/**
	 * Builds an AliasMap from a collection of "Table AS Alias" expressions.
	 * @param aliasExpressions a Collection of Strings
	 * @return a corresponding AliasMap
	 */	
	public static AliasMap buildFromSQL(Collection aliasExpressions) {
		HashMap m = new HashMap();
		Iterator it = aliasExpressions.iterator();
		while (it.hasNext()) {
			String s = (String) it.next();
			Matcher matcher = aliasPattern.matcher(s);
			if (!matcher.matches()) {
				throw new D2RQException("d2rq:alias '" + s +
						"' is not in 'table AS alias' form");
			}
			m.put(matcher.group(2), matcher.group(1));
		}
		return new AliasMap(m);
	}

	private Map aliasesToOriginals;
	private Map originalsToAliases;
	
	public AliasMap(Map aliasesToOriginals) {
		this.aliasesToOriginals = new HashMap(aliasesToOriginals);
		this.originalsToAliases = invertMap(aliasesToOriginals);
	}
	
	public boolean isAlias(String name) {
		return this.aliasesToOriginals.containsKey(name);
	}

	public boolean hasAlias(String original) {
		return this.originalsToAliases.containsKey(original);
	}
	
	public Set allAliases() {
		return this.aliasesToOriginals.keySet();
	}

	public Set allOriginalsWithAliases() {
		return this.originalsToAliases.keySet();
	}
	
	public String applyToTableName(String original) {
		if (!hasAlias(original)) {
			return original;
		}
		return (String) this.originalsToAliases.get(original);
	}
	
	public String originalOf(String name) {
		if (!isAlias(name)) {
			return name;
		}
		return (String) this.aliasesToOriginals.get(name);
	}
	
	public Column applyTo(Column column) {
		if (!hasAlias(column.getTableName())) {
			return column;
		}
		return new Column(applyToTableName(column.getTableName()), column.getColumnName());
	}
	
	public Column originalOf(Column column) {
		if (!isAlias(column.getTableName())) {
			return column;
		}
		return new Column(originalOf(column.getTableName()), column.getColumnName());
	}
	
	public Join applyTo(Join join) {
		if (!hasAlias(join.getFirstTable()) && !hasAlias(join.getSecondTable())) {
			return join;
		}
		return super.applyTo(join);
	}

	public AliasMap applyTo(AliasMap other) {
		if (other == null) {
			return this;
		}
		Map newAliases = new HashMap();
		Iterator it = other.allAliases().iterator();
		while (it.hasNext()) {
			String alias = (String) it.next();
			newAliases.put(applyToTableName(alias), other.originalOf(alias));
		}
		it = allAliases().iterator();
		while (it.hasNext()) {
			String alias = (String) it.next();
			newAliases.put(alias, originalOf(alias));
		}
		return new AliasMap(newAliases);
	}
	
	public Map applyToMapKeys(Map mapWithColumnKeys) {
		Map result = new HashMap();
		Iterator it = mapWithColumnKeys.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			Column column = (Column) entry.getKey();
			result.put(applyTo(column), entry.getValue());
		}
		return result;
	}
	
	public Map withOriginalKeys(Map mapWithColumnNameKeys) {
		Map result = new HashMap();
		Iterator it = mapWithColumnNameKeys.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			Column column = new Column((String) entry.getKey());
			if (hasAlias(column.getTableName())) {
				// We are overlaying this table with the back-translated alias
				continue;
			}
			result.put(originalOf(column).getQualifiedName(), entry.getValue());
		}
		return result;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof AliasMap)) {
			return false;
		}
		AliasMap otherAliasMap = (AliasMap) other;
		return this.originalsToAliases.equals(otherAliasMap.originalsToAliases);
	}
	
	public int hashCode() {
		return this.originalsToAliases.hashCode();
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("AliasMap(");
		List tables = new ArrayList(this.originalsToAliases.keySet());
		Collections.sort(tables);
		Iterator it = tables.iterator();
		while (it.hasNext()) {
			String table = (String) it.next();
			result.append(table);
			result.append(" AS ");
			result.append(this.originalsToAliases.get(table));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}
}
