package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
 * TODO: Reframe from "Map" to "Collection of Aliases"
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: AliasMap.java,v 1.3 2006/09/15 12:25:25 cyganiak Exp $
 */
public class AliasMap extends ColumnRenamer {
	public static final AliasMap NO_ALIASES = new AliasMap(Collections.EMPTY_SET);
	private static final Pattern aliasPattern = 
			Pattern.compile("(.+)\\s+AS\\s+(.+)", Pattern.CASE_INSENSITIVE);
	
	public static class Alias {
		private RelationName original;
		private RelationName alias;
		public Alias(RelationName original, RelationName alias) {
			this.original = original;
			this.alias = alias;
		}
		public RelationName original() {
			return this.original;
		}
		public RelationName alias() {
			return this.alias;
		}
		public int hashCode() {
			return this.original.hashCode() ^ this.alias.hashCode();
		}
		public boolean equals(Object o) {
			if (!(o instanceof Alias)) return false;
			return this.alias.equals(((Alias) o).alias) && this.original.equals(((Alias) o).original);
		}
		public String toString() {
			return this.original + " AS " + this.alias;
		}
	}
	
	public static Alias buildAlias(String aliasExpression) {
		Matcher matcher = aliasPattern.matcher(aliasExpression);
		if (!matcher.matches()) {
			throw new D2RQException("d2rq:alias '" + aliasExpression +
					"' is not in 'table AS alias' form");
		}
		return new Alias(new RelationName(matcher.group(1)), new RelationName(matcher.group(2)));
	}
	
	/**
	 * Builds an AliasMap from a collection of "Table AS Alias" expressions.
	 * @param aliasExpressions a Collection of Strings
	 * @return a corresponding AliasMap
	 */	
	public static AliasMap buildFromSQL(Collection aliasExpressions) {
		Collection aliases = new HashSet();
		Iterator it = aliasExpressions.iterator();
		while (it.hasNext()) {
			aliases.add(buildAlias((String) it.next()));
		}
		return new AliasMap(aliases);
	}

	private Map aliasesToOriginals;
	private Map originalsToAliases;
	
	// TODO: Kill this constructor
	public AliasMap(Map aliasesToOriginals) {
		this.aliasesToOriginals = new HashMap(aliasesToOriginals);
		this.originalsToAliases = invertMap(aliasesToOriginals);
	}
	
	public AliasMap(Collection aliases) {
		this.aliasesToOriginals = new HashMap();
		this.originalsToAliases = new HashMap();
		Iterator it = aliases.iterator();
		while (it.hasNext()) {
			Alias alias = (Alias) it.next();
			this.aliasesToOriginals.put(alias.alias(), alias.original());
			this.originalsToAliases.put(alias.original(), alias.alias());
		}
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
	
	public Alias originalOf(Alias alias) {
		if (!isAlias(alias.original())) {
			return alias;
		}
		// For some weird reason, substitute only the original side
		return new Alias(originalOf(alias.original()), alias.alias());
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
		return this.aliasesToOriginals.equals(otherAliasMap.aliasesToOriginals);
	}
	
	public int hashCode() {
		return this.aliasesToOriginals.hashCode();
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("AliasMap(");
		List tables = new ArrayList(this.aliasesToOriginals.keySet());
		Collections.sort(tables);
		Iterator it = tables.iterator();
		while (it.hasNext()) {
			RelationName alias = (RelationName) it.next();
			result.append(this.aliasesToOriginals.get(alias));
			result.append(" AS ");
			result.append(alias);
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}
}
