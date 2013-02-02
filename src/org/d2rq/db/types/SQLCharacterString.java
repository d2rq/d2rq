package org.d2rq.db.types;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Datatype for fixed-length character strings (e.g., CHAR but not VARCHAR).
 * It does not actually store the specific length.
 * 
 * A difference in behaviour
 * from {@link SQLCharacterStringVarying} is that trailing spaces are not
 * reported in results. So, even if the database stores 'AAA ', the resulting
 * value will be 'AAA'. This seems closer to user expectations. Note that for
 * standard SQL and many vendors (but not all), 'AAA '='AAA', so
 * selecting still works. Vendors that do not compare strings in that way
 * should not use this class, or selecting will break.
 */
public class SQLCharacterString extends DataType {
	private final boolean supportsDistinct;
	
	public SQLCharacterString(String name, boolean supportsDistinct) {
		super(name);
		this.supportsDistinct = supportsDistinct;
	}
	
	@Override
	public boolean supportsDistinct() {
		return supportsDistinct;
	}

	@Override
	public String value(ResultSet resultSet, int column) throws SQLException {
		String value = super.value(resultSet, column);
		if (value == null) return null;
		// Strip trailing spaces
		int i = value.length() - 1;
		while (i >= 0 && value.charAt(i) == ' ') {
			i--;
		}
		return value.substring(0, i + 1);
	}
}
