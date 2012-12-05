package org.d2rq.r2rml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.vocab.RR;


public abstract class LogicalTable extends MappingComponent {

	public static class R2RMLView extends LogicalTable {
		private SQLQuery sqlQuery = null;
		private Set<ConstantIRI> sqlVersions = new HashSet<ConstantIRI>();
		public ComponentType getType() {
			return ComponentType.R2RML_VIEW;
		}
		public void setSQLQuery(SQLQuery sqlQuery) {
			this.sqlQuery = sqlQuery;
		}
		public SQLQuery getSQLQuery() {
			return sqlQuery;
		}
		public Set<ConstantIRI> getSQLVersions() {
			return sqlVersions;
		}
		public String getEffectiveSQLQuery() {
			return sqlQuery.toString();
		}
		@Override
		public List<Identifier> getColumns(SQLConnection connection) {
			if (sqlQuery == null || !sqlQuery.isValid(connection)) return null;
			SQLOp sqlOp = connection.getSelectStatement(sqlQuery.toString());
			if (sqlOp == null) return null;
			List<Identifier> result = new ArrayList<Identifier>();
			for (ColumnName column: sqlOp.getColumns()) {
				result.add(column.getColumn());
			}
			return result;
		}
		@Override
		public void accept(MappingVisitor visitor) {
			super.accept(visitor);
			visitor.visitComponent(this);
			visitor.visitTermProperty(RR.sqlQuery, sqlQuery);
			for (ConstantIRI sqlVersion: sqlVersions) {
				visitor.visitTermProperty(RR.sqlVersion, sqlVersion);
			}
		}
	}
	
	public static class BaseTableOrView extends LogicalTable {
		private TableOrViewName tableName = null;
		public ComponentType getType() {
			return ComponentType.BASE_TABLE_OR_VIEW;
		}
		public void setTableName(TableOrViewName tableName) {
			this.tableName = tableName;
		}
		public TableOrViewName getTableName() {
			return tableName;
		}
		public String getEffectiveSQLQuery() {
			return "SELECT * FROM " + tableName;
		}
		@Override
		public List<Identifier> getColumns(SQLConnection connection) {
			if (tableName == null || !tableName.isValid(connection)) return null;
			TableOp table = connection.getTable(tableName.asQualifiedTableName());
			if (table == null) return null;
			List<Identifier> result = new ArrayList<Identifier>();
			for (ColumnName column: table.getColumns()) {
				result.add(column.getColumn());
			}
			return result;
		}
		@Override
		public void accept(MappingVisitor visitor) {
			super.accept(visitor);
			visitor.visitComponent(this);
			visitor.visitTermProperty(RR.tableName, tableName);
		}
	}
	
	public abstract String getEffectiveSQLQuery();
	
	public abstract List<Identifier> getColumns(SQLConnection connection);
	
	public void accept(MappingVisitor visitor) {
		visitor.visitComponent(this);
	}
}
