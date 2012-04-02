package de.fuberlin.wiwiss.d2rq.sql.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.expr.SQLExpression;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLApproximateNumeric;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLBinary;
import de.fuberlin.wiwiss.d2rq.sql.types.SQLCharacterString;
import de.fuberlin.wiwiss.d2rq.sql.types.UnsupportedDataType;

/**
 * This syntax class implements MySQL-compatible SQL syntax.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Oracle extends SQL92 {

	public Oracle() {
		super(false);
	}
	
	@Override
	public Expression getRowNumLimitAsExpression(int limit) {
		if (limit == Database.NO_LIMIT) return Expression.TRUE;
		return SQLExpression.create("ROWNUM <= " + limit);
	}

	@Override
	public String getRowNumLimitAsQueryAppendage(int limit) {
		return "";
	}
	
	@Override
	public String quoteBinaryLiteral(String hexString) {
		return quoteStringLiteral(hexString);
	}

	@Override
	public DataType getDataType(int jdbcType, String name, int size) {
		
		// Doesn't support DISTINCT over LOB types
		if (jdbcType == Types.CLOB || "NCLOB".equals(name)) {
			return new SQLCharacterString(this, false);
		}
		if (jdbcType == Types.BLOB) {
			return new SQLBinary(this, false);
		}
		
		DataType standard = super.getDataType(jdbcType, name, size);
		if (standard != null) return standard;

		// Oracle-specific character string types
		if ("VARCHAR2".equals(name) || "NVARCHAR2".equals(name)) {
			return new SQLCharacterString(this, true);
		}

		// Oracle-specific floating point types
    	if ("BINARY_FLOAT".equals(name) || "BINARY_DOUBLE".equals(name)) {
    		return new SQLApproximateNumeric(this);
    	}
    	
		// Oracle binary file pointer
		// TODO: We could at least support reading from BFILE, although querying for them seems hard
    	if ("BFILE".equals(name)) {
    		return new UnsupportedDataType(jdbcType, name);
    	}

    	return null;
	}

	@Override
	public void initializeConnection(Connection connection) throws SQLException {
		// Set Oracle date formats 
		Statement stmt = connection.createStatement();
		try {
			stmt.execute("ALTER SESSION SET NLS_DATE_FORMAT = 'SYYYY-MM-DD'");
			stmt.execute("ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'SYYYY-MM-DD HH24:MI:SS'");
		} finally {
			stmt.close();
		}
	}

	private static final String[] IGNORED_SCHEMAS = {
		"CTXSYS", "EXFSYS", "FLOWS_030000", "MDSYS", "OLAPSYS", "ORDSYS", 
		"SYS", "SYSTEM", "WKSYS", "WK_TEST", "WMSYS", "XDB"};
	@Override
	public boolean isIgnoredTable(String schema, String table) {
		// Skip Oracle system schemas as well as deleted tables in Oracle's Recycling Bin.
		// The latter have names like MYSCHEMA.BIN$FoHqtx6aQ4mBaMQmlTCPTQ==$0
		return Arrays.binarySearch(IGNORED_SCHEMAS, schema) >= 0 
				|| table.startsWith("BIN$");
	}
}
