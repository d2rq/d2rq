package org.d2rq.db.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.d2rq.db.vendor.Vendor;

import com.hp.hpl.jena.vocabulary.XSD;


public class SQLBinary extends DataType {
	private final boolean supportsDistinct;
	
	public SQLBinary(String name, boolean supportsDistinct) {
		super(name);
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
		return XSD.hexBinary.getURI();
	}
	@Override
	public String value(ResultSet resultSet, int column) throws SQLException {
		byte[] bytes = resultSet.getBytes(column);
		return resultSet.wasNull() ? null : toHexString(bytes);
	}
	@Override
	public String toSQLLiteral(String value, Vendor vendor) {
		if (!isHexString(value)) {
			log.warn("Unsupported BINARY format: '" + value + "'; treating as NULL");
			return "NULL";
		}
		return vendor.quoteBinaryLiteral(value);
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

	public static boolean isHexString(String s) {
		return HEX_STRING_PATTERN.matcher(s).matches();
	}
	private final static Pattern HEX_STRING_PATTERN = 
		Pattern.compile("^([0-9a-fA-F][0-9a-fA-F])*$");	
}