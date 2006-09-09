package de.fuberlin.wiwiss.d2rq.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.fuberlin.wiwiss.d2rq.D2RQException;

/**
 * A {@link ColumnRenamer} based on a fixed map of
 * original and replacement columns.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ColumnRenamerMap.java,v 1.1 2006/09/09 15:40:02 cyganiak Exp $
 */
public class ColumnRenamerMap extends ColumnRenamer {
	private Map originalsToReplacements;
	
	public ColumnRenamerMap(Map originalsToReplacements) {
		this.originalsToReplacements = originalsToReplacements;
	}
	
	public Column applyTo(Column original) {
		if (this.originalsToReplacements.containsKey(original)) {
			return (Column) this.originalsToReplacements.get(original);
		}
		return original;
	}
	
	public Map applyToMapKeys(Map mapWithColumnKeys) {
		Map result = new HashMap();
		Iterator it = mapWithColumnKeys.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			Column originalColumn = (Column) entry.getKey();
			Object originalValue = entry.getValue();
			Column replacedColumn = applyTo(originalColumn);
			if (result.containsKey(replacedColumn)
					&& !originalValue.equals(result.get(replacedColumn))) {
				return null;
			}
			result.put(applyTo(originalColumn), entry.getValue());
		}
		return result;
	}
	
	public AliasMap applyTo(AliasMap aliases) {
		return aliases;
	}
	
	public Map withOriginalKeys(Map columnNamesToValues) {
		Map result = new HashMap(columnNamesToValues);
		Iterator it = this.originalsToReplacements.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			Column originalColumn = (Column) entry.getKey();
			Column replacedColumn = (Column) entry.getValue();
			if (result.containsKey(originalColumn)) {
				throw new D2RQException("Conflicting values while restoring original keys; " +
						"data was: " + columnNamesToValues + "; ColumnReplacer was: " + this);
			}
			Object value = columnNamesToValues.get(replacedColumn.getQualifiedName());
			if (value != null) {
				result.put(originalColumn.getQualifiedName(), value);
			}
		}
		return result;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("ColumnRenamerMap(");
		List columns = new ArrayList(this.originalsToReplacements.keySet());
		Collections.sort(columns);
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			result.append(column.getQualifiedName());
			result.append(" => ");
			result.append(((Column) this.originalsToReplacements.get(column)).getQualifiedName());
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}
}
