package org.d2rq.db.renamer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.d2rq.db.op.AliasOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;



/**
 * A {@link Renamer} that can be applied to various things in order to
 * substitute some tables for other tables, e.g., substitute a
 * base table by an alias.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TableRenamer extends Renamer {
	
	public static Renamer create(TableName old, TableName replacement) {
		return new TableRenamer(Collections.singletonMap(old, replacement));
	}
	
	public static Renamer create(Map<TableName,TableName> originalsToReplacements) {
		return new TableRenamer(originalsToReplacements);
	}
	
	/**
	 * Creates a renamer that substitutes originals with aliases; for example,
	 * if an alias "T1 AS A1, T2 AS A2B, A2 AS A2C" is applied to
	 * "SELECT T1.X, A2.Y, Z FROM T1, T2 AS A2", it will yield:
	 * "SELECT A1.X, A2C.Y, Z FROM T1 AS A1, T2 AS A2C". Aliases whose
	 * originals are not named have no effect.
	 */
	public static Renamer create(Collection<AliasOp> aliases) {
		Map<TableName,TableName> originalsToReplacements =
			new HashMap<TableName,TableName>();
		for (AliasOp alias: aliases) {
			if (alias.getOriginal().getTableName() == null) {
				continue;	// Original is not named 
			}
			originalsToReplacements.put(
					alias.getOriginal().getTableName(), 
					alias.getTableName());
		}
		return new TableRenamer(originalsToReplacements);
	}
	
	public static Renamer create(AliasOp alias) {
		return create(Collections.singleton(alias));
	}
	
	private final Map<TableName,TableName> fromTo;

	private TableRenamer(Map<TableName,TableName> fromTo) {
		this.fromTo = fromTo;
	}
	
	@Override
	public ColumnName applyTo(ColumnName original) {
		if (!original.isQualified()) return original;
		if (!fromTo.containsKey(original.getQualifier())) return original;
		return ColumnName.create(fromTo.get(original.getQualifier()), original.getColumn());
	}

	@Override
	public TableName applyTo(TableName original) {
		return fromTo.containsKey(original) ? fromTo.get(original) : original; 
	}

	public String toString() {
		return "TableRenamer(" + fromTo + ")";
	}
}
