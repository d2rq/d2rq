package de.fuberlin.wiwiss.d2rq.sql.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

/**
 * Represents a SQL data type. 
 * 
 * TODO: Data types should know whether they can be used in DISTINCT queries
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author Christian Becker <http://beckr.org#chris>
 */
public abstract class DataType {

	public final static Log log = LogFactory.getLog(DataType.class);

	public enum GenericType {
		CHARACTER (Types.VARCHAR, "VARCHAR"),
		BINARY    (Types.VARBINARY, "VARBINARY"),
		NUMERIC   (Types.NUMERIC, "NUMERIC"),
		BOOLEAN   (Types.BOOLEAN, "BOOLEAN"),
		DATE      (Types.DATE, "DATE"),
		TIME      (Types.TIME, "TIME"),
		TIMESTAMP (Types.TIMESTAMP, "TIMESTAMP"),
		INTERVAL  (Types.VARCHAR, "INTERVAL"),
		BIT       (Types.BIT, "BIT");
		private final int jdbcType;
		private final String name;
		GenericType(int jdbcType, String name) {
			this.jdbcType = jdbcType;
			this.name = name.toUpperCase();
		}
		public DataType dataTypeFor(Vendor vendor) {
			return vendor.getDataType(jdbcType, name, 0);
		}
	}
	
	private final Vendor sqlSyntax;
	private final String name;
	
	/**
	 * @param name Name as reported by JDBC metadata, for debugging
	 */
	public DataType(Vendor sqlSyntax, String name) {
		this.sqlSyntax = sqlSyntax;
		this.name = name;
	}

	/**
	 * Return the appropriate RDF datatype for a SQL data type. <code>null</code>
	 * indicates a known SQL type that cannot be mapped to RDF.
	 * 
	 * @return RDF datatype as prefixed name: <code>xsd:string</code> etc.
	 */
	public String rdfType() {
		return "xsd:string";
	}
	
	public boolean isIRISafe() {
		return false;
	}

	/**
	 * @return <code>true</code> if this column can be used in <code>SELECT DISTINCT</code> queries
	 */
	public boolean supportsDistinct() {
		return true;
	}

	public boolean isUnsupported() {
		return false;
	}
	
	/**
	 * Creates a SQL literal for the given value, suitable
	 * for comparison to a column of this indicated type.
	 * If the value is not suitable for the column type
	 * (e.g., not a number for a SQLExactNumeric), <code>NULL</code>
	 * is returned.
	 * 
	 * @param value A value
	 * @return A quoted and escaped SQL literal, suitable for comparison to a column 
	 */
	public String toSQLLiteral(String value) {
		return sqlSyntax.quoteStringLiteral(value);
	}

	/**
	 * Retrieves a string value in preferred format (canonical form
	 * of the closest XSD type) from a SQL ResultSet.
	 * 
	 * @param resultSet Result of a SELECT query
	 * @param column The column index to retrieve; leftmost columns is 1
	 * @return String representation, or <code>null</code> if SQL result was null or is not representable in the XSD type
	 * @throws SQLException
	 */
	public String value(ResultSet resultSet, int column) throws SQLException {
		return resultSet.getString(column);
	}

	/**
	 * A regular expression that covers the lexical form of
	 * all values of this datatype (in their RDF representation).
	 * This is especially important for types that are not mapped
	 * to a typed literal but to plain/xsd:string literals.
	 * 
	 * @return A regular expression covering the lexical forms of all values of this datatype
	 */
	public String valueRegex() {
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + name;
	}
	
	protected Vendor syntax() {
		return sqlSyntax;
	}
	
	/**
	 * Returns the datatype's name as reported by JDBC metadata
	 * (or closest equivalent), for debugging
	 */
	public String name() {
		return name;
	}
}