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
class TripleResultSet {

	/** List of tripleMakers that are used on every row of the result set. */
    private ArrayList tripleMakers = new ArrayList();
    private Iterator tripleMakerIterator;
	/** The name used for the default graph. */
    private Map columnNameNumberMap;
	/** Flag that the record set has already been created. */
    private boolean queryHasBeenExecuted = false;
    /** Array with the data from the current row of the resordset. */
    private String[] currentRow;

	/**
	 * The chached triple. A value of null means there is no triple
	 * in the cache. A triple gets chached by hasNext() and is
	 * delivered afterwards by next().
	 */
    private Triple chachedTriple;

    private Database database;
    private ResultSet resultSet = null;
    private String sql;
    private boolean rsForward = false;
    private int numCols = 0;

	public TripleResultSet(String SQL, Map columnNameNumberMap, Database db) {
		this.sql = SQL;
		this.database = db;
		this.columnNameNumberMap = columnNameNumberMap;
    }

	public void addTripleMaker(TripleQuery tripMaker) {
    		this.tripleMakers.add(tripMaker);
	}

	public boolean hasTripleMakers() {
		return this.tripleMakers.size() > 0;
    	}

	public boolean hasNext() {
		if (this.chachedTriple == null) {
			this.chachedTriple = next();
		}
		return this.chachedTriple != null;
	}

    /** Returns the next triple.
     * If there are no more triple makers for the current row of the result set
     * then the next row is cached and the triple makers iterator is reset.
     */
	public Triple next() {
		if (!hasTripleMakers()) {
			return null;
		}
		if (this.chachedTriple != null) {
			Triple t = this.chachedTriple;
			this.chachedTriple = null;
			return t;
		}
		if (!this.queryHasBeenExecuted) {
			executeSQLQuery();
			this.queryHasBeenExecuted = true;
		}
		if (this.rsForward && !this.tripleMakerIterator.hasNext()) {
			this.rsForward = false;
		}
		if (!this.rsForward) {
			this.currentRow = nextRow();
			if (this.currentRow == null) {
				return null;
			}
			this.tripleMakerIterator = this.tripleMakers.iterator();
			this.rsForward = true;
		}
		TripleQuery tripMaker = (TripleQuery) this.tripleMakerIterator.next();
		Triple triple = tripMaker.makeTriple(this.currentRow, this.columnNameNumberMap);
		return (triple != null) ? triple : next();
    }

    private void executeSQLQuery() {
		if (Logger.instance().debugEnabled()) {
            Logger.instance().debug("--------------------------------------------");
            Logger.instance().debug("SQL statement executed: " + this.sql);
            Logger.instance().debug("--------------------------------------------");
        }
        try {
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
	private String[] nextRow() {
		try {
			if (!this.resultSet.next()) {
				this.resultSet.close();
				return null;
			}
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
