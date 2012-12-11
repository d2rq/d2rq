package org.d2rq.mapgen;

import java.util.HashMap;
import java.util.Map;

import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableName;

/**
 * Returns string representations of various objects, guaranteeing that
 * different objects receive different string representations. The
 * class attempts to generate string representations that can be used
 * as local names in Turtle/SPARQL prefixed names, but this is not guaranteed.
 */
public class UniqueLocalNameGenerator {
	private final Map<String,Object> assignedNames = 
			new HashMap<String,Object>();

	/**
	 * Returns SCHEMA_TABLE. Except if that string is already taken
	 * by another table name (or column name); in that case we add
	 * more underscores until we have no clash.
	 */
	public String toString(TableName tableName) {
		if (tableName.getSchema() == null) {
			return tableName.getTable().getName();
		}
		String separator = "_";
		while (true) {
			String candidate = tableName.getSchema().getName() + separator + tableName.getTable().getName();
			if (!assignedNames.containsKey(candidate)) {
				assignedNames.put(candidate, tableName);
				return candidate;
			}
			if (assignedNames.get(candidate).equals(tableName)) {
				return candidate;
			}
			separator += "_";
		}
	}
	
	/**
	 * Returns TABLE_COLUMN. Except if that string is already taken by
	 * another column name (e.g., AAA.BBB_CCC and AAA_BBB.CCC would
	 * result in the same result AAA_BBB_CCC); in that case we add more
	 * underscores (AAA__BBB_CCC) until we have no clash. 
	 */
	public String toString(TableName tableName, Identifier column) {
		String separator = "_";
		while (true) {
			String candidate = toString(tableName) + separator + column.getName();
			if (!assignedNames.containsKey(candidate)) {
				assignedNames.put(candidate, ColumnName.create(tableName, column));
				return candidate;
			}
			if (assignedNames.get(candidate).equals(ColumnName.create(tableName, column))) {
				return candidate;
			}
			separator += "_";
		}
	}

	// TODO: UniqueNameGenerator.toString(TableName, Key) does not yet guarantee uniqueness
	public String toString(TableName tableName, Key columns) {
		StringBuffer result = new StringBuffer();
		result.append(toString(tableName));
		for (Identifier column: columns.getColumns()) {
			result.append("_" + column.getName());
		}
		return result.toString();
	}
}
