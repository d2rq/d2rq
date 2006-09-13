package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.D2RQException;

/**
 * Executes an SQL query and delivers result rows as an iterator over arrays
 * of Strings. The query is executed lazily. This class logs all executed SQL queries.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: QueryExecutionIterator.java,v 1.5 2006/09/13 14:06:23 cyganiak Exp $
 */
public class QueryExecutionIterator implements ClosableIterator {
	public static Collection protocol=null;

	private String sql;
	private List columns;
	private ConnectedDB database;
	private ResultSet resultSet = null;
	private ResultRow prefetchedRow = null;
	private int numCols = 0;
	private boolean queryExecuted = false;
	private boolean explicitlyClosed = false;

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
	    if (this.resultSet != null) {
			try {
				this.resultSet.close();
				this.resultSet = null;
			} catch (SQLException ex) {
				throw new D2RQException(ex.getMessage() + "; query was: " + this.sql);
			}
	    }
	}

	public void remove() {
		throw new RuntimeException("Operation not supported");
	}

	private void ensureQueryExecuted() {
	    if (this.queryExecuted) {
	    	return;
	    }
    	this.queryExecuted = true;
    	LogFactory.getLog(QueryExecutionIterator.class).debug(this.sql);
    	BeanCounter.totalNumberOfExecutedSQLQueries++;
    	if (protocol!=null)
    	    protocol.add(this.sql);
        try {
			Connection con = this.database.connection();
			java.sql.Statement stmt = con.createStatement();
			this.resultSet = stmt.executeQuery(this.sql);
			this.numCols = this.resultSet.getMetaData().getColumnCount();
        } catch (SQLException ex) {
        	throw new D2RQException(ex.getMessage() + ": " + this.sql);
        }
    }
}
