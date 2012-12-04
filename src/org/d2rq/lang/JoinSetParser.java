package org.d2rq.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.d2rq.D2RQException;
import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableName;


/**
 * Parses join condition strings as used in d2rq:join. Groups multiple
 * condition that connect the same two table (multi-column keys) into
 * a single join {@link ColumnListEquality} expression. Join conditions have
 * the format:
 * 
 * [schema.]table.column OP [schema.].table.column
 * 
 * Where OP is either <= or = or =>. The arrow-style operators assert that
 * a foreign key relationship exists along the join. The asserted foreign
 * keys can be retrieved as well.
 */
public class JoinSetParser {
	private final Collection<String> joins;
	private final Set<ColumnListEquality> expressions = new HashSet<ColumnListEquality>();
	private final Map<ForeignKey,TableName> assertedForeignKeys = 
			new HashMap<ForeignKey,TableName>();
	private boolean done = false;

	/**
	 * @param joinExpressions a collection of D2RQ-style join expressions
	 */
	public JoinSetParser(Collection<String> joinExpressions) {
		this.joins = joinExpressions;
	}
	
	public Set<ColumnListEquality> getExpressions() {
		run();
		return expressions;
	}
	
	public Map<ForeignKey,TableName> getAssertedForeignKeys() {
		run();
		return assertedForeignKeys;
	}
	
	public void run() {
		if (done) return;
		done = true;
		List<Bucket> buckets = new ArrayList<Bucket>();
		for (String expression: joins) {
			Join parsed = parseJoinCondition(expression);
			for (Bucket bucket: buckets) {
				if (bucket.joinsSameTablesAs(parsed)) {
					bucket.add(parsed);
					parsed = null;
					break;
				}
			}
			if (parsed != null) {
				buckets.add(new Bucket(parsed));
			}
		}
		for (Bucket bucket: buckets) {
			expressions.add(bucket.getExpression());
			if (bucket.getForeignKeyTable() != null) {
				assertedForeignKeys.put(
						bucket.getForeignKeyDefinition(), 
						bucket.getForeignKeyTable());
			}
		}
	}

	private Join parseJoinCondition(String joinCondition) {
		Direction operator = null;
		int index = -1;
		for (Direction direction: Direction.values()) {
			index = joinCondition.indexOf(direction.operator);
			if (index >= 0) {
				operator = direction;
				break;
			}
		}
		if (operator == null) {
			throw new D2RQException("d2rq:join \"" + joinCondition +
					"\" is not in \"table1.col1 [ <= | => | = ] table2.col2\" form",
					D2RQException.SQL_INVALID_JOIN);
		}
		ColumnName leftSide = Microsyntax.parseColumn(joinCondition.substring(0, index).trim());
		ColumnName rightSide = Microsyntax.parseColumn(joinCondition.substring(index + operator.operator.length()).trim());
		return new Join(leftSide, rightSide, operator);
	}
	
	private class Bucket {
		private final TableName table1;
		private final TableName table2;
		private final List<Identifier> columns1 = new ArrayList<Identifier>();
		private final List<Identifier> columns2 = new ArrayList<Identifier>();
		private final Direction operator;
		Bucket(Join seed) {
			this.table1 = seed.column1.getQualifier();
			this.table2 = seed.column2.getQualifier();
			this.operator = seed.operator;
			add(seed);
		}
		void add(Join join) {
			if (join.operator != operator) {
				throw new D2RQException(
						"d2rq:join between " + join.column1.getQualifier() + " and " + 
						join.column2.getQualifier() + " has conflicting join operators " +
						operator.operator + " and " + join.operator.operator, 
						D2RQException.SQL_INVALID_JOIN);
			}
			if (!table1.equals(join.column1.getQualifier())) {
				join = join.flip();
			}
			columns1.add(join.column1.getColumn());
			columns2.add(join.column2.getColumn());
		}
		boolean joinsSameTablesAs(Join join) {
			return (table1.equals(join.column1.getQualifier()) && 
					table2.equals(join.column2.getQualifier())) ||
					(table1.equals(join.column2.getQualifier()) && 
					table2.equals(join.column1.getQualifier()));
		}
		ColumnListEquality getExpression() {
			return ColumnListEquality.create(
					table1, Key.createFromIdentifiers(columns1), 
					table2, Key.createFromIdentifiers(columns2));
		}
		TableName getForeignKeyTable() {
			if (operator == Direction.RIGHT) {
				return table1;
			}
			if (operator == Direction.LEFT) {
				return table2;
			}
			return null;
		}
		ForeignKey getForeignKeyDefinition() {
			if (operator == Direction.RIGHT) {
				return new ForeignKey(Key.createFromIdentifiers(columns1), 
						Key.createFromIdentifiers(columns2), table2);
			}
			if (operator == Direction.LEFT) {
				return new ForeignKey(Key.createFromIdentifiers(columns2), 
						Key.createFromIdentifiers(columns1), table1);
			}
			return null;
		}
	}
	
	private class Join {
		final ColumnName column1;
		final ColumnName column2;
		final Direction operator;
		Join(ColumnName column1, ColumnName column2, Direction operator) {
			this.column1 = column1;
			this.column2 = column2;
			this.operator = operator;
		}
		Join flip() {
			return new Join(column2, column1, operator.flipped());
		}
	}

	public enum Direction {
		LEFT("<="),
		RIGHT("=>"),
		UNDIRECTED("=");
		public final String operator;
		Direction(String operator) {
			this.operator = operator;
		}
		public Direction flipped() {
			switch (this) {
			case LEFT: return RIGHT;
			case RIGHT: return LEFT;
			default: return UNDIRECTED;
			}
		}
	}
}