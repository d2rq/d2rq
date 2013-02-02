package org.d2rq.db.op;

import java.util.Collection;
import java.util.Collections;

import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;


/**
 * An operator or operand in a relational algebra expression 
 * over SQL-style tables.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface DatabaseOp {

	/**
	 * @return Name in [[CATALOG.]SCHEMA.]TABLE notation, possibly <code>null</code>
	 */
	TableName getTableName();

	/**
	 * @param column A qualified or unqualified column name
	 * @return <code>false</code> for ambiguous unqualified names
	 */
	boolean hasColumn(ColumnName column);
	
	/**
	 * @return Fully qualified column names if possible, no duplicates
	 */
	ColumnList getColumns();

	/**
	 * Indicates whether a column may contain <code>NULL</code> values.
	 * @param column A qualified or unqualified column name
	 * @return <code>false</code> for non-existing columns or ambiguous unqualified names
	 */
	boolean isNullable(ColumnName column);

	/**
	 * Indicates the datatype of a column.
	 * @param column A qualified or unqualified column name
	 * @return <code>null</code> for non-existing columns or ambiguous unqualified names
	 */
	DataType getColumnType(ColumnName column);

	/**
	 * A unique key is a list of columns whose combined values are guaranteed
	 * to be unique within the table.
	 */
	Collection<ColumnList> getUniqueKeys();
	
	void accept(OpVisitor visitor);

	/**
	 * A table with one row and no columns. Like Oracle's <code>DUAL</code>
	 * table. The identity element of the relational cross product operation.
	 * In many databases, this table can be accessed by simply omitting
	 * the <code>FROM</code> clause in a <code>SELECT</code> statement. 
	 */
	public DatabaseOp TRUE = new DatabaseOp() {
		public TableName getTableName() {
			return null;
		}
		public boolean hasColumn(ColumnName column) {
			return false;
		}
		public ColumnList getColumns() {
			return ColumnList.EMPTY;
		}
		public boolean isNullable(ColumnName column) {
			return false;
		}
		public DataType getColumnType(ColumnName column) {
			return null;
		}
		public Collection<ColumnList> getUniqueKeys() {
			return Collections.emptySet();
		}
		public void accept(OpVisitor visitor) {
			visitor.visitOpTrue();
		}
		public String toString() {
			return "[1ROW]";
		}
	};

	public abstract static class Wrapper implements DatabaseOp {
		private final DatabaseOp wrapped;
		public Wrapper(DatabaseOp wrapped) {
			this.wrapped = wrapped;
		}
		public DatabaseOp getWrapped() {
			return wrapped;
		}
		public TableName getTableName() {
			return wrapped.getTableName();
		}
		public boolean hasColumn(ColumnName column) {
			return wrapped.hasColumn(column);
		}
		public ColumnList getColumns() {
			return wrapped.getColumns();
		}
		public boolean isNullable(ColumnName column) {
			return wrapped.isNullable(column);
		}
		public DataType getColumnType(ColumnName column) {
			return wrapped.getColumnType(column);
		}
		public Collection<ColumnList> getUniqueKeys() {
			return wrapped.getUniqueKeys();
		}
	}
}
