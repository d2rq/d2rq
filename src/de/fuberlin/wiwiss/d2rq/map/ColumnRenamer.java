package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Something that can rename columns in various objects.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ColumnRenamer.java,v 1.1 2006/09/09 15:40:02 cyganiak Exp $
 */
public abstract class ColumnRenamer {
	
	/**
	 * An optimized ColumnRenamer that leaves every column unchanged
	 */
	public final static ColumnRenamer NULL = new ColumnRenamer() {
		public AliasMap applyTo(AliasMap aliases) { return aliases; }
		public Column applyTo(Column original) { return original; }
		public Expression applyTo(Expression original) { return original; }
		public Join applyTo(Join original) { return original; }
		public List applyToColumnList(List columns) { return columns; }
		public Set applyToColumnSet(Set columns) { return columns; }
		public Set applyToJoinSet(Set joins) { return joins; }
		public Map applyToMapKeys(Map mapWithColumnKeys) { return mapWithColumnKeys; }
		public Map withOriginalKeys(Map columnNamesToValues) { return columnNamesToValues; }
		public String toString() { return "ColumnRenamer.NULL"; }
	};
	
	/**
	 * Returns a new map with keys and values exchanged. Lossy if multiple
	 * keys in the original have equal values.
	 * @param m The original map
	 * @return An inverse map
	 */
	protected final static Map invertMap(Map m) {
		HashMap result = new HashMap();
		Iterator it = m.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			result.put(entry.getValue(), entry.getKey());
		}
		return result;
	}
	
	/**
	 * @param original A column
	 * @return The renamed version of that column, or the same column if the renamer
	 * 		does not apply to this argument
	 */
	public abstract Column applyTo(Column original);

	/**
	 * @param original A join
	 * @return A join with all columns renamed according to this Renamer
	 */
	public Join applyTo(Join original) {
		return original.renameColumns(this);
	}

	/**
	 * @param original An expression
	 * @return An expression with all columns renamed according to this Renamer
	 */
	public Expression applyTo(Expression original) {
		return original.renameColumns(this);
	}

	public Set applyToColumnSet(Set columns) {
		Set result = new HashSet();
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			result.add(applyTo(column));
		}
		return result;
	}
	
	public List applyToColumnList(List columns) {
		List result = new ArrayList(columns.size());
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			result.add(applyTo(column));
		}
		return result;
	}
	
	public Set applyToJoinSet(Set joins) {
		Set result = new HashSet();
		Iterator it = joins.iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			result.add(applyTo(join));
		}
		return result;
	}

	public abstract Map applyToMapKeys(Map mapWithColumnKeys);
		
	public abstract AliasMap applyTo(AliasMap aliases);
		
	public abstract Map withOriginalKeys(Map columnNamesToValues);
}
