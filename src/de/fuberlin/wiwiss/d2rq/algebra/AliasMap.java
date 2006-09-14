package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * @version $Id: AliasMap.java,v 1.2 2006/09/14 16:22:48 cyganiak Exp $
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
			m.put(new RelationName(matcher.group(2)),
					new RelationName(matcher.group(1)));
		}
		return new AliasMap(m);
	}

	private Map aliasesToOriginals;
	private Map originalsToAliases;
	
	public AliasMap(Map aliasesToOriginals) {
		this.aliasesToOriginals = new HashMap(aliasesToOriginals);
		this.originalsToAliases = invertMap(aliasesToOriginals);
	}
	
	public boolean isAlias(RelationName name) {
		return this.aliasesToOriginals.containsKey(name);
	}

	public boolean hasAlias(RelationName original) {
		return this.originalsToAliases.containsKey(original);
	}
	
	public RelationName applyTo(RelationName original) {
		if (!hasAlias(original)) {
			return original;
		}
		return (RelationName) this.originalsToAliases.get(original);
	}
	
	public RelationName originalOf(RelationName name) {
		if (!isAlias(name)) {
			return name;
		}
		return (RelationName) this.aliasesToOriginals.get(name);
	}
	
	public Attribute applyTo(Attribute column) {
		if (!hasAlias(column.relationName())) {
			return column;
		}
		return new Attribute(applyTo(column.relationName()) + "." + column.attributeName());
	}
	
	public Attribute originalOf(Attribute column) {
		if (!isAlias(column.relationName())) {
			return column;
		}
		return new Attribute(originalOf(column.relationName()) + "." + column.attributeName());
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
		Iterator it = other.aliasesToOriginals.keySet().iterator();
		while (it.hasNext()) {
			RelationName alias = (RelationName) it.next();
			newAliases.put(applyTo(alias), other.originalOf(alias));
		}
		it = this.aliasesToOriginals.keySet().iterator();
		while (it.hasNext()) {
			RelationName alias = (RelationName) it.next();
			newAliases.put(alias, originalOf(alias));
		}
		return new AliasMap(newAliases);
	}
	
	public Map applyToMapKeys(Map mapWithAttributeKeys) {
		Map result = new HashMap();
		Iterator it = mapWithAttributeKeys.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			Attribute column = (Attribute) entry.getKey();
			result.put(applyTo(column), entry.getValue());
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
			RelationName table = (RelationName) it.next();
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
