/*
 * Created on 24.01.2005 by Joerg Garbers, FU-Berlin
 *
 */
package de.fuberlin.wiwiss.d2rq;

import junit.framework.TestCase;
import java.net.URL;
import java.sql.*;
import sun.jdbc.odbc.*;
import java.awt.event.*;
import java.awt.*;

/**
 * @author jgarbers
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class DBConnectionTest extends TestCase {

    public DBConnectionTest() {
        super();
        // TODO Auto-generated constructor stub
    }

    public DBConnectionTest(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    public void testConnection() {
        Connection c;
        String name = "x";
        String pass = "y";
        String url = "jdbc:odbc:IswcDB";
        // String query = "select PaperID from Papers";
        String query = "SELECT 1 FROM Papers WHERE Papers.Year=2002 AND Papers.PaperID = 2 AND Papers.Publish = 1";
        String query_results = "";
        int col;
        int pos;
        try {
            System.out.println("connecting to the " + name + " data source...");
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            //          c = DriverManager.getConnection(url, name, pass);
            c = DriverManager.getConnection(url);
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery(query);
            col = (rs.getMetaData()).getColumnCount();
            while (rs.next()) {
                for (pos = 1; pos <= col; pos++) {
                    query_results += rs.getString(pos) + " ";
                }
            } // end while
            System.out.println(query_results);
        } //end try
        catch (Exception x) {
            x.printStackTrace();
        }
    }
}