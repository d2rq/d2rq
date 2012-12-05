package de.fuberlin.wiwiss.d2rq.sql.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class SQLTimestamp extends DataType {
	private final static Pattern TIMESTAMP_PATTERN = 
		Pattern.compile("^\\d?\\d?\\d?\\d-\\d\\d-\\d\\d \\d?\\d:\\d\\d:\\d\\d(.\\d+)?([+-]\\d?\\d:\\d\\d|Z)?$");
	public SQLTimestamp(Vendor syntax, String name) {
		super(syntax, name);
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String rdfType() {
		return "xsd:dateTime";
	}
	@Override
	public String value(ResultSet resultSet, int column) throws SQLException {
		String timestamp = resultSet.getString(column);
		if (timestamp == null || resultSet.wasNull()) return null;
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
			return timestamp;
		} else {
			// getString() didn't return expected format, let's use
			// getTimestamp(), which is supposed to return
			// yyyy-mm-dd hh:mm:ss.fffffffff format
			// but may swallow time zone
			return resultSet.getTimestamp(column).toString().replace(' ', 'T');
		}
	}
	@Override
	public String toSQLLiteral(String value) {
		value = value.replace('T', ' ').replace("Z", "+00:00");
		if (!TIMESTAMP_PATTERN.matcher(value).matches()) {
			log.warn("Unsupported TIMESTAMP format: '" + value + "'; treating as NULL");
			return "NULL";
		}
		return syntax().quoteTimestampLiteral(value);
	}
}