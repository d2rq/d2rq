package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import de.fuberlin.wiwiss.d2rq.expr.Expression;

/**
 * Something that can rename columns in various objects.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class ColumnRenamer {
	
	/**
	 * An optimized ColumnRenamer that leaves every column unchanged
	 */
	public final static ColumnRenamer NULL = new ColumnRenamer() {
		public AliasMap applyTo(AliasMap aliases) { return aliases; }
		public Attribute applyTo(Attribute original) { return original; }
		public Expression applyTo(Expression original) { return original; }
		public Join applyTo(Join original) { return original; }
		public Set applyToJoinSet(Set joins) { return joins; }
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
	public abstract Attribute applyTo(Attribute original);

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
		return original.renameAttributes(this);
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

	public ProjectionSpec applyTo(ProjectionSpec original) {
		return original.renameAttributes(this);
	}
	
	public Set applyToProjectionSet(Set projections) {
		Set result = new HashSet();
		Iterator it = projections.iterator();
		while (it.hasNext()) {
			ProjectionSpec projection = (ProjectionSpec) it.next();
			result.add(applyTo(projection));
		}
		return result;
	}
	
	public abstract AliasMap applyTo(AliasMap aliases);
}
