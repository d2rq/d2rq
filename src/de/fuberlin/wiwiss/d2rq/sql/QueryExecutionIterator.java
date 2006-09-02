package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.NoSuchElementException;

import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.helpers.InfoD2RQ;
import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Executes an SQL query and delivers result rows as an iterator over arrays
 * of Strings. The query is executed lazily. This class logs all executed SQL queries.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: QueryExecutionIterator.java,v 1.2 2006/09/02 22:41:44 cyganiak Exp $
 */
public class QueryExecutionIterator implements ClosableIterator {
	public static Collection protocol=null;

	private String sql;
	private Database database;
	private ResultSet resultSet = null;
	private String[] prefetchedRow = null;
	private int numCols = 0;
	private boolean queryExecuted = false;
	private boolean explicitlyClosed = false;

	public QueryExecutionIterator(String sql, Database db) {
		this.sql = sql;
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
	 * Delivers the next query result row.
	 * @return An array of strings, each representing one cell of the row.
	 */
	public String[] nextRow() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		String[] result = this.prefetchedRow;
		this.prefetchedRow = null;
		return result;
	}

	private String[] tryFetchNextRow() {
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
			InfoD2RQ.totalNumberOfReturnedRows++;
			InfoD2RQ.totalNumberOfReturnedFields+=this.numCols;
			String[] result = new String[this.numCols];
			for (int i = 0; i < this.numCols; i++) {
				result[i] = this.resultSet.getString(i + 1);
			}
			return result;
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
    	InfoD2RQ.totalNumberOfExecutedSQLQueries++;
    	if (protocol!=null)
    	    protocol.add(this.sql);
        try {
			Connection con = this.database.getConnnection();
			java.sql.Statement stmt = con.createStatement();
			this.resultSet = stmt.executeQuery(this.sql);
			this.numCols = this.resultSet.getMetaData().getColumnCount();
        } catch (SQLException ex) {
        	throw new D2RQException(ex.getMessage() + ": " + this.sql);
        }
    }
}
