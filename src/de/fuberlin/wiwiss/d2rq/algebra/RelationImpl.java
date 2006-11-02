package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class RelationImpl implements Relation {
	private ConnectedDB database;
	private AliasMap aliases;
	private Expression condition;
	private Set joinConditions;
	
	public RelationImpl(ConnectedDB database, AliasMap aliases,
			Expression condition, Set joinConditions) {
		this.database = database;
		this.aliases = aliases;
		this.condition = condition;
		this.joinConditions = joinConditions;
	}

	public ConnectedDB database() {
		return this.database;
	}
	
	public AliasMap aliases() {
		return this.aliases;
	}

	public Expression condition() {
		return this.condition;
	}

	public Set joinConditions() {
		return this.joinConditions;
	}

	public Relation select(Expression selectCondition) {
		if (selectCondition.isTrue()) {
			return this;
		}
		if (selectCondition.isFalse()) {
			return Relation.EMPTY;
		}
		return new RelationImpl(this.database, this.aliases, this.condition.and(selectCondition),
				this.joinConditions);
	}
	
	public Relation renameColumns(ColumnRenamer renames) {
		return new RelationImpl(this.database, renames.applyTo(this.aliases),
				renames.applyTo(this.condition), renames.applyToJoinSet(this.joinConditions));
	}
}
