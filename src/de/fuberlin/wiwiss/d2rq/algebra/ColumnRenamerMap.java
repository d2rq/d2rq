package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * A {@link ColumnRenamer} based on a fixed map of
 * original and replacement columns.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ColumnRenamerMap.java,v 1.2 2007/10/22 10:21:16 cyganiak Exp $
 */
public class ColumnRenamerMap extends ColumnRenamer {
	private Map originalsToReplacements;
	
	public ColumnRenamerMap(Map originalsToReplacements) {
		this.originalsToReplacements = originalsToReplacements;
	}
	
	public Attribute applyTo(Attribute original) {
		if (this.originalsToReplacements.containsKey(original)) {
			return (Attribute) this.originalsToReplacements.get(original);
		}
		return original;
	}

	public AliasMap applyTo(AliasMap aliases) {
		return aliases;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("ColumnRenamerMap(");
		List columns = new ArrayList(this.originalsToReplacements.keySet());
		Collections.sort(columns);
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			result.append(column.qualifiedName());
			result.append(" => ");
			result.append(((Attribute) this.originalsToReplacements.get(column)).qualifiedName());
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}
}
