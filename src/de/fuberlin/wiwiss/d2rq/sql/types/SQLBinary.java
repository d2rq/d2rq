package de.fuberlin.wiwiss.d2rq.sql.types;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.fuberlin.wiwiss.d2rq.sql.SQL;
import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class SQLBinary extends DataType {
	private final boolean supportsDistinct;
	
	public SQLBinary(Vendor syntax, String name, boolean supportsDistinct) {
		super(syntax, name);
		this.supportsDistinct = supportsDistinct;
	}

	@Override
	public boolean isIRISafe() {
		return true;
	}
	
	@Override
	public boolean supportsDistinct() {
		return supportsDistinct;
	}
	
	@Override
	public String rdfType() {
		return "xsd:hexBinary";
	}
	@Override
	public String value(ResultSet resultSet, int column) throws SQLException {
		byte[] bytes = resultSet.getBytes(column);
		return resultSet.wasNull() ? null : toHexString(bytes);
	}
	@Override
	public String toSQLLiteral(String value) {
		if (!SQL.isHexString(value)) {
			log.warn("Unsupported BINARY format: '" + value + "'; treating as NULL");
			return "NULL";
		}
		return syntax().quoteBinaryLiteral(value);
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
}