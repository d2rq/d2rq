/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import java.sql.*;
import java.util.*;

/** The TripleResultSet contains the result set from one SQL query and transforms it into triples.
 * A triple is produced for TripleMaker in TripleMaker and each row in the result set.
 *
 * <BR>History: 06-06-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
public class TripleResultSet {

	/** List of tripleMakers that are used on every row of the result set. */
    protected ArrayList tripleMakers;
    protected Iterator tripleMakerIterator;
	/** The name used for the default graph. */
    protected HashMap columnNameNumberMap;
	/** Flag that the record set has already been created. */
    protected boolean queryHasBeenExecuted = false;
    /** Array with the data from the current row of the resordset. */
    protected String[] currentRow;
    /** Caches the triple which have allready been delivered in order to eliminate dublicates. */
    protected HashSet deliveredTriples;

    /** Flag that a triple has been chached. */
    protected boolean hasTripleChached = false;
    /** The chached triple.
     * A triple gets chached by hasNext() and if delivered afterwards by next()
    */
    protected Triple chachedTriple;


    protected Database database;
    protected ResultSet resultSet = null;
    protected String sql;
    protected boolean rsForward = false;
    protected int numCols = 0;
    protected boolean debug = false;


    protected TripleResultSet(String SQL, HashMap columnNameNumberMap, Database db, boolean debug) {
         sql = SQL;
         database = db;
         this.columnNameNumberMap = columnNameNumberMap;
         this.debug = debug;
         
         tripleMakers = new ArrayList();
         deliveredTriples = new HashSet();
    }

    protected void addTripleMaker(TripleMaker tripMaker) {
        tripleMakers.add(tripMaker);
    }

    protected void addTripleMaker(NodeMaker subjectMaker, NodeMaker predicateMaker, NodeMaker objectMaker) {
        TripleMaker tripMaker = new TripleMaker(subjectMaker, predicateMaker, objectMaker);
        addTripleMaker(tripMaker);
    }

    protected boolean hasTripleMakers() {
       if (tripleMakers.size() > 0) {
           return true;
       } else {
           return false;
       }
    }

    /** Checks if there are more triples. */
    protected boolean hasNext() {
    	if (hasTripleChached) {
    		return true;
    	}
    	Triple test = next();
        if (test == null) {
        	return false;
        }
        hasTripleChached = true;
        chachedTriple = test;
        return true;
    }

    /** Returns the next triple.
     * If there are no more triple makers for the current row of the result set
     * then the next row is cached and the triple makers iterator is reset.
     */
    protected Triple next() {
    	if (!hasTripleMakers()) {
    		return null;
    	}
		if (hasTripleChached) {
			hasTripleChached = false;
			return chachedTriple;
		}
        if (!queryHasBeenExecuted) {
            executeSQLQuery();
        }
		if (rsForward && !tripleMakerIterator.hasNext()) {
			rsForward = false;
		}
		if (!rsForward) {
			try {
		        if (!resultSet.next()) {
					resultSet.close();
					queryHasBeenExecuted = false;
					rsForward = false;
					return null;        	
				}
				cacheCurrentRow();
				tripleMakerIterator = tripleMakers.iterator();
				rsForward = true;
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
				ex.printStackTrace();
			}
		}
		TripleMaker tripMaker = (TripleMaker) tripleMakerIterator.next();
		Triple triple = tripMaker.makeTriple(currentRow, columnNameNumberMap);
		if (triple == null || deliveredTriples.contains(triple.toString())) {
			triple = next();
		} else {
			deliveredTriples.add(triple.toString());
		}
		return triple;
    }

    protected void executeSQLQuery() {
		if (debug) {
            System.out.println("--------------------------------------------");
            System.out.println("SQL statement executed: " + sql);
            System.out.println("--------------------------------------------");
        }
        try {
			Connection con = this.database.getConnnection();
			// Create and execute SQL statement
			java.sql.Statement stmt = con.createStatement();
			this.resultSet = stmt.executeQuery(sql);
			this.numCols = this.resultSet.getMetaData().getColumnCount();
			this.queryHasBeenExecuted = true;
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    protected void close() {
        try {
		  this.resultSet.close();
		  this.resultSet = null;
	    } catch (SQLException ex) {
            // System.err.println(ex.getMessage());
            // ex.printStackTrace();
		}
        queryHasBeenExecuted = false;
    }

    /** Caches the current row from the result set in an array which is passed to the triple makers */
    protected void cacheCurrentRow() {
        currentRow = new String[20];
        try {
			int numCols = resultSet.getMetaData().getColumnCount();
			for (int i = 1; i <= numCols; i++) {
				currentRow[i] = resultSet.getString(i);
			}
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
