package org.d2rq.values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.expr.ColumnExpr;
import org.d2rq.db.expr.Concatenation;
import org.d2rq.db.expr.Conjunction;
import org.d2rq.db.expr.Constant;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.nodes.NodeSetFilter;


/**
 * A blank node identifier that uniquely identifies all resources generated from
 * a specific ClassMap.
 * <p>
 * (Note: The implementation makes some assumptions about the Column
 * class to keep the code simple and fast. This means BlankNodeIdentifier
 * might not work with some hypothetical subclasses of Column.)
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class BlankNodeIDValueMaker implements ValueMaker {
	private final static String DELIMITER = "@@";

	private final String id;
	private final List<ColumnName> columns;
	private final Set<ColumnName> columnsAsSet;
	
	/**
	 * Constructs a new blank node identifier.
	 * @param id A string that is unique for the class map
	 * 		whose resources are identified by this BlankNodeIdentifier 
	 * @param columns A set of {@link ColumnName}s that uniquely
	 * 		identify the nodes
	 */
	public BlankNodeIDValueMaker(String id, List<ColumnName> columns) {
		this.id = id;
		this.columns = columns;
		columnsAsSet = new HashSet<ColumnName>(columns);
	}

	public Set<ColumnName> getRequiredColumns() {
		return columnsAsSet;
	}
	
	public String getID() {
		return id;
	}
	
	public void describeSelf(NodeSetFilter c) {
		c.limitValuesToBlankNodeID(this);
	}

	/**
	 * A string matches if it contains the correct number of delimiters,
	 * and the first delimited part is the correct ID.
	 */
	public boolean matches(String value) {
		return value != null && splitValue(value).length == columns.size() + 1 
				&& id.equals(splitValue(value)[0]);
	}
	
	public Expression valueExpression(String value, DatabaseOp table, Vendor vendor) {
		if (!matches(value)) return Expression.FALSE;
		String[] parts = splitValue(value);
		int i = 1;	// parts[0] is classMap identifier
		Collection<Expression> expressions = new ArrayList<Expression>(columns.size());
		for (ColumnName column: columns) {
			expressions.add(Equality.createColumnValue(
					column, parts[i], table.getColumnType(column)));
			i++;
		}
		return Conjunction.create(expressions);
	}

	private String[] splitValue(String value) {
		return value.split(DELIMITER);
	}
	
	/**
	 * Creates an identifier from a database row.
	 * @param row a database row
	 * @return this column's blank node identifier
	 */
	public String makeValue(ResultRow row) {
		StringBuffer result = new StringBuffer(this.id);
		for (ColumnName column: columns) {
			String value = row.get(column);
			if (value == null) {
				return null;
		    }
			result.append(DELIMITER);
			result.append(value);
		}
        return result.toString();
	}
	
	public ValueMaker rename(Renamer renamer) {
		List<ColumnName> replacedColumns = new ArrayList<ColumnName>();
		for (ColumnName column: columns) {
			replacedColumns.add(renamer.applyTo(column));
		}
		return new BlankNodeIDValueMaker(this.id, replacedColumns);
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		List<OrderSpec> result = new ArrayList<OrderSpec>(columns.size());
		for (ColumnName column: columns) {
			result.add(new OrderSpec(new ColumnExpr(column), ascending));
		}
		return result;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("BlankNodeID(");
		Iterator<ColumnName> it = columns.iterator();
		while (it.hasNext()) {
			ColumnName column = (ColumnName) it.next();
			result.append(column);
			if (it.hasNext()) {
				result.append(",");
			}
		}
		result.append(")");
		return result.toString();
	}
	
	public Expression toExpression() {
		List<Expression> parts = new ArrayList<Expression>();
		parts.add(Constant.create(id, GenericType.CHARACTER));
		for (ColumnName column: columns) {
			parts.add(Constant.create(DELIMITER, GenericType.CHARACTER));
			parts.add(new ColumnExpr(column));
		}
		return Concatenation.create(parts);
	}
}