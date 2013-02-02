package org.d2rq.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;


/**
 * A result row returned by a database query, presented as a
 * map from SELECT clause entries to string values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ResultRow {
	public static final ResultRow NO_ATTRIBUTES = 
			new ResultRow(Collections.<ColumnName,String>emptyMap());

	public static ResultRow createOne(ColumnName column, String value) {
		return new ResultRow(Collections.singletonMap(column, value));
	}
	
	public static ResultRow fromResultSet(ResultSet resultSet, 
			ColumnList columns, SQLConnection database) 
	throws SQLException {
		Map<ColumnName,String> result = new HashMap<ColumnName,String>();
		ResultSetMetaData metaData = resultSet.getMetaData();
		for (int i = 0; i < columns.size(); i++) {
			ColumnName key = columns.get(i);
			int jdbcType = metaData == null ? Integer.MIN_VALUE : metaData.getColumnType(i + 1);
			String name = metaData == null ? "UNKNOWN" : metaData.getColumnTypeName(i + 1);
			result.put(key, database.vendor().getDataType(jdbcType, name.toUpperCase(), -1).value(resultSet, i + 1));
		}
		return new ResultRow(result);
	}
	
	private final Map<ColumnName,String> columnsToValues;
	
	public ResultRow(Map<ColumnName,String> columnsToValues) {
		this.columnsToValues = columnsToValues;
	}
	
	public String get(ColumnName column) {
		return (String) this.columnsToValues.get(column);
	}

	public String toString() {
		List<ColumnName> columns = new ArrayList<ColumnName>(this.columnsToValues.keySet());
		Collections.sort(columns);
		StringBuffer result = new StringBuffer("{");
		Iterator<ColumnName> it = columns.iterator();
		while (it.hasNext()) {
			ColumnName column = (ColumnName) it.next();
			result.append(column.toString());
			result.append(" => '");
			result.append(this.columnsToValues.get(column));
			result.append("'");
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append("}");
		return result.toString();
	}
}
