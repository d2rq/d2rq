package org.d2rq.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DistinctOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.OrderOp;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.ProjectionSpec.ColumnProjectionSpec;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.OpVisitor;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.vendor.Vendor;



/**
 * Turns a {@link DatabaseOp} into a SQL SELECT statement.
 * 
 * Works by doing a depth-first traversal of the query tree, collecting
 * information in a {@link SimpleQuery} object. Such an object can contain
 * a non-nested SQL query. Once nesting is necessary, the current
 * {@link SimpleQuery} is put on a stack, a new one is allocated and
 * information collected in it, and then it is "flattened" by turning
 * it into a raw SQL string that becomes a FROM clause in the upper
 * instance.
 * 
 * The list of SELECT clauses is not built while working the tree, but
 * computed in the end from the {@link DatabaseOp}'s column list.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SelectStatementBuilder extends OpVisitor.Default {
	private final DatabaseOp input;
	private final Vendor vendor;
	private final Stack<SimpleQuery> queryStack = new Stack<SimpleQuery>();
	private boolean done = false;
	
	public SelectStatementBuilder(DatabaseOp input, Vendor vendor) {
		this.input = input;
		this.vendor = vendor;
	}

	private void run() {
		if (done) return;
		done = true;
		queryStack.push(new SimpleQuery());
		input.accept(this);
	}
	
	public String getSQL() {
		run();
		return queryStack.peek().getSQL(input, vendor, input.getColumns());
	}

	public List<ProjectionSpec> getColumnSpecs() {
		run();
		return queryStack.peek().toProjectionSpecs(input.getColumns());
	}
	
	@Override
	public void visit(TableOp table) {
		queryStack.peek().fromClauses.put(table.getTableName(), null);
	}

	@Override
	public void visit(SQLOp table) {
		queryStack.peek().rawSQL = table.getSQL();
	}

	@Override
	public void visitOpTrue() {
		// Do nothing
	}

	@Override
	public void visitLeave(ProjectOp table) {
		for (ProjectionSpec spec: table.getProjections()) {
			queryStack.peek().projections.put(spec.getColumn(), spec);
		}
	}
	
	@Override
	public void visitLeave(LimitOp table) {
		if (table.getLimit() == 0) {
			queryStack.peek().whereClause = Expression.FALSE;
		} else {
			queryStack.peek().limit = LimitOp.combineLimits(queryStack.peek().limit, table.getLimit());
		}
	}
	
	@Override
	public void visitLeave(DistinctOp table) {
		queryStack.peek().distinct = true;
	}

	@Override
	public void visitLeave(SelectOp table) {
		queryStack.peek().whereClause = queryStack.peek().whereClause.and(table.getCondition());
	}
	
	@Override
	public void visitLeave(OrderOp table) {
		List<OrderSpec> newOrder = new ArrayList<OrderSpec>(table.getOrderBy());
		for (OrderSpec spec: queryStack.peek().orderByClauses) {
			if (newOrder.contains(spec)) continue;
			newOrder.add(spec);
		}
		queryStack.peek().orderByClauses = newOrder;
	}

	@Override
	public void visitLeave(EmptyOp table) {
		queryStack.peek().whereClause = Expression.FALSE;
	}

	@Override
	public boolean visitEnter(AliasOp table) {
		queryStack.push(new SimpleQuery());
		return true;
	}
	
	@Override
	public void visitLeave(AliasOp table) {
		String wrappedSQL;
		if (queryStack.peek().isSimpleTable()) {
			TableName wrappedTable = queryStack.peek().fromClauses.keySet().iterator().next();
			wrappedSQL = vendor.toString(wrappedTable);
		} else {
			queryStack.peek().buildSQL(table.getOriginal(), vendor, table.getOriginal().getColumns());
			wrappedSQL = "(" + queryStack.peek().rawSQL + ")";
		}
		queryStack.pop();
		queryStack.peek().fromClauses.put(table.getTableName(), wrappedSQL);
	}

	@Override
	public void visitLeave(InnerJoinOp table) {
		for (ColumnListEquality join: table.getJoinConditions()) {
			queryStack.peek().whereClause = queryStack.peek().whereClause.and(join);
		}
	}
	
	/**
	 * A SQL query without nesting. FROM clauses are represented as simple
	 * SQL strings, so subqueries can be represented as a string of the
	 * form "(SELECT ...) AS xxx".
	 */
	private class SimpleQuery {
		String rawSQL;
		Map<ColumnName,ProjectionSpec> projections;
		Map<TableName,String> fromClauses;
		int limit;
		Expression whereClause;
		boolean distinct;
		List<OrderSpec> orderByClauses;
		
		SimpleQuery() {
			reset();
		}
		
		private void reset() {
			rawSQL = null;
			projections = new TreeMap<ColumnName,ProjectionSpec>();
			fromClauses = new TreeMap<TableName,String>();
			limit = LimitOp.NO_LIMIT;
			whereClause = Expression.TRUE;
			distinct = false;
			orderByClauses = new ArrayList<OrderSpec>();
		}
		
		boolean isSimpleTable() {
			return rawSQL == null && projections.isEmpty() && limit == LimitOp.NO_LIMIT
					&& whereClause.isTrue() && !distinct && orderByClauses.isEmpty()
					&& fromClauses.size() == 1 && fromClauses.entrySet().iterator().next().getValue() == null;
		}
		
		void buildSQL(DatabaseOp table, Vendor vendor, List<ColumnName> columns) {
			String tmp = getSQL(table, vendor, columns);
			reset();
			rawSQL = tmp;
		}
		
		private List<ProjectionSpec> toProjectionSpecs(List<ColumnName> columns) {
			List<ProjectionSpec> result = new ArrayList<ProjectionSpec>();
			for (ColumnName column: columns) {
				result.add(projections.containsKey(column) ? 
						projections.get(column) : ColumnProjectionSpec.create(column));
			}
			return result;
		}
		
		String getSQL(DatabaseOp table, Vendor vendor, List<ColumnName> columns) {
			if (rawSQL != null) return rawSQL;
			StringBuffer result = new StringBuffer("SELECT ");
			if (distinct) {
				result.append("DISTINCT ");
			}
			if (limit != LimitOp.NO_LIMIT) {			
				String prefix = vendor.getRowNumLimitAsSelectModifier(limit);
				if (!"".equals(prefix)) {
					result.append(prefix);
					result.append(" ");
				}
			}
			if (columns.isEmpty()) {
				result.append("1");
			} else {
				Iterator<ProjectionSpec> columnsIt = toProjectionSpecs(columns).iterator();
				while (columnsIt.hasNext()) {
					result.append(columnsIt.next().toSQL(table, vendor));
					if (columnsIt.hasNext()) {
						result.append(", ");
					}
				}
			}
			Iterator<TableName> tablesIt = fromClauses.keySet().iterator();
			if (!tablesIt.hasNext()) {
				if (vendor.getTrueTable() != null) {
					result.append(" FROM " + vendor.getTrueTable());
				}
			} else {
				result.append(" FROM ");
				while (tablesIt.hasNext()) {
					TableName t = tablesIt.next();
					if (fromClauses.get(t) != null) {
						result.append(fromClauses.get(t));
						result.append(vendor.getAliasOperator());
					}
					result.append(vendor.toString(t));
					if (tablesIt.hasNext()) {
						result.append(", ");
					}
				}
			}
			whereClause = whereClause.and(vendor.getRowNumLimitAsExpression(limit));
			if (!whereClause.isTrue()) {
				result.append(" WHERE ");
				result.append(whereClause.toSQL(table, vendor));
			}
			Iterator<OrderSpec> orderIt = orderByClauses.iterator();
			if (orderIt.hasNext()) {
				result.append(" ORDER BY ");
			}
			while (orderIt.hasNext()) {
				OrderSpec o = orderIt.next();
				if (o.isAscending()) {
					result.append(o.getExpression().toSQL(table, vendor));
				} else {
					result.append("DESC(");
					result.append(o.getExpression().toSQL(table, vendor));
					result.append(")");
				}
				if (orderIt.hasNext()) {
					result.append(", ");
				}
			}
			if (limit != LimitOp.NO_LIMIT) {			
				String suffix = vendor.getRowNumLimitAsQueryAppendage(limit);
				if (!"".equals(suffix)) {
					result.append(" ");
					result.append(suffix);
				}
			}
			return result.toString();
		}
	}
}
