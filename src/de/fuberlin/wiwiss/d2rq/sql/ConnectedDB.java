package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType.GenericType;
import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;
 
/**
 * TODO Move all engine-specific code from here to {@link Vendor} and its implementations
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @author kurtjx (http://github.com/kurtjx)
 */
public abstract class ConnectedDB {
	private static final Log log = LogFactory.getLog(ConnectedDB.class);

	private final Map<Attribute,Boolean> cachedColumnNullability = 
		new HashMap<Attribute,Boolean>();
	private final Map<Attribute,DataType> cachedColumnTypes = 
		new HashMap<Attribute,DataType>();
	private final Map<Attribute,GenericType> overriddenColumnTypes =
		new HashMap<Attribute,GenericType>();
	private DatabaseSchemaInspector schemaInspector = null;
	// Lazy initialization -- use vendor() for access!
	private Vendor vendor = null;
	private int dbMajorVersion = -1;
	private int dbMinorVersion = -1;
	
	private int limit;
	private int fetchSize;
	private int defaultFetchSize = Database.NO_FETCH_SIZE;
	private Map<Attribute,Boolean> zerofillCache = new HashMap<Attribute,Boolean>();
	private Map<RelationName,Map<String,List<String>>> uniqueIndexCache = 
		new HashMap<RelationName,Map<String,List<String>>>();


	public ConnectedDB(Map<String,GenericType> columnTypes,
			int limit, int fetchSize) {
		// TODO replace column type arguments with a single column => type map
		this.limit = limit;
		this.fetchSize = fetchSize;
		
		for (String columnName: columnTypes.keySet()) {
			overriddenColumnTypes.put(SQL.parseAttribute(columnName), 
					columnTypes.get(columnName));
		}
	}
	
	
	/**
	 * Call during initialization to establish the database connection(s).
	 */
	public abstract void init();
	
	/**
	 * Returns a connection to the database.
	 * 
	 * @return a connection to the database.
	 */
	public abstract Connection connection();
	
	/**
	 * When no longer needed, a connection obtained by {@link #connection()} should be closed by calling this function.
	 * 
	 * @param c The connection that needs to be closed
	 */
	public abstract void close(Connection c);
	
	/**
	 * Call during shutdown, to close all connections to the database.
	 */
	public abstract void close();


	
	public int limit() {
		return this.limit;
	}
	
	public void setDefaultFetchSize(int value) {
		defaultFetchSize = value;
	}
	
	public int fetchSize() {
		if (fetchSize == Database.NO_FETCH_SIZE) {
			if (vendorIs(Vendor.MySQL)) {
				return Integer.MIN_VALUE;
			}
			return defaultFetchSize;
		}
		return fetchSize;
	}
	
	public DatabaseSchemaInspector schemaInspector() {
		if (this.schemaInspector == null /*&& this.jdbcURL != null */ ) {
			this.schemaInspector = new DatabaseSchemaInspector(this);
		}
		return this.schemaInspector;
	}

	public DataType columnType(Attribute column) {
		if (!cachedColumnTypes.containsKey(column)) {
			if (overriddenColumnTypes.containsKey(column)) {
				cachedColumnTypes.put(column, overriddenColumnTypes.get(column).dataTypeFor(vendor()));
			} else if (schemaInspector() == null) {
				cachedColumnTypes.put(column, GenericType.CHARACTER.dataTypeFor(vendor()));
			} else {
				cachedColumnTypes.put(column, schemaInspector().columnType(column));
			}
		}
		return cachedColumnTypes.get(column);
	}
	
	public boolean isNullable(Attribute column) {
		if (!cachedColumnNullability.containsKey(column)) {
			cachedColumnNullability.put(column, 
					schemaInspector() == null ? true : schemaInspector().isNullable(column));
		}
		return cachedColumnNullability.get(column);
	}
	
	/**
	 * Reports the brand of RDBMS.
	 * @return <tt>true</tt> if this database is of the given brand
	 * @see #dbType()
	 * 
	 * TODO make private, use {@link #vendor()} and its methods instead
	 */
	public boolean vendorIs(Vendor vendor) {
		return this.vendor.equals(vendor);
	}
	
	public int dbMajorVersion()
	{
		if (this.dbMajorVersion == -1) {
			try {
				Connection connection = connection();
				this.dbMajorVersion = connection.getMetaData().getDatabaseMajorVersion();
				close(connection);
			} catch (SQLException ex) {
				throw new D2RQException("Database exception", ex);
			}
		}
		return this.dbMajorVersion;
	}
	
