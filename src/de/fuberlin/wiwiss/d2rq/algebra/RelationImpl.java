package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashSet;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class RelationImpl extends Relation {
	private final ConnectedDB database;
	private final AliasMap aliases;
	private final Expression condition;
	private final Set joinConditions;
	private final Set leftJoinConditions;
	private final Set projections;
	private final boolean isUnique;
	
	public RelationImpl(ConnectedDB database, AliasMap aliases,
			Expression condition, Set joinConditions, Set projections,
			boolean isUnique) {
		this.database = database;
		this.aliases = aliases;
		this.condition = condition;
		this.joinConditions = joinConditions;
		this.projections = projections;
		this.isUnique = isUnique;
		this.leftJoinConditions = new HashSet();
	}

	public RelationImpl(ConnectedDB database, AliasMap aliases,
			Expression condition, Set joinConditions, Set projections, Set leftJoinConditions,
			boolean isUnique) {
		this.database = database;
		this.aliases = aliases;
		this.condition = condition;
		this.joinConditions = joinConditions;
		this.projections = projections;
		this.isUnique = isUnique;
		this.leftJoinConditions = leftJoinConditions;
	}
	
	public Set leftJoinConditions() {
		return leftJoinConditions;
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

	public Set projections() {
		return projections;
	}

	public boolean isUnique() {
		return isUnique;
	}
	
	public Relation select(Expression selectCondition) {
		if (selectCondition.isTrue()) {
			return this;
		}
		if (selectCondition.isFalse()) {
			return Relation.EMPTY;
		}
		return new RelationImpl(database, aliases, condition.and(selectCondition),
				joinConditions, projections, isUnique);
	}
	
	public Relation renameColumns(ColumnRenamer renames) {
		return new RelationImpl(database, renames.applyTo(aliases),
				renames.applyTo(condition), renames.applyToJoinSet(joinConditions),
				renames.applyToProjectionSet(projections), isUnique);
	}

	public Relation project(Set projectionSpecs) {
		Set newProjections = new HashSet(projectionSpecs);
		newProjections.retainAll(projections);
		return new RelationImpl(database, aliases, condition, joinConditions, 
				newProjections, isUnique);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("RelationImpl(");
		if (isUnique) {
			result.append("[unique]");
		}
		result.append("\n");
		result.append("    project: ");
		result.append(projections);
		result.append("\n");
		if (!joinConditions.isEmpty()) {
			result.append("    joins: ");
			result.append(joinConditions);
			result.append("\n");
		}
		if (!condition.isTrue()) {
			result.append("    condition: ");
			result.append(condition);
			result.append("\n");
		}
		if (!aliases.equals(AliasMap.NO_ALIASES)) {
			result.append("    aliases: ");
			result.append(aliases);
			result.append("\n");
		}
		result.append(")");
		return result.toString();
	}
}
