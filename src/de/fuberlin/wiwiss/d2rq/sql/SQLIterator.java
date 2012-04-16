package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Executes an SQL query and delivers result rows as an iterator over {@link ResultRow}s.
 * The query is executed lazily. This class logs all executed SQL queries.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 */

public class SQLIterator implements ClosableIterator<ResultRow> {
	private final static Log log = LogFactory.getLog(SQLIterator.class);
	private String sql;
	private List<ProjectionSpec> columns;
	private ConnectedDB database;
	private Connection con;
	private Statement statement = null;
	private ResultSet resultSet = null;
	private ResultRow prefetchedRow = null;
	private int numCols = 0;
	private boolean queryExecuted = false;
	private boolean explicitlyClosed = false;


	public SQLIterator(String sql, List<ProjectionSpec> columns, ConnectedDB db) {
		this.sql = sql;
		this.columns = columns;
		this.database = db;
    }

	public boolean hasNext() {
		if (this.explicitlyClosed) {
			return false;
		}
		if (this.prefetchedRow == null) {
			tryFetchNextRow();
		}
		return this.prefetchedRow != null;
	}

	/**
	 * @return The next query ResultRow.
	 */
	public ResultRow next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		ResultRow result = this.prefetchedRow;
		this.prefetchedRow = null;
		return result;
	}

	/**
	 * @deprecated Use {@link #next()} instead
	 */
	public ResultRow nextRow() {
		return next();
	}

	private void tryFetchNextRow() {
	    ensureQueryExecuted();
	    if (this.resultSet == null) {
	    	this.prefetchedRow = null;
	    	return;
	    }
		try {
			if (!this.resultSet.next()) {
				this.resultSet.close();
				this.resultSet = null;
		    	this.prefetchedRow = null;
		    	return;
			}
			BeanCounter.totalNumberOfReturnedRows++;
			BeanCounter.totalNumberOfReturnedFields+=this.numCols;
			prefetchedRow = ResultRowMap.fromResultSet(resultSet, columns, database);
		} catch (SQLException ex) {
			throw new D2RQException(ex);
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
				//ignore  throw new D2RQException(ex.getMessage() + "; query was: " + this.sql);
			}
	    }
	    
	    if (this.statement != null) {
			try {
				this.statement.close();
				this.statement = null;
			} catch (SQLException ex) {
				//ignore throw new D2RQException(ex.getMessage() + "; query was: " + this.sql);
			}
	    }
	    
		if (this.con != null) {
			this.database.close(con);
		}

	    this.prefetchedRow = null;

	}

	public void remove() {
		throw new RuntimeException("Operation not supported");
	}

	private void ensureQueryExecuted() {
	    if (this.queryExecuted) {
	    	return;
	    }
    	this.queryExecuted = true;
    	log.info(sql);
    	BeanCounter.totalNumberOfExecutedSQLQueries++;
        try {
			con = this.database.connection();
			this.statement = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			if (database.fetchSize() != Database.NO_FETCH_SIZE) {
				try {
					this.statement.setFetchSize(database.fetchSize());
				}
				catch (SQLException e) {
					log.warn("could not set fetch size: " + e.getMessage());
				} /* Some drivers don't support fetch sizes, e.g. JDBC-ODBC */
			}
			long start = System.nanoTime();
			this.resultSet = this.statement.executeQuery(this.sql);
			long stop = System.nanoTime();
			log.debug("SQL query took " + TimeUnit.NANOSECONDS.toMillis(stop - start) + " ms");
			this.numCols = this.resultSet.getMetaData().getColumnCount();
        } catch (SQLException ex) {
        	throw new D2RQException(ex.getMessage() + ": " + this.sql);
        }
    }
}
