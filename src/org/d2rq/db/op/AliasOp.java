package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;


/**
 * FIXME: Handle column name clashes like TABLE1.COL, TABLE2.COL which have to be made unique (e.g., ALIAS.TABLE1_COL, ALIAS.TABLE2_COL)
 */
public class AliasOp extends NamedOp {
	private final Identifier name;
	private final DatabaseOp original;
	private final ColumnList columns;
	private final Collection<ColumnList> uniqueKeys = new ArrayList<ColumnList>();
	
	public static AliasOp create(DatabaseOp original, TableName alias) {
		return new AliasOp(new OpVisitor.Default(false) {
			private DatabaseOp result;
			public boolean visitEnter(AliasOp table) {
				result = table.getOriginal();
				return true;
			}
			DatabaseOp removeTopLevelAliases(DatabaseOp op) {
				result = op;
				op.accept(this);
				return result;
			}
		}.removeTopLevelAliases(original), alias);
	}
	
	public static AliasOp create(DatabaseOp original, String alias) {
		return AliasOp.create(original, 
				TableName.create(null, null, Identifier.createUndelimited(alias)));
	}
	
	/**
	 * Generates an alias for a given tabular with a unique name.
	 * 
	 * @param original The {@link DatabaseOp} to be wrapped
	 * @param baseName A base name to be prepended to the unique name for readability
	 * @return An alias with a unique name (e.g., "BASENAME1234")
	 */
	public static AliasOp createWithUniqueName(DatabaseOp original, String baseName) {
		return create(original, 
				baseName + Integer.toHexString(original.hashCode()).toUpperCase());
	}
	
	private AliasOp(DatabaseOp original, TableName alias) {
		super(alias);
		if (alias.getSchema() != null || alias.getCatalog() != null) {
			throw new IllegalArgumentException("Alias name cannot be qualified: " + 
					alias + " (for " + original + ")");
		}
		this.name = alias.getTable();
		while (original instanceof AliasOp) {
			original = ((AliasOp) original).getOriginal();
		}
		this.original = original;
		List<ColumnName> aliasedColumns = new ArrayList<ColumnName>();
		for (ColumnName column: original.getColumns()) {
			aliasedColumns.add(getTableName().qualifyColumn(column));
		}
		columns = ColumnList.create(aliasedColumns);
		for (ColumnList key: original.getUniqueKeys()) {
			aliasedColumns.clear();
			for (ColumnName column: key) {
				aliasedColumns.add(getTableName().qualifyColumn(column));
			}
			uniqueKeys.add(ColumnList.create(aliasedColumns));
		}
	}
	
	/**
	 * Guaranteed not to be another {@link AliasOp}.
	 */
	public DatabaseOp getOriginal() {
		return this.original;
	}
	
	public ColumnName getOriginalColumnName(ColumnName aliasedName) {
		if (!hasColumn(aliasedName)) return null;
		return ColumnName.create(original.getTableName(), aliasedName.getColumn());
	}
	
	/**
	 * Returns a {@link Renamer} that replaces references in the original
	 * {@link DatabaseOp} with the aliased versions of these references.
	 */
	public Renamer getRenamer() {
		// FIXME: This just ignores the old column name and uses the alias name instead. That works only because we don't handle column clashes.
		return new Renamer() {
			@Override
			public ColumnName applyTo(ColumnName original) {
				return getTableName().qualifyColumn(original);
			}
			@Override
			public TableName applyTo(TableName original) {
				return getTableName();
			}
		};
	}
	
	public ColumnList getColumns() {
		return columns;
	}

	public boolean isNullable(ColumnName column) {
		if (!hasColumn(column)) return false; // can't return null...
		return original.isNullable(ColumnName.create(column.getColumn()));
	}

	public DataType getColumnType(ColumnName column) {
		if (!hasColumn(column)) return null;
		return original.getColumnType(ColumnName.create(column.getColumn()));
	}

	public Collection<ColumnList> getUniqueKeys() {
		return uniqueKeys;
	}

	public void accept(OpVisitor visitor) {
		if (visitor.visitEnter(this)) {
			original.accept(visitor);
		}
		visitor.visitLeave(this);
	}
	
	@Override
	public String toString() {
		return "Alias(" + original + " AS " + name + ")";
	}
	
	@Override
	public int hashCode() {
		return original.hashCode() ^ name.hashCode() ^ 542;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AliasOp)) return false;
		return name.equals(((AliasOp) o).name) && original.equals(((AliasOp) o).original);
	}
}
