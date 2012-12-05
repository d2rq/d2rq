package org.d2rq.r2rml;



/**
 * A SQL query is a SELECT query in the SQL language that can be executed 
 * over the input database. The string must conform to the production 
 * "direct select statement: multiple rows" in [SQL2] with an optional 
 * trailing semicolon character and optional surrounding white space 
 * (excluding comments) as defined in [TURTLE]. It must be valid to 
 * execute over the SQL connection. The result of the query execution 
 * must not have duplicate column names. Any columns in the SELECT list 
 * derived by projecting an expression should be named, because otherwise 
 * they cannot be reliably referenced in the rest of the mapping.
 * 
 * Database objects referenced in the SQL query may be qualified with a 
 * catalog or schema name. For any database objects referenced without an 
 * explicit catalog name or schema name, the default catalog and default 
 * schema of the SQL connection are assumed.
 * 
 * @see <a href="http://www.w3.org/TR/r2rml/#dfn-sql-query">R2RML: SQL query</a>
 */
public class SQLQuery extends MappingTerm {

	/**
	 * Always succeeds. Check {@link #isValid()} to see if syntax is ok.
	 * @return <code>null</code> if arg is <code>null</code>
	 */
	public static SQLQuery create(String sql) {
		return sql == null ? null : new SQLQuery(sql);
	}

	private final String sql;

	private SQLQuery(String sql) {
		this.sql = cleanUp(sql);
	}

	/**
	 * "The string must conform to the production "direct select statement:
	 * multiple rows" in [SQL2] with an optional trailing semicolon character 
	 * and optional surrounding white space (excluding comments) as defined in 
	 * [TURTLE]."
	 * 
	 * @see http://www.w3.org/TR/r2rml/#dfn-sql-query
	 */
	private String cleanUp(String sql) {
		sql = sql.trim();
		return sql.endsWith(";") ? sql.substring(0, sql.length() - 1) : sql;
	}
	
	@Override
	public String toString() {
		return sql;
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitTerm(this);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SQLQuery)) return false;
		return sql.equals(((SQLQuery) other).sql);
	}
	
	@Override
	public int hashCode() {
		return sql.hashCode() ^ 19673;
	}
}
