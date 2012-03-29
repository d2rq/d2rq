package de.fuberlin.wiwiss.d2rq.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationImpl;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * TODO Describe this type
 * TODO isUnique is not properly handled yet
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class RelationBuilder {
	private final ConnectedDB database;
	private Expression condition = Expression.TRUE;
	private Set<Join> joinConditions = new HashSet<Join>();
	private Set<Alias> aliases = new HashSet<Alias>();
	private final Set<ProjectionSpec> projections = new HashSet<ProjectionSpec>();
	private boolean isUnique = false;
	private List<OrderSpec> orderSpecs = new ArrayList<OrderSpec>();
	private int limit = Relation.NO_LIMIT;
	private int limitInverse = Relation.NO_LIMIT;
		
	public RelationBuilder(ConnectedDB database) {
		this.database = database;
	}
	
	public void setIsUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}
	
	public void addOther(RelationBuilder other) {
		this.condition = this.condition.and(other.condition);
		this.joinConditions.addAll(other.joinConditions);
		this.aliases.addAll(other.aliases);
		this.projections.addAll(other.projections);
		this.isUnique = this.isUnique || other.isUnique;
		if (!other.orderSpecs.isEmpty()) {
			// Overwrite our ordering if the other builder is ordered 
			this.orderSpecs = other.orderSpecs;
		}
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
		if (!other.orderSpecs.isEmpty()) {
			// Overwrite our ordering if the other builder is ordered 
			this.orderSpecs = aliases().applyTo(other.orderSpecs);
		}
		this.limit = Relation.combineLimits(limit, other.limit);
		this.limitInverse = Relation.combineLimits(limitInverse, other.limitInverse);
	}
	
	public void addCondition(String condition) {
		this.condition = this.condition.and(SQLExpression.create(condition));
	}
	
	public void addCondition(Expression condition) {
		this.condition = this.condition.and(condition);
	}
	
	public void addAlias(Alias alias) {
		this.aliases.add(alias);
	}
	
	public void addAliases(Collection<Alias> aliases) {
		this.aliases.addAll(aliases);
	}
	
	public void addJoinCondition(Join joinCondition) {
		this.joinConditions.add(joinCondition);
	}
	
	public void addProjection(ProjectionSpec projection) {
		this.projections.add(projection);
	}
	
	public void setOrderSpecs(List<OrderSpec> orderSpecs) {
	    this.orderSpecs = orderSpecs;
	}
	
	public void setLimit(int limit) {
	    this.limit = limit;
	}
	
	public void setLimitInverse(int limitInverse) {
	    this.limitInverse = limitInverse;
	}
	
	public Relation buildRelation() {
		if (!isUnique) {
			for (ProjectionSpec projection: projections) {
				for (Attribute column: projection.requiredAttributes()) {
					if (!database.columnType(column).supportsDistinct()) {
						throw new D2RQException("The datatype of " + column + " (" + 
								database.columnType(column) + ") does not support " +
								"SELECT DISTINCT and therefore cannot be used in a " +
								"context where d2rq:containsDuplicates is true.", 
								D2RQException.DATATYPE_DOES_NOT_SUPPORT_DISTINCT);
					}
				}
			}
		}
		AliasMap aliases = aliases();
		
		// The contract is that all projections are required (must not be NULL).
		// So let's add them all as soft conditions. The soft condition for
		// a non-nullable column is TRUE.
		Collection<Expression> softConditions = new HashSet<Expression>(projections.size());
		for (ProjectionSpec projection: projections) {
			softConditions.add(projection.notNullExpression(database, aliases));
		}
		
		return new RelationImpl(
				database,
				aliases, 
				this.condition, 
				Conjunction.create(softConditions),
				this.joinConditions,
				this.projections,
				this.isUnique,
				this.orderSpecs,
				this.limit,
				this.limitInverse);
	}
	
	public AliasMap aliases() {
		return new AliasMap(this.aliases);
	}
}