package org.d2rq.lang;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.TableName;

/**
 * A representation of a <code>d2rq:join</code> value. Expresses equality
 * between two columns, optionally indicating that the equality runs
 * along a foreign key relationship.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Join {
	
	public static Join[] createFrom(TableName table, ForeignKey fk) {
		Join[] result = new Join[fk.getLocalColumns().size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = new Join(
					table.qualifyIdentifier(fk.getLocalColumns().get(i)),
					fk.getReferencedTable().qualifyIdentifier(fk.getReferencedColumns().get(i)), 
					Direction.RIGHT);
		}
		return result;
	}
	
	public enum Direction {
		LEFT("<="),
		RIGHT("=>"),
		UNDIRECTED("=");
		private final String operator;
		Direction(String operator) { this.operator = operator; }
		public String toString() { return operator; }
		public Direction flipped() {
			switch (this) {
			case LEFT: return RIGHT;
			case RIGHT: return LEFT;
			default: return UNDIRECTED;
			}
		}
	}
	
	private final ColumnName column1;
	private final ColumnName column2;
	private final Direction direction;
	
	public Join(ColumnName column1, ColumnName column2, Direction direction) {
		this.column1 = column1;
		this.column2 = column2;
		this.direction = direction;
	}
	
	public TableName getTable1() {
		return column1.getQualifier();
	}
	
	public TableName getTable2() {
		return column2.getQualifier();
	}
	
	public ColumnName getColumn1() {
		return column1;
	}
	
	public ColumnName getColumn2() {
		return column2;
	}
	
	public Direction getDirection() {
		return direction;
	}
	
	public Join getFlipped() {
		return new Join(column2, column1, direction.flipped());
	}
}