	public int dbMinorVersion()
	{
		if (this.dbMinorVersion == -1) {
			try {
				Connection connection = connection();
				this.dbMinorVersion = connection.getMetaData().getDatabaseMinorVersion();
				close(connection);
			} catch (SQLException ex) {
				throw new D2RQException("Database exception", ex);
			}
		}
		return this.dbMinorVersion;
	}
	
	
	/**
	 * @return A helper for generating SQL statements conforming to the syntax
	 * of the database engine used in this connection
	 */
	public Vendor vendor() {
		ensureVendorInitialized();
		return vendor;
	}

	protected String getDatabaseProductType() throws SQLException {
		Connection connection = null;
		try {
			connection = connection();
			String type = connection.getMetaData().getDatabaseProductName();
			return type;
		} finally {
			close(connection);
		}
	}
	
	private void ensureVendorInitialized() {
		if (vendor != null) return;
		try {
			String productName = getDatabaseProductType();
			log.info("JDBC database product type: " + productName);
			productName = productName.toLowerCase();
			if (productName.indexOf("mysql") >= 0) {
				vendor = Vendor.MySQL;
			} else if (productName.indexOf("postgresql") >= 0) {
				vendor = Vendor.PostgreSQL;
			} else if (productName.indexOf("interbase") >= 0) {
				vendor = Vendor.InterbaseOrFirebird;
			} else if (productName.indexOf("oracle") >= 0) {
				this.vendor = Vendor.Oracle; 
			} else if (productName.indexOf("microsoft sql server") >= 0) {
				this.vendor = Vendor.SQLServer;
			} else if (productName.indexOf("access") >= 0) {
				this.vendor = Vendor.MSAccess;
			} else if (productName.indexOf("hsql") >= 0) {
				this.vendor = Vendor.HSQLDB;
			} else {
				this.vendor = Vendor.SQL92;
			}
			log.info("Using vendor class: " + vendor.getClass().getName());
		} catch (SQLException ex) {
			throw new D2RQException("Database exception", ex);
		}
	}
	
	/**
	 * <p>Checks if two columns are formatted by the database in a compatible
	 * fashion.</p>
	 * 
	 * <p>Assuming <tt>v1</tt> is a value from column1, and <tt>v2</tt> a value
	 * from column2, and <tt>v1 = v2</tt> evaluates to <tt>true</tt> within the
	 * database, then we call the values have <em>compatible formatting</em> if
	 * <tt>SELECT</tt>ing them results in character-for-character identical
	 * strings. As an example, a <tt>TINYINT</tt> and a <tt>BIGINT</tt> are
	 * compatible because equal values will be formatted in the same way
	 * when <tt>SELECT</tt>ed, e.g. <tt>1 = 1</tt>. But if one of them is
	 * <tt>ZEROFILL</tt>, then <tt>SELECT</tt>ing will result in a different
	 * character string, e.g. <tt>1 = 0000000001</tt>. The two columns wouldn't
	 * be compatible.</p>
	 * 
	 * <p>This is used by the engine when removing unnecessary joins. If
	 * two columns have compatible formatting, then we can sometimes use
	 * one in place of the other when they are known to have equal values.
	 * But not if they are incompatible, because e.g. <tt>http://example.org/id/1</tt>
	 * is different from <tt>http://example.org/id/0000000001</tt>.</p>
	 * 
	 * @return <tt>true</tt> if both arguments have compatible formatting
	 */
	public boolean areCompatibleFormats(Attribute column1, Attribute column2) {
		// TODO Right now we only catch the ZEROFILL case. There are many more!
		return !isZerofillColumn(column1) && !isZerofillColumn(column2);
	}
	
	private boolean isZerofillColumn(Attribute column) {
		if (!vendorIs(Vendor.MySQL)) return false;
		if (!zerofillCache.containsKey(column)) {
			zerofillCache.put(column, schemaInspector().isZerofillColumn(column));
		}
		return zerofillCache.get(column);
	}
	
	public Map<String,List<String>> getUniqueKeyColumns(RelationName tableName) {
		if (!uniqueIndexCache.containsKey(tableName) && schemaInspector() != null)
			uniqueIndexCache.put(tableName, schemaInspector().uniqueColumns(tableName));
		return uniqueIndexCache.get(tableName);
	}
    
	/**
	 * In some situations, MySQL stores table names using lowercase only, and then performs
	 * case-insensitive comparison.
	 * We need to account for this when comparing table names reported by MySQL and those from the mapping.   
	 * 
	 * @see <a href="http://dev.mysql.com/doc/refman/5.0/en/identifier-case-sensitivity.html">MySQL Manual, Identifier Case Sensitivity</a>
	 */
	public boolean lowerCaseTableNames() {
		Connection c = connection();
		if (c instanceof com.mysql.jdbc.ConnectionImpl)
			return ((com.mysql.jdbc.ConnectionImpl)c).lowerCaseTableNames();
		else
			return false;
	}



}
