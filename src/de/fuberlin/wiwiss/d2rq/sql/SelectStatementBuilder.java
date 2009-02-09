package de.fuberlin.wiwiss.d2rq.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.expr.Conjunction;
import de.fuberlin.wiwiss.d2rq.expr.Equality;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.optimizer.LeftJoin;

/**
 * Collects parts of a SELECT query and delivers a corresponding SQL statement.
 * Used within TripleResultSets.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: SelectStatementBuilder.java,v 1.27 2009/02/09 12:21:31 fatorange Exp $
 */
public class SelectStatementBuilder {
	private ConnectedDB database;
	private List selectSpecs = new ArrayList(10);
	private List conditions = new ArrayList();		// Expressions
	private Expression cachedCondition = null;
	private boolean eliminateDuplicates = false;
	private AliasMap aliases = AliasMap.NO_ALIASES;
	private Collection mentionedTables = new HashSet(5); // Strings in their alias forms	
	private StringBuffer leftJoinExpression = new StringBuffer();
	
	public SelectStatementBuilder(Relation relation) {
		if (relation.isTrivial()) {
			throw new IllegalArgumentException("Cannot create SQL for trivial relation");
		}
		if (relation.equals(Relation.EMPTY)) {
			throw new IllegalArgumentException("Cannot create SQL for empty relation");
		}
		database = relation.database();
		this.aliases = this.aliases.applyTo(relation.aliases());
		Iterator it = relation.joinConditions().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			Iterator it2 = join.attributes1().iterator();
			while (it2.hasNext()) {
				Attribute attribute1 = (Attribute) it2.next();
				Attribute attribute2 = join.equalAttribute(attribute1);
				addCondition(Equality.createAttributeEquality(attribute1, attribute2));
			}
		}
		addCondition(relation.condition());
		it = relation.projections().iterator();
		while (it.hasNext()) {
			addSelectSpec((ProjectionSpec) it.next());
		}
		eliminateDuplicates = !relation.isUnique();
	
		addMentionedTablesFromConditions();
		createLeftJoinExpression(relation.leftJoinConditions());
	}
	
	private Expression condition() {
		if (this.cachedCondition == null) {
			this.cachedCondition = Conjunction.create(this.conditions);
		}
		return this.cachedCondition;
	}
	
	private void addMentionedTablesFromConditions() {
		Iterator it = condition().attributes().iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			this.mentionedTables.add(column.relationName());
		}
	}
	
	/**
	 * Creates a sql-leftJoin-Expression if there is one
	 * @param leftJoinConditions
	 */
	private void createLeftJoinExpression(Set leftJoinConditions)
	{
		
		RelationName relationName1, relationName2;
		LeftJoin leftJoin;
		Set leftJoinTableNames = new HashSet();
		int tableNr = 1;
		
		// create the left-join tables (table1 left join table2, ...)
		for (Iterator iterator = leftJoinConditions.iterator(); iterator.hasNext(); )
		{
			
			leftJoin = (LeftJoin) iterator.next();
			relationName1 = leftJoin.getRelation1(); 
			relationName2 = leftJoin.getRelation2();
			
			this.mentionedTables.remove(relationName1);
			this.mentionedTables.remove(relationName2);
		
			// swap if necessary
			if (!leftJoinTableNames.contains(relationName1) && leftJoinTableNames.contains(relationName2))
			{
				relationName2 = relationName1;
			}
			
			leftJoinTableNames.add(relationName1);
			leftJoinTableNames.add(relationName2);
			
			// left-join syntax: table1 left join table2 on (...) left join table3 on (...)
			if (tableNr == 1)
			{
				leftJoinExpression.append(this.database.quoteRelationName(this.aliases.originalOf(relationName1)));
				
				if (this.database.dbTypeIs(ConnectedDB.Oracle)) 
				{
					leftJoinExpression.append(" ");
				} else {
					leftJoinExpression.append(" AS ");
				}
				leftJoinExpression.append(this.database.quoteRelationName(relationName1));
				
				
			}
			tableNr++;

			leftJoinExpression.append(" LEFT JOIN ");
			leftJoinExpression.append(this.database.quoteRelationName(this.aliases.originalOf(relationName2)));
			
			if (this.database.dbTypeIs(ConnectedDB.Oracle)) 
			{
				leftJoinExpression.append(" ");
			} else {
				leftJoinExpression.append(" AS ");
			}
			leftJoinExpression.append(this.database.quoteRelationName(relationName2));
			
			leftJoinExpression.append(" ON ");
			// join-conditions
			leftJoinExpression.append(leftJoin.getJoinConditions().toSQL(this.database, this.aliases));
		}
	}
	
	
	public String getSQLStatement() {
		
		StringBuffer result = new StringBuffer("SELECT ");
		if (this.database.limit() != Database.NO_LIMIT) {
			result.append("TOP " + this.database.limit() + " ");
		}
		if (this.eliminateDuplicates && database.allowDistinct()) {
			result.append("DISTINCT ");
		}
		Iterator it = this.selectSpecs.iterator();
		if (!it.hasNext()) {
			result.append("1");
		}
		while (it.hasNext()) {
			ProjectionSpec projection = (ProjectionSpec) it.next();
			result.append(projection.toSQL(database, aliases));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		
		
		result.append(" FROM ");
		it = mentionedTables.iterator();
		while (it.hasNext()) {			
			RelationName tableName = (RelationName) it.next();
			if (this.aliases.isAlias(tableName)) {
				result.append(this.database.quoteRelationName(this.aliases.originalOf(tableName)));
				if (this.database.dbTypeIs(ConnectedDB.Oracle)) {
					result.append(" ");
				} else {
					result.append(" AS ");
				}
			}
			result.append(this.database.quoteRelationName(tableName));
			if (it.hasNext()) {
				result.append(", ");
			}
		
		}
		
		// are some left joins
		if (leftJoinExpression.length() > 0)
		{
			
			if (!this.mentionedTables.isEmpty())
			{
				result.append(", ");
			}
			result.append(leftJoinExpression);
		}
		
		if (!condition().isTrue()) {
			result.append(" WHERE ");
			result.append(condition().toSQL(this.database, this.aliases));
		}
//		if (this.database.limit() != Database.NO_LIMIT) {
//			result.append(" LIMIT " + this.database.limit());
//		}
		
//		System.out.println("SQL-Statement: " + result);
		
		return result.toString();
	}
	
	/**
	 * Returns the projection specs used in this query, in order of appearance 
	 * in the "SELECT x, y, z" part of the query.
	 * 
	 * @return A list of {@link ProjectionSpec}s
	 */
	public List getColumnSpecs() {
		return this.selectSpecs;
	}
	
	/**
	 * Adds a {@link ProjectionSpec} to the SELECT part of the query.
	 * @param projection
	 */
	private void addSelectSpec(ProjectionSpec projection) {
		if (this.selectSpecs.contains(projection)) {
			return;
		}
		Iterator it = projection.requiredAttributes().iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			this.mentionedTables.add(attribute.relationName());
		}
		this.selectSpecs.add(projection);
	}

	private void addCondition(Expression condition) {
		this.conditions.add(condition);
		this.cachedCondition = null;
	}
}
