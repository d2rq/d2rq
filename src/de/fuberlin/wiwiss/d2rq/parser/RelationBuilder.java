package de.fuberlin.wiwiss.d2rq.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * TODO Describe this type
 * TODO isUnique is not properly handled yet
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: RelationBuilder.java,v 1.11 2009/07/29 12:03:53 fatorange Exp $
 */
public class RelationBuilder {
	private Expression condition = Expression.TRUE;
	private Set joinConditions = new HashSet();
	private Set aliases = new HashSet();
	private final Set projections = new HashSet();
	private boolean isUnique = false;
	private Attribute order = null;
	private boolean orderDesc = false;
	private int limit = Relation.NO_LIMIT;
	private int limitInverse = Relation.NO_LIMIT;
		
	public RelationBuilder() {}
	
	public void setIsUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}
	
	public void addOther(RelationBuilder other) {
		this.condition = this.condition.and(other.condition);
		this.joinConditions.addAll(other.joinConditions);
		this.aliases.addAll(other.aliases);
		this.projections.addAll(other.projections);
		this.isUnique = this.isUnique || other.isUnique;
		this.orderDesc = order==null?other.orderDesc:orderDesc;
		this.order = order==null?other.order:order;
		this.limit = Relation.combineLimits(limit, other.limit);
		this.limitInverse = Relation.combineLimits(limitInverse, other.limitInverse);
	}
	
	/**
	 * Adds information from another relation builder to this one,
	 * applying this builder's alias mappings to the other one.
	 *  
	 * @param other A relation builder that potentially uses aliases declared in this builder
	 */
	public void addAliased(RelationBuilder other) {
		this.condition = this.condition.and(aliases().applyTo(other.condition));
		this.joinConditions.addAll(aliases().applyToJoinSet(other.joinConditions));
		this.projections.addAll(aliases().applyToProjectionSet(other.projections));
		Collection<Alias> newAliases = new ArrayList<Alias>();
		Collection<Alias> removedAliases = new ArrayList<Alias>();
		for (Alias alias : (Collection<Alias>) this.aliases) {
			Alias newAlias = other.aliases().originalOf(alias);
			if (!alias.equals(newAlias)) {
				removedAliases.add(alias);
			}
			newAliases.add(newAlias);
		}
		this.aliases.removeAll(removedAliases);
		this.aliases.addAll(newAliases);
		this.orderDesc = order==null?other.orderDesc:orderDesc;
		// FIXME should the order clauses be concatenated? however, this would still be not commutative
		this.order = order==null?other.order:order;
		this.limit = Relation.combineLimits(limit, other.limit);
		this.limitInverse = Relation.combineLimits(limitInverse, other.limitInverse);
	}
	
	public void addCondition(String condition) {
		this.condition = this.condition.and(SQLExpression.create(condition));
	}
	
	public void addAlias(Alias alias) {
		this.aliases.add(alias);
	}
	
	public void addAliases(Collection aliases) {
		this.aliases.addAll(aliases);
	}
	
	public void addJoinCondition(Join joinCondition) {
		this.joinConditions.add(joinCondition);
	}
	
	public void addProjection(ProjectionSpec projection) {
		this.projections.add(projection);
	}
	
	public void setOrder(Attribute attribute, boolean orderDesc) {
	    this.order = attribute;
	    this.orderDesc = orderDesc;
	}
	
	public void setLimit(int limit) {
	    this.limit = limit;
	}
	
	public void setLimitInverse(int limitInverse) {
	    this.limitInverse = limitInverse;
	}
	
	public Relation buildRelation(ConnectedDB database) {
		return new RelationImpl(
				database,
				aliases(), 
				this.condition, 
				this.joinConditions,
				this.projections,
				this.isUnique,
				this.order,
				this.orderDesc,
				this.limit,
				this.limitInverse);
	}
	
	public AliasMap aliases() {
		return new AliasMap(this.aliases);
	}
}