/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import java.util.*;
import java.io.*;
//import java.sql.*;

/** SQLStatementMakers collect SELECT and WHERE elements and deliver a corresponding SQL statement.
 * They are used within TripleResultSets.
 *
 * <BR>History: 06-07-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */

public class SQLStatementMaker {
     ArrayList sqlSelect;
     ArrayList sqlFrom;
     ArrayList sqlWhere;
     /** Maps column names from the database to columns numbers in the result set. */
     HashMap columnNameNumber;

protected SQLStatementMaker() {
     sqlSelect = new ArrayList(10);
     sqlFrom = new ArrayList(5);
     sqlWhere = new ArrayList(15);
    }

    protected String getSQLStatement() {
        String statement = "SELECT DISTINCT ";
        columnNameNumber = new HashMap(10);
        Iterator it = sqlSelect.iterator();
        int i = 1;
        while (it.hasNext()) {
            String columnname = (String) it.next();
            columnNameNumber.put(columnname, Integer.toString(i));
            statement = statement + columnname  + ", ";
            i++;
        }
        statement = statement.substring(0, statement.length() - 2);
        statement = statement + " FROM ";
        it = sqlFrom.iterator();
        while (it.hasNext()) {
            statement = statement +  it.next() + ", ";
        }
        statement = statement.substring(0, (statement.length() - 2));
        it = sqlWhere.iterator();
        if (it.hasNext()) {
            statement = statement + " WHERE ";
			while (it.hasNext()) {
				statement = statement + (String) it.next() + " AND ";
			}
			statement = statement.substring(0, statement.length() - 5);
        }
        statement = statement + ";";
        return statement;
    }
    protected void addSelectColumn(String select) {
        if (!sqlSelect.contains(select)) {
            sqlSelect.add(select);
			String table = D2RQUtil.getTableName(select);
			if (!sqlFrom.contains(table)) {
				sqlFrom.add(table);
			}
        }
    }
    protected void addWhereClause(String where) {
        if (!sqlWhere.contains(where)) {
            sqlWhere.add(where);
            String table = D2RQUtil.getTableName(where);
            if (!sqlFrom.contains(table)) {
                sqlFrom.add(table);
            }
        }
    }
    protected void addJoins(ArrayList joins) {
        Iterator it = joins.iterator();
        while (it.hasNext()) {
           String join = (String) it.next();
		   if (!sqlWhere.contains(join)) {
				sqlWhere.add(join);
                // Split join into two column names
                String firstcolumn = join.substring(0, join.indexOf("="));
                String secondcolumn = join.substring(0, join.indexOf("=") + 1);
                // Add tables to where clause
                String table = D2RQUtil.getTableName(firstcolumn.trim());
                if (!sqlFrom.contains(table)) {
                      sqlFrom.add(table); }
                table = D2RQUtil.getTableName(secondcolumn.trim());
                if (!sqlFrom.contains(table)) {
                      sqlFrom.add(table); }
			}
        }
    }

    protected HashMap getColumnNameNumberMap() { return columnNameNumber; }
}
