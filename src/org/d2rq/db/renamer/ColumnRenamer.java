package org.d2rq.db.renamer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.d2rq.db.op.NamedOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.TableName;




/**
 * A {@link Renamer} based on a fixed map of
 * original and replacement columns.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ColumnRenamer extends Renamer {
	private final Map<ColumnName,ColumnName> original2NewColumns;
	
	public ColumnRenamer(Map<ColumnName,ColumnName> columns) {
		original2NewColumns = columns;
	}
	
	public ColumnName applyTo(ColumnName original) {
		return original2NewColumns.containsKey(original) ? 
				original2NewColumns.get(original) : original;
	}

	public NamedOp applyTo(NamedOp table) {
		return table;
	}

	public TableName applyTo(TableName table) {
		return table;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("ColumnRenamerMap(");
		List<ColumnName> columns = new ArrayList<ColumnName>(original2NewColumns.keySet());
		Collections.sort(columns);
		Iterator<ColumnName> it = columns.iterator();
		while (it.hasNext()) {
			ColumnName column = it.next();
			result.append(column);
			result.append(" => ");
			result.append(original2NewColumns.get(column));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}
}
