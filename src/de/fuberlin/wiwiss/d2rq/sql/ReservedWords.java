package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A list of reserved words in SQL. Used to decide if identifiers
 * should be quoted or not. We could of course quote all identifiers
 * always, but that would produce ugly SQL and at this stage of
 * the project, somewhat readable SQL seems important to me. I guess
 * this should be made configurable or removed completely at a
 * later stage (say mid-2007 ;-)
 * 
 * The current list contains:
 *
 * All SQL-99 reserved words, taken from
 * http://www.ncb.ernet.in/education/modules/dbms/SQL99/ansi-iso-9075-2-1999.pdf
 * 
 * All MySQL 5.0 reserved words, taken from
 * http://dev.mysql.com/doc/refman/5.0/en/reserved-words.html
 *  
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ReservedWords.java,v 1.1 2006/08/30 19:32:35 cyganiak Exp $
 */
public class ReservedWords {

	private static Set reservedWords = new HashSet(Arrays.asList(new String[]{
			"ABSOLUTE",    // SQL-99
			"ACTION",    // SQL-99
			"ADD",    // SQL-99
			"ADMIN",    // SQL-99
			"AFTER",    // SQL-99
			"AGGREGATE",    // SQL-99
			"ALIAS",    // SQL-99
			"ALL",    // SQL-99
			"ALLOCATE",    // SQL-99
			"ALTER",    // SQL-99
			"ANALYZE",    // MySQL 5.0
			"AND",    // SQL-99
			"ANY",    // SQL-99
			"ARE",    // SQL-99
			"ARRAY",    // SQL-99
			"AS",    // SQL-99
			"ASC",    // SQL-99
			"ASENSITIVE",    // MySQL 5.0
			"ASSERTION",    // SQL-99
			"AT",    // SQL-99
			"AUTHORIZATION",    // SQL-99
			"BEFORE",    // SQL-99
			"BEGIN",    // SQL-99
			"BETWEEN",    // MySQL 5.0
			"BIGINT",    // MySQL 5.0
			"BINARY",    // SQL-99
			"BIT",    // SQL-99
			"BLOB",    // SQL-99
			"BOOLEAN",    // SQL-99
			"BOTH",    // SQL-99
			"BREADTH",    // SQL-99
			"BY",    // SQL-99
			"CALL",    // SQL-99
			"CASCADE",    // SQL-99
			"CASCADED",    // SQL-99
			"CASE",    // SQL-99
			"CAST",    // SQL-99
			"CATALOG",    // SQL-99
			"CHANGE",    // MySQL 5.0
			"CHAR",    // SQL-99
			"CHARACTER",    // SQL-99
			"CHECK",    // SQL-99
			"CLASS",    // SQL-99
			"CLOB",    // SQL-99
			"CLOSE",    // SQL-99
			"COLLATE",    // SQL-99
			"COLLATION",    // SQL-99
			"COLUMN",    // SQL-99
			"COMMIT",    // SQL-99
			"COMPLETION",    // SQL-99
			"CONDITION",    // MySQL 5.0
			"CONNECT",    // SQL-99
			"CONNECTION",    // SQL-99
			"CONSTRAINT",    // SQL-99
			"CONSTRAINTS",    // SQL-99
			"CONSTRUCTOR",    // SQL-99
			"CONTINUE",    // SQL-99
			"CONVERT",    // MySQL 5.0
			"CORRESPONDING",    // SQL-99
			"CREATE",    // SQL-99
			"CROSS",    // SQL-99
			"CUBE",    // SQL-99
			"CURRENT",    // SQL-99
			"CURRENT_DATE",    // SQL-99
			"CURRENT_PATH",    // SQL-99
			"CURRENT_ROLE",    // SQL-99
			"CURRENT_TIME",    // SQL-99
			"CURRENT_TIMESTAMP",    // SQL-99
			"CURRENT_USER",    // SQL-99
			"CURSOR",    // MySQL 5.0
			"CURSOR",    // SQL-99
			"CYCLE",    // SQL-99
			"DATA",    // SQL-99
			"DATABASE",    // MySQL 5.0
			"DATABASES",    // MySQL 5.0
			"DATE",    // SQL-99
			"DAY",    // SQL-99
			"DAY_HOUR",    // MySQL 5.0
			"DAY_MICROSECOND",    // MySQL 5.0
			"DAY_MINUTE",    // MySQL 5.0
			"DAY_SECOND",    // MySQL 5.0
			"DEALLOCATE",    // SQL-99
			"DEC",    // SQL-99
			"DECIMAL",    // SQL-99
			"DECLARE",    // SQL-99
			"DEFAULT",    // SQL-99
			"DEFERRABLE",    // SQL-99
			"DEFERRED",    // SQL-99
			"DELAYED",    // MySQL 5.0
			"DELETE",    // SQL-99
			"DEPTH",    // SQL-99
			"DEREF",    // SQL-99
			"DESC",    // SQL-99
			"DESCRIBE",    // SQL-99
			"DESCRIPTOR",    // SQL-99
			"DESTROY",    // SQL-99
			"DESTRUCTOR",    // SQL-99
			"DETERMINISTIC",    // SQL-99
			"DIAGNOSTICS",    // SQL-99
			"DICTIONARY",    // SQL-99
			"DISCONNECT",    // SQL-99
			"DISTINCT",    // SQL-99
			"DISTINCTROW",    // MySQL 5.0
			"DIV",    // MySQL 5.0
			"DOMAIN",    // SQL-99
			"DOUBLE",    // SQL-99
			"DROP",    // SQL-99
			"DUAL",    // MySQL 5.0
			"DYNAMIC",    // SQL-99
			"EACH",    // SQL-99
			"ELSE",    // SQL-99
			"ELSEIF",    // MySQL 5.0
			"ENCLOSED",    // MySQL 5.0
			"END",    // SQL-99
			"END-EXEC",    // SQL-99
			"ENUM",    // MySQL 5.0
			"EQUALS",    // SQL-99
			"ESCAPE",    // SQL-99
			"ESCAPED",    // MySQL 5.0
			"EVERY",    // SQL-99
			"EXCEPT",    // SQL-99
			"EXCEPTION",    // SQL-99
			"EXEC",    // SQL-99
			"EXECUTE",    // SQL-99
			"EXISTS",    // MySQL 5.0
			"EXIT",    // MySQL 5.0
			"EXPLAIN",    // MySQL 5.0
			"EXTERNAL",    // SQL-99
			"FALSE",    // SQL-99
			"FETCH",    // SQL-99
			"FIRST",    // SQL-99
			"FLOAT",    // SQL-99
			"FLOAT4",    // MySQL 5.0
			"FLOAT8",    // MySQL 5.0
			"FOR",    // MySQL 5.0
			"FOR",    // SQL-99
			"FORCE",    // MySQL 5.0
			"FOREIGN",    // SQL-99
			"FOUND",    // SQL-99
			"FREE",    // SQL-99
			"FROM",    // SQL-99
			"FULL",    // SQL-99
			"FULLTEXT",    // MySQL 5.0
			"FUNCTION",    // SQL-99
			"GENERAL",    // SQL-99
			"GET",    // SQL-99
			"GLOBAL",    // SQL-99
			"GO",    // SQL-99
			"GOTO",    // SQL-99
			"GRANT",    // SQL-99
			"GROUP",    // SQL-99
			"GROUPING",    // SQL-99
			"HAVING",    // SQL-99
			"HIGH_PRIORITY",    // MySQL 5.0
			"HOST",    // SQL-99
			"HOUR",    // SQL-99
			"HOUR_MICROSECOND",    // MySQL 5.0
			"HOUR_MINUTE",    // MySQL 5.0
			"HOUR_SECOND",    // MySQL 5.0
			"IDENTITY",    // SQL-99
			"IF",    // MySQL 5.0
			"IGNORE",    // SQL-99
			"IMMEDIATE",    // SQL-99
			"IN",    // SQL-99
			"INDEX",    // MySQL 5.0
			"INDICATOR",    // SQL-99
			"INFILE",    // MySQL 5.0
			"INITIALIZE",    // SQL-99
			"INITIALLY",    // SQL-99
			"INNER",    // SQL-99
			"INOUT",    // SQL-99
			"INPUT",    // SQL-99
			"INSENSITIVE",    // MySQL 5.0
			"INSERT",    // SQL-99
			"INT",    // SQL-99
			"INT1",    // MySQL 5.0
			"INT2",    // MySQL 5.0
			"INT3",    // MySQL 5.0
			"INT4",    // MySQL 5.0
			"INT8",    // MySQL 5.0
			"INTEGER",    // SQL-99
			"INTERSECT",    // SQL-99
			"INTERVAL",    // SQL-99
			"INTO",    // SQL-99
			"IS",    // SQL-99
			"ISOLATION",    // SQL-99
			"ITERATE",    // SQL-99
			"JOIN",    // SQL-99
			"KEY",    // SQL-99
			"KEYS",    // MySQL 5.0
			"KILL",    // MySQL 5.0
			"LANGUAGE",    // SQL-99
			"LARGE",    // SQL-99
			"LAST",    // SQL-99
			"LATERAL",    // SQL-99
			"LEADING",    // SQL-99
			"LEAVE",    // MySQL 5.0
			"LEFT",    // SQL-99
			"LESS",    // SQL-99
			"LEVEL",    // SQL-99
			"LIKE",    // SQL-99
			"LIMIT",    // SQL-99
			"LINES",    // MySQL 5.0
			"LOAD",    // MySQL 5.0
			"LOCAL",    // SQL-99
			"LOCALTIME",    // SQL-99
			"LOCALTIMESTAMP",    // SQL-99
			"LOCATOR",    // SQL-99
			"LOCK",    // MySQL 5.0
			"LONG",    // MySQL 5.0
			"LONGBLOB",    // MySQL 5.0
			"LONGTEXT",    // MySQL 5.0
			"LOOP",    // MySQL 5.0
			"LOW_PRIORITY",    // MySQL 5.0
			"MAP",    // SQL-99
			"MATCH",    // SQL-99
			"MEDIUMBLOB",    // MySQL 5.0
			"MEDIUMINT",    // MySQL 5.0
			"MEDIUMTEXT",    // MySQL 5.0
			"MIDDLEINT",    // MySQL 5.0
			"MINUTE",    // SQL-99
			"MINUTE_MICROSECOND",    // MySQL 5.0
			"MINUTE_SECOND",    // MySQL 5.0
			"MOD",    // MySQL 5.0
			"MODIFIES",    // SQL-99
			"MODIFY",    // SQL-99
			"MODULE",    // SQL-99
			"MONTH",    // SQL-99
			"NAMES",    // SQL-99
			"NATIONAL",    // SQL-99
			"NATURAL",    // MySQL 5.0
			"NATURAL",    // SQL-99
			"NCHAR",    // SQL-99
			"NCLOB",    // SQL-99
			"NEW",    // SQL-99
			"NEXT",    // SQL-99
			"NO",    // SQL-99
			"NONE",    // SQL-99
			"NOT",    // SQL-99
			"NO_WRITE_TO_BINLOG",    // MySQL 5.0
			"NULL",    // SQL-99
			"NUMERIC",    // SQL-99
			"OBJECT",    // SQL-99
			"OF",    // SQL-99
			"OFF",    // SQL-99
			"OLD",    // SQL-99
			"ON",    // SQL-99
			"ONLY",    // SQL-99
			"OPEN",    // SQL-99
			"OPERATION",    // SQL-99
			"OPTIMIZE",    // MySQL 5.0
			"OPTION",    // SQL-99
			"OPTIONALLY",    // MySQL 5.0
			"OR",    // SQL-99
			"ORDER",    // SQL-99
			"ORDINALITY",    // SQL-99
			"OUT",    // SQL-99
			"OUTER",    // SQL-99
			"OUTFILE",    // MySQL 5.0
			"OUTPUT",    // SQL-99
			"PAD",    // SQL-99
			"PARAMETER",    // SQL-99
			"PARAMETERS",    // SQL-99
			"PARTIAL",    // SQL-99
			"PATH",    // SQL-99
			"POSTFIX",    // SQL-99
			"PRECISION",    // SQL-99
			"PREFIX",    // SQL-99
			"PREORDER",    // SQL-99
			"PREPARE",    // SQL-99
			"PRESERVE",    // SQL-99
			"PRIMARY",    // SQL-99
			"PRIOR",    // SQL-99
			"PRIVILEGES",    // SQL-99
			"PROCEDURE",    // SQL-99
			"PUBLIC",    // SQL-99
			"PURGE",    // MySQL 5.0
			"RAID0",    // MySQL 5.0
			"READ",    // SQL-99
			"READS",    // SQL-99
			"REAL",    // SQL-99
			"RECURSIVE",    // SQL-99
			"REF",    // SQL-99
			"REFERENCES",    // SQL-99
			"REFERENCING",    // SQL-99
			"REGEXP",    // MySQL 5.0
			"RELATIVE",    // SQL-99
			"RELEASE",    // MySQL 5.0
			"RENAME",    // MySQL 5.0
			"REPEAT",    // MySQL 5.0
			"REPLACE",    // MySQL 5.0
			"REQUIRE",    // MySQL 5.0
			"RESTRICT",    // SQL-99
			"RESULT",    // SQL-99
			"RETURN",    // SQL-99
			"RETURNS",    // SQL-99
			"REVOKE",    // SQL-99
			"RIGHT",    // SQL-99
			"RLIKE",    // MySQL 5.0
			"ROLE",    // SQL-99
			"ROLLBACK",    // SQL-99
			"ROLLUP",    // SQL-99
			"ROUTINE",    // SQL-99
			"ROW",    // SQL-99
			"ROWS",    // SQL-99
			"SAVEPOINT",    // SQL-99
			"SCHEMA",    // SQL-99
			"SCHEMAS",    // MySQL 5.0
			"SCOPE",    // SQL-99
			"SCROLL",    // SQL-99
			"SEARCH",    // SQL-99
			"SECOND",    // SQL-99
			"SECOND_MICROSECOND",    // MySQL 5.0
			"SECTION",    // SQL-99
			"SELECT",    // SQL-99
			"SENSITIVE",    // MySQL 5.0
			"SEPARATOR",    // MySQL 5.0
			"SEQUENCE",    // SQL-99
			"SESSION",    // SQL-99
			"SESSION_USER",    // SQL-99
			"SET",    // SQL-99
			"SETS",    // SQL-99
			"SHOW",    // MySQL 5.0
			"SIZE",    // SQL-99
			"SMALLINT",    // SQL-99
			"SOMESPACE",    // SQL-99
			"SONAME",    // MySQL 5.0
			"SPATIAL",    // MySQL 5.0
			"SPECIFIC",    // SQL-99
			"SPECIFICTYPE",    // SQL-99
			"SQL",    // SQL-99
			"SQLEXCEPTION",    // SQL-99
			"SQLSTATE",    // SQL-99
			"SQLWARNING",    // SQL-99
			"SQL_BIG_RESULT",    // MySQL 5.0
			"SQL_CALC_FOUND_ROWS",    // MySQL 5.0
			"SQL_SMALL_RESULT",    // MySQL 5.0
			"SSL",    // MySQL 5.0
			"START",    // SQL-99
			"STARTING",    // MySQL 5.0
			"STATE",    // SQL-99
			"STATEMENT",    // SQL-99
			"STATIC",    // SQL-99
			"STRAIGHT_JOIN",    // MySQL 5.0
			"STRUCTURE",    // SQL-99
			"SYSTEM_USER",    // SQL-99
			"TABLE",    // SQL-99
			"TEMPORARY",    // SQL-99
			"TERMINATE",    // SQL-99
			"TERMINATED",    // MySQL 5.0
			"TEXT",    // MySQL 5.0
			"THAN",    // SQL-99
			"THEN",    // SQL-99
			"TIME",    // SQL-99
			"TIMESTAMP",    // SQL-99
			"TIMEZONE_HOUR",    // SQL-99
			"TIMEZONE_MINUTE",    // SQL-99
			"TINYBLOB",    // MySQL 5.0
			"TINYINT",    // MySQL 5.0
			"TINYTEXT",    // MySQL 5.0
			"TO",    // SQL-99
			"TRAILING",    // SQL-99
			"TRANSLATION",    // SQL-99
			"TREAT",    // SQL-99
			"TRIGGER",    // SQL-99
			"TRUE",    // SQL-99
			"UNDER",    // SQL-99
			"UNDO",    // MySQL 5.0
			"UNION",    // SQL-99
			"UNIQUE",    // SQL-99
			"UNKNOWN",    // SQL-99
			"UNLOCK",    // MySQL 5.0
			"UNNEST",    // SQL-99
			"UNSIGNED",    // MySQL 5.0
			"UPDATE",    // SQL-99
			"UPGRADE",    // MySQL 5.0
			"USAGE",    // SQL-99
			"USE",    // MySQL 5.0
			"USER",    // SQL-99
			"USING",    // SQL-99
			"UTC_DATE",    // MySQL 5.0
			"UTC_TIME",    // MySQL 5.0
			"UTC_TIMESTAMP",    // MySQL 5.0
			"VALUE",    // SQL-99
			"VALUES",    // SQL-99
			"VARBINARY",    // MySQL 5.0
			"VARCHAR",    // SQL-99
			"VARCHARACTER",    // MySQL 5.0
			"VARIABLE",    // SQL-99
			"VARYING",    // SQL-99
			"VIEW",    // SQL-99
			"WHEN",    // SQL-99
			"WHENEVER",    // SQL-99
			"WHERE",    // SQL-99
			"WHILE",    // MySQL 5.0
			"WITH",    // SQL-99
			"WITHOUT",    // SQL-99
			"WORK",    // SQL-99
			"WRITE",    // SQL-99
			"X509",    // MySQL 5.0
			"XOR",    // MySQL 5.0
			"YEAR",    // SQL-99
			"YEAR_MONTH",    // MySQL 5.0
			"ZEROFILL",    // MySQL 5.0
			"ZONE",    // SQL-99
        }));
	
	/**
	 * @param identifier Any String
	 * @return <tt>true</tt> iff it is a reserved word in SQL
	 */
	public static boolean contains(String identifier) {
		return reservedWords.contains(identifier.toUpperCase());
	}
}