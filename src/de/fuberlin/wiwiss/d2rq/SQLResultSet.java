/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import java.sql.*;
import java.util.*;

/**
 * Contains the result set from one SQL query and transforms it into triples.
 * A triple is produced for TripleMaker in TripleMaker and each row in the result set.
 *
 * <p>History:<br>
 * 06-06-2004: Initial version of this class.<br>
 * 08-03-2004: Almost complete rewrite to make logic more explicit.<br>
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
class SQLResultSet {

	/** The name used for the default graph. */
    protected Map columnNameNumberMap;
	/** Flag that the record set has already been created. */
    protected boolean queryHasBeenExecuted = false;
    /** Array with the data from the current row of the resordset. */
    protected String[] currentRow;

    protected Database database;
    protected ResultSet resultSet = null;
    protected String sql;
    protected int numCols = 0;

	public SQLResultSet(String SQL, Map columnNameNumberMap, Database db) {
		this.sql = SQL;
		this.database = db;
		this.columnNameNumberMap = columnNameNumberMap;
    }

    protected void executeSQLQuery() {
		if (Logger.instance().debugEnabled()) {
            Logger.instance().debug("--------------------------------------------");
            Logger.instance().debug("SQL statement executed: " + this.sql);
            Logger.instance().debug("--------------------------------------------");
        }
        try {
        		InfoD2RQ.totalNumberOfExecutedSQLQueries++;
			Connection con = this.database.getConnnection();
			// Create and execute SQL statement
			java.sql.Statement stmt = con.createStatement();
			this.resultSet = stmt.executeQuery(this.sql);
			this.numCols = this.resultSet.getMetaData().getColumnCount();
        } catch (SQLException ex) {
            Logger.instance().error(ex.getMessage());
        }
    }

	public void close() {
	    if (this.resultSet == null) {
	        return;
	    }
		try {
			this.resultSet.close();
			this.resultSet = null;
		} catch (SQLException ex) {
			// System.err.println(ex.getMessage());
			// ex.printStackTrace();
		}
		this.queryHasBeenExecuted = false;
	}

	/** Gets the current row from the result set in an array which is passed to the triple makers */
	protected String[] nextRow() {
		try {
			if (!this.resultSet.next()) {
				this.resultSet.close();
				return null;
			}
			InfoD2RQ.totalNumberOfReturnedRows++;
			InfoD2RQ.totalNumberOfReturnedFields+=this.numCols;
			String[] result = new String[this.numCols + 1];
			for (int i = 1; i <= this.numCols; i++) {
				result[i] = this.resultSet.getString(i);
			}
			return result;
		} catch (SQLException ex) {
			Logger.instance().error(ex.getMessage());
			return null;
		}
	}
}
