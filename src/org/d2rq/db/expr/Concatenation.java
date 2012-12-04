package org.d2rq.db.expr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


public class Concatenation extends Expression {

	public static Expression create(List<Expression> expressions) {
		List<Expression> nonEmpty = new ArrayList<Expression>(expressions.size());
		for (Expression expression: expressions) {
			if (expression instanceof Constant 
					&& "".equals(((Constant) expression).value())) {
				continue;
			}
			nonEmpty.add(expression);
		}
		if (nonEmpty.isEmpty()) {
			return Constant.create("", GenericType.CHARACTER);
		}
		if (nonEmpty.size() == 1) {
			return nonEmpty.get(0);
		}
		return new Concatenation(nonEmpty);
	}
	
	private final List<Expression> parts;
	private final Set<ColumnName> columns = new HashSet<ColumnName>();
	
	private Concatenation(List<Expression> parts) {
		this.parts = parts;
		for (Expression expression: parts) {
			columns.addAll(expression.getColumns());
		}
	}
	
	public Set<ColumnName> getColumns() {
		return columns;
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression rename(Renamer columnRenamer) {
		List<Expression> renamedExpressions = new ArrayList<Expression>(parts.size());
		for (Expression expression: parts) {
			renamedExpressions.add(columnRenamer.applyTo(expression));
		}
		return new Concatenation(renamedExpressions);
	}

	public String toSQL(DatabaseOp table, Vendor vendor) {
		String[] fragments = new String[parts.size()];
		for (int i = 0; i < parts.size(); i++) {
			Expression part = (Expression) parts.get(i);
			fragments[i] = part.toSQL(table, vendor);
		}
		return vendor.getConcatenationExpression(fragments);
	}
	
	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return GenericType.CHARACTER.dataTypeFor(vendor);
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Concatenation)) return false;
		return parts.equals(((Concatenation) other).parts);
	}
	
	public int hashCode() {
		return parts.hashCode() ^ 234645;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("Concatenation(");
		Iterator<Expression> it = parts.iterator();
		while (it.hasNext()) {
			result.append(it.next());
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}
}
