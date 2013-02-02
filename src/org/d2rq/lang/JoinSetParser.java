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
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.IdentifierList;
import org.d2rq.db.schema.TableName;
import org.d2rq.lang.Join.Direction;


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
	
	/**
	 * @param joinExpressions a collection of D2RQ-style join expressions
	 */
	public static JoinSetParser create(String... joinExpressions) {
		List<Join> result = new ArrayList<Join>();
		for (String join: joinExpressions) {
			result.add(Microsyntax.parseJoin(join));
		}
		return new JoinSetParser(result);
	}
	
	public static JoinSetParser create(Collection<Join> joinExpressions) {
		return new JoinSetParser(joinExpressions);
	}
	
	private final Collection<Join> joins;
	private final Set<ColumnListEquality> expressions = new HashSet<ColumnListEquality>();
	private final Map<ForeignKey,TableName> assertedForeignKeys = 
			new HashMap<ForeignKey,TableName>();
	private boolean done = false;

	private JoinSetParser(Collection<Join> joinExpressions) {
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
		for (Join join: joins) {
			for (Bucket bucket: buckets) {
				if (bucket.joinsSameTablesAs(join)) {
					bucket.add(join);
					join = null;
					break;
				}
			}
			if (join != null) {
				buckets.add(new Bucket(join));
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

	private class Bucket {
		private final TableName table1;
		private final TableName table2;
		private final List<Identifier> columns1 = new ArrayList<Identifier>();
		private final List<Identifier> columns2 = new ArrayList<Identifier>();
		private final Direction operator;
		Bucket(Join seed) {
			this.table1 = seed.getTable1();
			this.table2 = seed.getTable2();
			this.operator = seed.getDirection();
			add(seed);
		}
		void add(Join join) {
			if (join.getDirection() != operator) {
				throw new D2RQException(
						"d2rq:join between " + join.getTable1() + " and " + 
						join.getTable2() + " has conflicting join operators " +
						operator + " and " + join.getDirection(), 
						D2RQException.SQL_INVALID_JOIN);
			}
			if (!table1.equals(join.getTable1())) {
				join = join.getFlipped();
			}
			columns1.add(join.getColumn1().getColumn());
			columns2.add(join.getColumn2().getColumn());
		}
		boolean joinsSameTablesAs(Join join) {
			return (table1.equals(join.getTable1()) && 
					table2.equals(join.getTable2())) ||
					(table1.equals(join.getTable2()) && 
					table2.equals(join.getTable1()));
		}
		ColumnListEquality getExpression() {
			return ColumnListEquality.create(
					table1, IdentifierList.createFromIdentifiers(columns1), 
					table2, IdentifierList.createFromIdentifiers(columns2));
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
				return new ForeignKey(IdentifierList.createFromIdentifiers(columns1), 
						IdentifierList.createFromIdentifiers(columns2), table2);
			}
			if (operator == Direction.LEFT) {
				return new ForeignKey(IdentifierList.createFromIdentifiers(columns2), 
						IdentifierList.createFromIdentifiers(columns1), table1);
			}
			return null;
		}
	}
}