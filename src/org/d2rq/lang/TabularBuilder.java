package org.d2rq.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.d2rq.D2RQException;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.expr.ColumnExpr;
import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.AssertUniqueKeyOp;
import org.d2rq.db.op.DistinctOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.OrderOp;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.renamer.TableRenamer;
import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType.GenericType;


/**
 * Builder for the {@link DatabaseOp}s corresponding to various mapping
 * constructs in the D2RQ mapping language.
 * 
 * TODO: isUnique is not properly handled yet
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TabularBuilder {
	private final SQLConnection sqlConnection;
	private final Map<ColumnName,GenericType> overriddenColumnTypes;
	private Set<TableName> mentionedTableNames = new HashSet<TableName>();
	private Expression condition = Expression.TRUE;
	private Set<ColumnListEquality> joinConditions = new HashSet<ColumnListEquality>();
	private Map<ForeignKey,TableName> assertedForeignKeys = new HashMap<ForeignKey,TableName>();
	private Map<TableName,AliasDeclaration> aliasDeclarations = new HashMap<TableName,AliasDeclaration>();
	private final List<ProjectionSpec> projections = new ArrayList<ProjectionSpec>();
	private boolean containsDuplicates = true;
	private ColumnName orderColumn = null;
	private boolean orderDesc = false;
	private int limit = LimitOp.NO_LIMIT;
	private int limitInverse = LimitOp.NO_LIMIT;
		
	public TabularBuilder(SQLConnection database, 
			Map<ColumnName,GenericType> overriddenColumnTypes) {
		this.sqlConnection = database;
		this.overriddenColumnTypes = overriddenColumnTypes;
	}
	
	public void addCondition(Expression expression) {
		condition = condition.and(expression);
		registerColumns(expression.getColumns());
	}
	
	public void addCondition(String condition) {
		addCondition(Microsyntax.parseSQLExpression(condition, GenericType.BOOLEAN));
	}
	
	public void addConditions(Collection<String> conditions) {
		for (String condition: conditions) {
			addCondition(condition);
		}
	}

	public void addAliasDeclaration(AliasDeclaration declaration) {
		aliasDeclarations.put(declaration.getAlias(), declaration);
	}
	
	public void addAliasDeclarations(Collection<AliasDeclaration> declarations) {
		for (AliasDeclaration aliasDeclaration: declarations) {
			addAliasDeclaration(aliasDeclaration);
		}
	}
	
	public void addJoins(Collection<ColumnListEquality> joins) {
		joinConditions.addAll(joins);
		for (ColumnListEquality join: joins) {
			registerTable(join.getTableName1());
			registerTable(join.getTableName2());
		}
	}
	
	public void addJoinExpressions(Collection<Join> joinExpressions) {
		JoinSetParser parsedJoins = JoinSetParser.create(joinExpressions);
		addJoins(parsedJoins.getExpressions());
		assertedForeignKeys.putAll(parsedJoins.getAssertedForeignKeys());
	}
	
	public void addProjection(ProjectionSpec projection) {
		if (projections.contains(projection)) return;
		projections.add(projection);
		registerTables(projection.getTableNames());
		registerColumn(projection.getColumn());
	}
	
	public void addProjections(Collection<ProjectionSpec> projections) {
		for (ProjectionSpec projection: projections) {
			addProjection(projection);
		}
	}
	
	public void addRelationBuilder(TabularBuilder other) {
		addCondition(other.condition);
		addJoins(other.joinConditions);
		assertedForeignKeys.putAll(other.assertedForeignKeys);
		addAliasDeclarations(other.aliasDeclarations.values());
		addProjections(other.projections);
		setContainsDuplicates(containsDuplicates && other.containsDuplicates);
		if (other.orderColumn != null) {
			// Overwrite our ordering if the other builder is ordered 
			setOrderColumn(other.orderColumn);
			setOrderDesc(other.orderDesc);
		}
		setLimit(LimitOp.combineLimits(limit, other.limit));
		setLimitInverse(LimitOp.combineLimits(limitInverse, other.limitInverse));
	}
	
	/**
	 * Adds information from another relation builder to this one,
	 * applying this builder's alias mappings to the other one.
	 *  
	 * @param other A relation builder that potentially uses aliases declared in this builder
	 */
	public void addAliasedRelationBuilder(TabularBuilder other) {
		Renamer renamer = TableRenamer.create(createAliases(aliasDeclarations));
		addCondition(renamer.applyTo(other.condition));
		addJoins(renamer.applyToJoinConditions(other.joinConditions));
		for (Entry<ForeignKey,TableName> entry: other.assertedForeignKeys.entrySet()) {
			assertedForeignKeys.put(
					renamer.applyTo(entry.getValue(), entry.getKey()), 
					entry.getValue());
		}
		addProjections(renamer.applyToProjections(other.projections));
		if (other.orderColumn != null) {
			// Overwrite our ordering if the other builder is ordered 
			setOrderColumn(renamer.applyTo(other.orderColumn));
			setOrderDesc(other.orderDesc);
		}
		
		// Some of our aliases may exist solely to rename aliases in the other
		// builder. Remove these, as their original is not an actual table
		// but an alias. 
		Map<TableName,TableName> otherAliases = createAliases(other.aliasDeclarations);
		Collection<TableName> removedAliases = new ArrayList<TableName>();
		for (AliasDeclaration declaration: aliasDeclarations.values()) {
			for (TableName otherAliasOriginal: otherAliases.keySet()) {
				if (otherAliasOriginal.equals(declaration.getOriginal())) {
					removedAliases.add(declaration.getAlias());
				}
			}
		}
		for (TableName alias: removedAliases) {
			aliasDeclarations.remove(alias);
		}
		for (AliasDeclaration declaration: other.aliasDeclarations.values()) {
			addAliasDeclaration(new AliasDeclaration(declaration.getOriginal(), renamer.applyTo(declaration.getAlias())));
		}
		limit = LimitOp.combineLimits(limit, other.limit);
		limitInverse = LimitOp.combineLimits(limitInverse, other.limitInverse);
	}
	
	public void setContainsDuplicates(boolean containsDuplicates) {
		this.containsDuplicates = containsDuplicates;
	}
	
	public void setOrderColumn(ColumnName column) {
		this.orderColumn = column;
		registerColumn(column);
	}
	
	public void setOrderDesc(boolean desc) {
		this.orderDesc = desc;
	}
	
	public void setLimit(int limit) {
	    this.limit = limit;
	}
	
	public void setLimitInverse(int limitInverse) {
	    this.limitInverse = limitInverse;
	}
	
	private NamedOp getBaseTableOrAlias(TableName name) {
		if (!aliasDeclarations.containsKey(name)) {
			return getBaseTable(name);
		}
		return AliasOp.create(
				getBaseTable(aliasDeclarations.get(name).getOriginal()), name);
	}
	
	private NamedOp getBaseTable(TableName name) {
		TableOp table = sqlConnection.getTable(name);
		if (table == null) {
			throw new D2RQException("Table used in mapping not found: " + name,
					D2RQException.SQL_TABLE_NOT_FOUND);
		}
		return overrideColumnTypes(assertForeignKeys(name, table));
	}
	
	private TableOp assertForeignKeys(TableName name, TableOp from) {
		for (Entry<ForeignKey,TableName> entry: assertedForeignKeys.entrySet()) {
			if (!entry.getValue().equals(name)) continue;
			TableDef old = from.getTableDefinition();
			Set<ForeignKey> newForeignKeys = new HashSet<ForeignKey>(old.getForeignKeys());
			newForeignKeys.add(entry.getKey());
			TableDef newTableDef = new TableDef(old.getName(), old.getColumns(),
					old.getPrimaryKey(), old.getUniqueKeys(), newForeignKeys);
			from = new TableOp(newTableDef);
		}
		return from;
	}
	
	private TableOp overrideColumnTypes(TableOp original) {
		List<ColumnDef> newColumns = new ArrayList<ColumnDef>();
		for (ColumnDef column: original.getTableDefinition().getColumns()) {
			ColumnName name = original.getTableName().qualifyIdentifier(column.getName());
			if (overriddenColumnTypes.containsKey(name)) {
				newColumns.add(new ColumnDef(
						column.getName(), 
						overriddenColumnTypes.get(name).dataTypeFor(sqlConnection.vendor()), 
						column.isNullable()));
			} else {
				newColumns.add(column);
			}
		}
		return new TableOp(new TableDef(
				original.getTableName(), 
				newColumns, 
				original.getTableDefinition().getPrimaryKey(), 
				original.getTableDefinition().getUniqueKeys(),
				original.getTableDefinition().getForeignKeys()));
	}
	
	public DatabaseOp getTabular() {
		Collection<NamedOp> tables = new ArrayList<NamedOp>();
		for (TableName name: mentionedTableNames) {
			NamedOp t = getBaseTableOrAlias(name);
			if (t == null) {
				throw new D2RQException("No such table or d2rq:alias: " + name, D2RQException.SQL_TABLE_NOT_FOUND);
			}
			tables.add(t);
		}
		DatabaseOp result = InnerJoinOp.join(tables, joinConditions);
		result = SelectOp.select(result, condition);
		if (orderColumn != null) {
			List<OrderSpec> order = Collections.<OrderSpec>singletonList(
					new OrderSpec(new ColumnExpr(orderColumn), orderDesc));
			result = new OrderOp(order, result);
		}
		result = ProjectOp.create(result, projections);
		if (!containsDuplicates && result.getUniqueKeys().isEmpty()) {
			List<Identifier> columns = new ArrayList<Identifier>();
			for (ProjectionSpec projection: projections) {
				if (columns.contains(projection.getColumn())) continue;
				columns.add(projection.getColumn().getColumn());
			}
			result = new AssertUniqueKeyOp(result, Key.createFromIdentifiers(columns));
		}
		if (result.getUniqueKeys().isEmpty()) {
			result = new DistinctOp(result);
		}
		result = LimitOp.limit(result, limit, limitInverse);
		return result;
	}
	
	private void registerColumns(Collection<? extends ColumnName> columnNames) {
		for (ColumnName columnName: columnNames) {
			registerColumn(columnName);
		}
	}
	
	private void registerColumn(ColumnName columnName) {
		registerTable(columnName.getQualifier());
	}

	private void registerTables(Collection<TableName> tables) {
		mentionedTableNames.addAll(tables);
	}
	
	private void registerTable(TableName table) {
		if (table == null) return;
		mentionedTableNames.add(table);
	}
	
	private Map<TableName,TableName> createAliases(Map<TableName,AliasDeclaration> declarations) {
		Map<TableName,TableName> result = new HashMap<TableName,TableName>();
		for (AliasDeclaration alias: declarations.values()) {
			result.put(alias.getOriginal(), alias.getAlias());
		}
		return result;
	}
}