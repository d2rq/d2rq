package de.fuberlin.wiwiss.d2rq.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

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
		Collection newAliases = new ArrayList();
		Iterator it = this.aliases.iterator();
		while (it.hasNext()) {
			Alias alias = (Alias) it.next();
			newAliases.add(other.aliases().originalOf(alias));
		}
		this.aliases.addAll(newAliases);
	}
	
	public void addCondition(String condition) {
		this.condition = this.condition.and(SQLExpression.create(condition));
	}
	
	public void addAlias(Alias alias) {
		this.aliases.add(alias);
	}
	
	public void addJoinCondition(Join joinCondition) {
		this.joinConditions.add(joinCondition);
	}
	
	public Relation buildRelation(ConnectedDB database) {
		return new RelationImpl(
				database,
				aliases(), 
				this.condition, 
				this.joinConditions);
	}
	
	public AliasMap aliases() {
		return new AliasMap(this.aliases);
	}
}