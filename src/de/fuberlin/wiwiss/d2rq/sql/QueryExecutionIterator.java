package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Executes an SQL query and delivers result rows as an iterator over {@link ResultRow}s.
 * The query is executed lazily. This class logs all executed SQL queries.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: QueryExecutionIterator.java,v 1.13 2009/08/06 10:51:34 fatorange Exp $
 */
public class QueryExecutionIterator implements ClosableIterator {
	
	private static Logger logger = LoggerFactory.getLogger(QueryExecutionIterator.class);
	
	private String sql;
	private List columns;
	private ConnectedDB database;
	private Statement statement = null;
	private ResultSet resultSet = null;
	private ResultRow prefetchedRow = null;
	private int numCols = 0;
	private boolean queryExecuted = false;
	private boolean explicitlyClosed = false;
	
	private int numOfRowsFetched = 0;

	public QueryExecutionIterator(String sql, List columns, ConnectedDB db) {
		this.sql = sql;
		this.columns = columns;
		this.database = db;
	}

	public boolean hasNext() {
		if (this.explicitlyClosed) {
			return false;
		}
		if (this.prefetchedRow == null) {
			this.prefetchedRow = tryFetchNextRow();
		}
		return this.prefetchedRow != null;
	}

	/**
	 * Delivers the next query result row.
	 * @return An array of strings, each representing one cell of the row.
	 */
	public Object next() {
		return nextRow();
	}

	/**
	 * @return The next query ResultRow.
	 */
	public ResultRow nextRow() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		ResultRow result = this.prefetchedRow;
		this.prefetchedRow = null;
		return result;
	}

	private ResultRow tryFetchNextRow() {
		ensureQueryExecuted();
		if (this.resultSet == null) {
			return null;
		}
		try {
			if (!this.resultSet.next()) {
				this.resultSet.close();
				this.resultSet = null;
				return null;
			}
			numOfRowsFetched++;
			BeanCounter.totalNumberOfReturnedRows++;
			BeanCounter.totalNumberOfReturnedFields+=this.numCols;
			return ResultRowMap.fromResultSet(this.resultSet, this.columns);
		} catch (SQLException ex) {
			throw new D2RQException(ex.getMessage());
		}
	}
	
	/**
	 * Make sure the SQL result set is closed and freed. Will auto-close when the
	 * record-set is exhausted.
	 */
	public void close() {
		this.explicitlyClosed = true;
		
		/* JDBC 4+ requires manual closing of result sets and statements */
		if (this.resultSet != null) {
			try {
				this.resultSet.close();
				this.resultSet = null;
			} catch (SQLException ex) {
				throw new D2RQException(ex.getMessage() + "; query was: " + this.sql);
			}
		}
		
		if (this.statement != null) {
			try {
				this.statement.close();
				this.statement = null;
			} catch (SQLException ex) {
				throw new D2RQException(ex.getMessage() + "; query was: " + this.sql);
			}
		}
		
		this.prefetchedRow = null;
		
		logger.debug("resultset closed, returned {} rows", Integer.valueOf(numOfRowsFetched));

	}

	public void remove() {
		throw new RuntimeException("Operation not supported");
	}

	private void ensureQueryExecuted() {
		if (this.queryExecuted) {
			return;
		}
		this.queryExecuted = true;
		logger.debug(this.sql);
		BeanCounter.totalNumberOfExecutedSQLQueries++;
		try {
			Connection con = this.database.connection();
			this.statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			if (database.fetchSize() != Database.NO_FETCH_SIZE) {
				try {
					this.statement.setFetchSize(database.fetchSize());
				}
				catch (SQLException e) {} /* Some drivers don't support fetch sizes, e.g. JDBC-ODBC */
			}
			long start = System.currentTimeMillis();
			this.resultSet = this.statement.executeQuery(this.sql);
			long stop = System.currentTimeMillis();
			logger.debug("SQL query took {} ms", Long.valueOf(stop - start));
			this.numCols = this.resultSet.getMetaData().getColumnCount();
		} catch (SQLException ex) {
			throw new D2RQException(ex.getMessage() + ": " + this.sql);
		}
	}
}
