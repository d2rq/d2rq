package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
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
 * @version $Id: ResultRowMap.java,v 1.7 2010/08/13 13:45:29 cyganiak Exp $
 */
public class ResultRowMap implements ResultRow {
	
	public static ResultRowMap fromResultSet(ResultSet resultSet, List projectionSpecs) throws SQLException {
		Map result = new HashMap();
		ResultSetMetaData metaData = resultSet.getMetaData();
		
		for (int i = 0; i < projectionSpecs.size(); i++) {
			/*
			 * Return string representations of the values using information from the type map
			 * 
			 * TODO Generally use resultSet.getObject(i+1).toString() instead of resultSet.getString(i+1); maybe even map to Objects instead of Strings?
			 * This would convert at JDBC/Java level, which will likely differ from current data, so it's probably best to keep things as they are for now  
			 */
			if (metaData != null) {
				String classString = metaData.getColumnClassName(i + 1);
				/*
				 * Specifically handle Oracle DATEs and TIMESTAMPs because the regular getString()
				 * returns them in non-standard fashion, e.g. "2008-3-22.0.0. 0. 0".
				 * This occurs independently of the NLS_DATE_FORMAT / NLS_TIMESTAMP_FORMAT in use.
				 * 
				 * Note: getObject(i+1).toString() does not work in this case; the only other options seems to be Oracle's toJdbc()
				 */
				if (classString != null && classString.equals("oracle.sql.DATE")) {
					Date oracleDate = resultSet.getDate(i + 1);
					if (!resultSet.wasNull()) {
						result.put(projectionSpecs.get(i), oracleDate.toString());
					} else {
						result.put(projectionSpecs.get(i), null);
					}
				} else if (classString != null && classString.equals("oracle.sql.TIMESTAMP")) {
					Timestamp oracleTimestamp = resultSet.getTimestamp(i + 1);
					if (!resultSet.wasNull()) {
						result.put(projectionSpecs.get(i), oracleTimestamp.toString());
					} else {
						result.put(projectionSpecs.get(i), null);
					}
				}
				/*
				 * Let the JDBC driver convert boolean values for us as their representation differs greatly amongst DBs (e.g. PostgreSQL employs 't' and 'f', others use 0 and 1) 
				 */
				else if (classString != null && classString.equals("java.lang.Boolean"))
					result.put(projectionSpecs.get(i), Boolean.toString(resultSet.getBoolean(i + 1)));
				else {
					 /* 
					  * Return native string representation of the object
					  */
					result.put(projectionSpecs.get(i), resultSet.getString(i + 1));
				}
			}
			else
				result.put(projectionSpecs.get(i), resultSet.getString(i + 1)); /* treat everything as String if no type map is available */
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
