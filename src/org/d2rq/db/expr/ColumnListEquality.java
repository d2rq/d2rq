package org.d2rq.db.expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.IdentifierList;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;



/**
 * Compares two lists of columns in two tables. Used as a convenient way
 * of representing join conditions in SQL joins.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ColumnListEquality extends Expression {
	
	public static ColumnListEquality create(ColumnName oneSide, ColumnName otherSide) {
		return new ColumnListEquality(oneSide.getQualifier(), IdentifierList.create(oneSide), 
				otherSide.getQualifier(), IdentifierList.create(otherSide));
	}
	
	public static ColumnListEquality create(TableName oneTable, IdentifierList oneColumnList,
			TableName otherTable, IdentifierList otherColumnList) {
		return new ColumnListEquality(oneTable, oneColumnList, otherTable, otherColumnList);
	}
	
	private final IdentifierList columns1;
	private final IdentifierList columns2;
	private final TableName table1;
	private final TableName table2;
	private final Map<ColumnName,ColumnName> otherSide = new HashMap<ColumnName,ColumnName>();
	
	private ColumnListEquality(TableName oneTable, IdentifierList oneColumnList,
			TableName otherTable, IdentifierList otherColumnList) {
		boolean oneGoesFirst = false;
		if (oneTable.compareTo(otherTable) < 0) {
			oneGoesFirst = true;
		} else if (oneTable.compareTo(otherTable) == 0) {
			oneGoesFirst = oneColumnList.compareTo(otherColumnList) < 0;
		}
		table1 = oneGoesFirst ? oneTable : otherTable;
		table2 = oneGoesFirst ? otherTable : oneTable;
		columns1 = oneGoesFirst ? oneColumnList : otherColumnList;
		columns2 = oneGoesFirst ? otherColumnList : oneColumnList;
		for (int i = 0; i < columns1.size(); i++) {
			ColumnName c1 = table1.qualifyIdentifier(columns1.get(i));
			ColumnName c2 = table2.qualifyIdentifier(columns2.get(i));
			otherSide.put(c1, c2);
			otherSide.put(c2, c1);
		}
	}
	
	public boolean isSameTable() {
		return table1.equals(table2);
	}
	
	public boolean containsColumn(ColumnName column) {
		return (
				(!column.isQualified() || column.getQualifier().equals(table1))
				&& columns1.contains(column.getColumn()))
			|| (
				(!column.isQualified() || column.getQualifier().equals(table2))
				&& columns2.contains(column.getColumn()));
	}

	public TableName getTableName1() {
		return table1;
	}
	
	public TableName getTableName2() {
		return table2;
	}
	
	public IdentifierList getColumns1() {
		return columns1;
	}
	
	public IdentifierList getColumns2() {
		return columns2;
	}
	
	public ColumnName getEqualColumn(ColumnName column) {
		return otherSide.get(column);
	}
	
	@Override
	public boolean isTrue() {
		return false;
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	@Override
	public boolean isConstant() {
		return false;
	}

	@Override
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue, 
			boolean constIfFalse, boolean constIfConstantValue) {
		return false;
	}

	@Override
	public Set<ColumnName> getColumns() {
		return otherSide.keySet();
	}

	@Override
	public Expression rename(Renamer renamer) {
		return new ColumnListEquality(
				renamer.applyTo(table1), renamer.applyTo(table1, columns1),
				renamer.applyTo(table2), renamer.applyTo(table2, columns2));
	}

	public Expression toSimpleExpression() {
		List<Expression> eqs = new ArrayList<Expression>();
		for (int i = 0; i < columns1.size(); i++) {
			eqs.add(Equality.createColumnEquality(
					table1.qualifyIdentifier(columns1.get(i)),
					table2.qualifyIdentifier(columns1.get(i))));
		}
		return Conjunction.create(eqs);
	}
	
	public Expression substitute(ColumnName column, Expression substitution) {
		Expression simple = toSimpleExpression();
		Expression substituted = simple.substitute(column, substitution);
		return simple.equals(substituted) ? this : substituted;
	}

	@Override
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return GenericType.BOOLEAN.dataTypeFor(vendor);
	}

	@Override
	public String toSQL(DatabaseOp table, Vendor vendor) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < columns1.size(); i++) {
			if (i > 0) {
				result.append(" AND ");
			}
			result.append(vendor.toString(ColumnName.create(table1, columns1.get(i))));
			result.append("=");
			result.append(vendor.toString(ColumnName.create(table2, columns2.get(i))));
		}
		return result.toString();
	}

	public String toString() {
		return "(" + toSQL(null, Vendor.SQL92) + ")";
	}

	public int hashCode() {
		return table1.hashCode() ^ columns1.hashCode() ^ table2.hashCode() ^ columns2.hashCode() ^ 311;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof ColumnListEquality)) {
			return false;
		}
		ColumnListEquality other = (ColumnListEquality) o;
		return table1.equals(other.table1) && table2.equals(other.table2) &&
				columns1.equals(other.columns2) && columns2.equals(other.columns2);
	}
}
