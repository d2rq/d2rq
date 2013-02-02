package org.d2rq.db.renamer;

import java.util.Collections;
import java.util.Map;

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
