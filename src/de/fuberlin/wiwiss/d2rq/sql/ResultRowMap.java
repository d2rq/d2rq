package de.fuberlin.wiwiss.d2rq.sql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hsqldb.types.Types;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;

/**
 * A result row returned by a database query, presented as a
 * map from SELECT clause entries to string values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ResultRowMap implements ResultRow {
	
	private final static Pattern TIME_PATTERN = 
			Pattern.compile("^\\d?\\d:\\d\\d:\\d\\d(.\\d+)?([+-]\\d?\\d:\\d\\d|Z)?$");
	private final static Pattern TIMESTAMP_PATTERN = 
			Pattern.compile("^\\d+-\\d\\d-\\d\\d \\d?\\d:\\d\\d:\\d\\d(.\\d+)?([+-]\\d?\\d:\\d\\d|Z)?$");
	
	public static ResultRowMap fromResultSet(ResultSet resultSet, List<ProjectionSpec> projectionSpecs) throws SQLException {
		Map<ProjectionSpec,String> result = new HashMap<ProjectionSpec,String>();
		ResultSetMetaData metaData = resultSet.getMetaData();
		
		for (int i = 0; i < projectionSpecs.size(); i++) {
			ProjectionSpec key = projectionSpecs.get(i);
			/*
			 * Return string representations of the values using information from the type map
			 */
			String classString = metaData == null ? null : metaData.getColumnClassName(i + 1);
			int type = metaData == null ? Integer.MIN_VALUE : metaData.getColumnType(i + 1);
			if (type == Types.DOUBLE || type == Types.REAL || type == Types.FLOAT) {
				double d = resultSet.getDouble(i + 1);
				if (resultSet.wasNull()) {
					result.put(projectionSpecs.get(i), null);
				} else {
					if (d == Double.NaN) {
						result.put(key, "NaN");
					} else if (d == Double.POSITIVE_INFINITY) {
						result.put(key, "INF");
					} else if (d == Double.NEGATIVE_INFINITY) {
						result.put(key, "-INF");
					} else {
						String dd = Double.toString(d);
						if (!dd.contains("E")) {
							// Canonical XSD form requires 0.0E0 form
							dd += "E0";
						}
						result.put(key, dd);
					}
				}
			} else if (type == Types.DECIMAL || type == Types.NUMERIC) {
				String num = resultSet.getString(i + 1);
				if (num == null) {
					result.put(key, null);
				} else {
					// Canonical XSD form - no trailing zeros or empty fraction
					while (num.contains(".") && (num.endsWith("0") || num.endsWith("."))) {
						num = num.substring(0, num.length() - 1);
					}
					result.put(key, num);
				}
			} else if (type == Types.BOOLEAN) {
				boolean b = resultSet.getBoolean(i + 1);
				result.put(key, resultSet.wasNull() ? null : (b ? "true" : "false"));
			} else if (type == Types.BLOB || type == Types.BINARY || type == Types.VARBINARY || type == Types.LONGVARBINARY) {
				byte[] bytes = resultSet.getBytes(i + 1);
				result.put(key, resultSet.wasNull() ? null : toHexString(bytes));
			} else if (type == Types.TIME) {
				String time = resultSet.getString(i + 1);
				if (time == null) {
					result.put(key, null);
				} else {
					Matcher m = TIME_PATTERN.matcher(time);
					if (m.matches()) {
						if (time.substring(1, 2).equals(":")) {
							// H:MM:SS format, we need to make it HH:MM:SS
							time = '0' + time;
						}
						int tzStart = Math.max(time.indexOf('-'), time.indexOf('+'));
						if (tzStart > 0 && time.substring(tzStart + 2, tzStart + 3).equals(":")) {
							// Time zone is in H:MM format, we need to make it HH:MM
							time = time.substring(0, tzStart + 1) + '0' + time.substring(tzStart + 1);
						}
						int fractionStart = time.indexOf('.');
						if (fractionStart > 0) {
							// Strip trailing zeros from fraction
							int fractionIndex = fractionStart;
							while (fractionIndex + 1 < time.length() && 
									Character.isDigit(time.charAt(fractionIndex + 1))) {
								fractionIndex++;
							}
							while (time.charAt(fractionIndex) == '0' || time.charAt(fractionIndex) == '.') {
								time = time.substring(0, fractionIndex) + time.substring(fractionIndex + 1);
								fractionIndex--;
								if (fractionIndex < fractionStart) break;
							}
						}
						// Canonical time zone representation
						time = time.replace("+00:00", "Z").replace("-00:00", "Z");
						result.put(key, time);
					} else {
						// getString() didn't return expected format, let's use
						// getTime(), which is supposed to return HH:MM:SS format
						// but may swallow fractional seconds and time zone
						result.put(key, resultSet.getTime(i + 1).toString());
					}
				}
			} else if (type == Types.TIMESTAMP) {
				String timestamp = resultSet.getString(i + 1);
				if (timestamp == null) {
					result.put(key, null);
				} else {
					Matcher m = TIMESTAMP_PATTERN.matcher(timestamp);
					if (m.matches()) {
						// Need at least four digits in year; pad with 0 if necessary
						int yearDigits = timestamp.indexOf('-');
						for (int j = 0; j < 4 - yearDigits; j++) {
							timestamp = '0' + timestamp;
						}
						// Turn space between date and time into 'T' as req. by XSD
						timestamp = timestamp.replace(' ', 'T');
						int timeStart = timestamp.indexOf('T') + 1;
						if (timestamp.substring(timeStart + 1, timeStart + 2).equals(":")) {
							// Time is in H:MM:SS format, we need to make it HH:MM:SS
							timestamp = timestamp.substring(0, timeStart) 
									+ '0' + timestamp.substring(timeStart);
						}
						int tzStart = Math.max(timestamp.indexOf('-', timeStart), 
								timestamp.indexOf('+', timeStart));
						if (tzStart > 0 && timestamp.substring(tzStart + 2, tzStart + 3).equals(":")) {
							// Time zone is in H:MM format, we need to make it HH:MM
							timestamp = timestamp.substring(0, tzStart + 1) + '0' + timestamp.substring(tzStart + 1);
						}
						int fractionStart = timestamp.indexOf('.');
						if (fractionStart > 0) {
							// Strip trailing zeros from fraction
							int fractionIndex = fractionStart;
							while (fractionIndex + 1 < timestamp.length() && 
									Character.isDigit(timestamp.charAt(fractionIndex + 1))) {
								fractionIndex++;
							}
							while (timestamp.charAt(fractionIndex) == '0' || timestamp.charAt(fractionIndex) == '.') {
								timestamp = timestamp.substring(0, fractionIndex) + timestamp.substring(fractionIndex + 1);
								fractionIndex--;
								if (fractionIndex < fractionStart) break;
							}
						}
						// Canonical time zone representation
						timestamp = timestamp.replace("+00:00", "Z").replace("-00:00", "Z");
						result.put(key, timestamp);
					} else {
						// getString() didn't return expected format, let's use
						// getTime(), which is supposed to return
						// yyyy-mm-dd hh:mm:ss.fffffffff format
						// but may swallow time zone
						result.put(key, resultSet.getTimestamp(i + 1).toString().replace(' ', 'T'));
					}
				}
			} else if (type == Types.DATE) {
				Date date = resultSet.getDate(i + 1);
				if (resultSet.wasNull()) {
					result.put(key,  null);
				} else {
					String s = date.toString();
					// Need at least four digits in year; pad with 0 if necessary
					int yearDigits = s.indexOf('-');
					for (int j = 0; j < 4 - yearDigits; j++) {
						s = '0' + s;
					}
					result.put(key, s);
				}
// Oracle BFILE support -- won't compile without Oracle driver on the classpath
// TODO: Move into a separate Java file that is excluded from the default build
				
//			} else if ("oracle.sql.BFILE".equals(classString)) {
//				// TODO Not actually properly tested
//				BFILE bFile = (BFILE) resultSet.getObject(i + 1);
//				if (resultSet.wasNull()) {
//					result.put(key,  null);
//				} else {
//					bFile.openFile();
//					try {
//						InputStream is = bFile.getBinaryStream(i + 1);
//						result.put(key, toHexString(is));
//					} finally {
//						bFile.closeFile();
//					}
//				}
			} else {
				result.put(key, resultSet.getString(i + 1));
			}
		}
		return new ResultRowMap(result);
	}
	
	private static final char[] HEX_DIGITS = {
			'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
		};
	private static String toHexString(byte[] bytes) {
		if (bytes == null) return null;
		final StringBuilder hex = new StringBuilder(2 * bytes.length);
		for (final byte b : bytes) {
			hex.append(HEX_DIGITS[(b & 0xF0) >> 4]);
			hex.append(HEX_DIGITS[b & 0x0F]);
		}
		return hex.toString();
	}

	private static String toHexString(InputStream in) {
		final StringBuilder hex = new StringBuilder();
		try {
			while (true) {
				int b = in.read();
				if (b == -1) break;
				hex.append(HEX_DIGITS[(b & 0xF0) >> 4]);
				hex.append(HEX_DIGITS[b & 0x0F]);
			}
			return hex.toString();

		} catch (IOException ex) {
			throw new D2RQException(ex);
		}
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
