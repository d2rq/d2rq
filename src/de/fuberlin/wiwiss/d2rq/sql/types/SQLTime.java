package de.fuberlin.wiwiss.d2rq.sql.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class SQLTime extends DataType {
	private final static Pattern TIME_PATTERN = 
		Pattern.compile("^\\d?\\d:\\d\\d:\\d\\d(.\\d+)?([+-]\\d?\\d:\\d\\d|Z)?$");
	public SQLTime(Vendor syntax, String name) {
		super(syntax, name);
	}
	@Override
	public boolean isIRISafe() {
		return true;
	}
	@Override
	public String rdfType() {
		return "xsd:time";
	}
	@Override
	public String value(ResultSet resultSet, int column) throws SQLException {
		String time = resultSet.getString(column);
		if (time == null || resultSet.wasNull()) return null;
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
			return time;
		} else {
			// getString() didn't return expected format, let's use
			// getTime(), which is supposed to return HH:MM:SS format
			// but may swallow fractional seconds and time zone
			return resultSet.getTime(column).toString();
		}
	}
	@Override
	public String toSQLLiteral(String value) {
		value = value.replace("Z", "+00:00");
		if (!TIME_PATTERN.matcher(value).matches()) {
			log.warn("Unsupported TIME format: '" + value + "'; treating as NULL");
			return "NULL";
		}
		return syntax().quoteTimeLiteral(value);
	}
}