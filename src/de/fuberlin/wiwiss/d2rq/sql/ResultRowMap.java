package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;

/**
 * A result row returned by a database query, presented as a
 * map from SELECT clause entries to string values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ResultRowMap.java,v 1.4 2009/02/19 00:54:17 fatorange Exp $
 */
public class ResultRowMap implements ResultRow {
	
	public static ResultRowMap fromResultSet(ResultSet resultSet, List projectionSpecs) throws SQLException {
		Map result = new HashMap();
		for (int i = 0; i < projectionSpecs.size(); i++) {
			/*
			 * Specifically handle Oracle DATEs and TIMESTAMPs because the regular getString()
			 * returns them in non-standard fashion, e.g. "2008-3-22.0.0. 0. 0".
			 * This occurs independently of the NLS_DATE_FORMAT / NLS_TIMESTAMP_FORMAT in use.
			 */
			Object resultObj = resultSet.getObject(i + 1);
			if (resultObj instanceof oracle.sql.DATE)
				result.put(projectionSpecs.get(i), resultSet.getDate(i + 1).toString());
			else if (resultObj instanceof oracle.sql.TIMESTAMP)
				result.put(projectionSpecs.get(i), resultSet.getTimestamp(i + 1).toString());
			else
				result.put(projectionSpecs.get(i), resultSet.getString(i + 1));
		}
		return new ResultRowMap(result);
	}
	
	private Map projectionsToValues;
	
	public ResultRowMap(Map projectionsToValues) {
		this.projectionsToValues = projectionsToValues;
	}
	
	public String get(ProjectionSpec projection) {
		return (String) this.projectionsToValues.get(projection);
	}

	public String toString() {
		List columns = new ArrayList(this.projectionsToValues.keySet());
		Collections.sort(columns);
		StringBuffer result = new StringBuffer("{");
		Iterator it = columns.iterator();
		while (it.hasNext()) {
			ProjectionSpec projection = (ProjectionSpec) it.next();
			result.append(projection.toString());
			result.append(" => '");
			result.append(this.projectionsToValues.get(projection));
			result.append("'");
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append("}");
		return result.toString();
	}
}
