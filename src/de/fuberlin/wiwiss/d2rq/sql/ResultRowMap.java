package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.map.Column;

/**
 * A result row returned by a database query, presented as a
 * map from columns to string values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ResultRowMap.java,v 1.1 2006/09/09 23:25:16 cyganiak Exp $
 */
public class ResultRowMap implements ResultRow {
	
	public static ResultRowMap fromResultSet(ResultSet resultSet, List columns) throws SQLException {
		Map result = new HashMap();
		for (int i = 0; i < columns.size(); i++) {
			result.put(columns.get(i), resultSet.getString(i + 1));
		}
		return new ResultRowMap(result);
	}
	
	private Map columnsToValues;
	
	public ResultRowMap(Map columnsToValues) {
		this.columnsToValues = columnsToValues;
	}
	
	public String get(Column column) {
		return (String) this.columnsToValues.get(column);
	}

	public String toString() {
		List columns = new ArrayList(this.columnsToValues.keySet());
		Collections.sort(columns);
		StringBuffer result = new StringBuffer("{");
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			result.append(column.getQualifiedName());
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
