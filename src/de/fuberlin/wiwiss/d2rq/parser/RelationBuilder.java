package de.fuberlin.wiwiss.d2rq.parser;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.Expression;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.map.AliasMap;
import de.fuberlin.wiwiss.d2rq.map.Database;

public class RelationBuilder {
	private Expression condition = Expression.TRUE;
	private Set joinConditions = new HashSet();
	private Set aliases = new HashSet();

	public RelationBuilder() {}
	
	public void addOther(RelationBuilder other) {
		this.condition = this.condition.and(other.condition);
		this.joinConditions.addAll(other.joinConditions);
		this.aliases.addAll(other.aliases);
	}
	
	public void addAliased(RelationBuilder other) {
		this.condition = this.condition.and(aliases().applyTo(other.condition));
		this.joinConditions.addAll(aliases().applyToJoinSet(other.joinConditions));
		// TODO: Do we have to apply our aliases to other.aliases as well?
		this.aliases.addAll(other.aliases);
	}
	
	public void addCondition(String condition) {
		this.condition = this.condition.and(new Expression(condition));
	}
	
	public void addAlias(String alias) {
		this.aliases.add(alias);
	}
	
	public void addJoinCondition(String joinCondition) {
		this.joinConditions.add(joinCondition);
	}
	
	public Relation buildRelation(Database database) {
		return new RelationImpl(
				database,
				aliases(), 
				Collections.EMPTY_MAP, 
				this.condition, 
				Join.buildFromSQL(this.joinConditions));
	}
	
	public AliasMap aliases() {
		return AliasMap.buildFromSQL(this.aliases);
	}
}