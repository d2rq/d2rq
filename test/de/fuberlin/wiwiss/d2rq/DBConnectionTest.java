/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import de.fuberlin.wiwiss.d2rq.Database;
import de.fuberlin.wiwiss.d2rq.MapParser;


/**
 * @author jgarbers
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class DBConnectionTest extends TestCase {

	private Model mapModel;

	private MapParser parser;

	private Collection databases;

	private String simplestQuery;

	public DBConnectionTest() {
		super();
		// TODO Auto-generated constructor stub
	}

	public DBConnectionTest(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	protected void setUp() throws Exception {
		mapModel = ModelFactory.createDefaultModel();
		mapModel.read(TestFramework.D2RQMap, "N3");
		parser = new MapParser(mapModel);
		parser.parse();
		databases = parser.getDatabases();
		simplestQuery = "SELECT 1;";
	}

	protected void tearDown() throws Exception {
		mapModel.close();
	}

	public void testConnections() {
		Iterator it = databases.iterator();
		while (it.hasNext()) {
			Database db = (Database) it.next();
			System.out.println("Testing Database " + db.toString());
			Connection c = db.getConnnection();
			String result = performQuery(c, simplestQuery);
			assertEquals(result, "1");
		}
	}
	
	private static String performQuery(Connection c, String theQuery) {
		String query_results = "";
		Statement s;
		ResultSet rs;
		try {
			s = c.createStatement();
			rs = s.executeQuery(theQuery);
			int col = (rs.getMetaData()).getColumnCount();
			while (rs.next()) {
				for (int pos = 1; pos <= col; pos++) {
					if (pos>1)
						query_results+=" ";
					query_results += rs.getString(pos);
				}
			} // end while
		} catch (SQLException e) {
			query_results = null;
		}
		return query_results;
	}

	public Connection manuallyConfiguredConnection() {
		int configure = 2;
		String driverClass;
		String url;
		String name;
		String pass;

		if (configure == 1) {
			driverClass = "com.mysql.jdbc.Driver";
			url = "jdbc:mysql:///iswc";
			name = ""; //  "@localhost";
			pass = ""; // "";
		} else if (configure == 2) {
			driverClass = "sun.jdbc.odbc.JdbcOdbcDriver";
			url = "jdbc:odbc:IswcDB";
			name = "x";
			pass = "y";
		} else
			return null;

		Connection c=null;
		try {
			System.out.println("connecting to the " + url + " data source...");
			Class.forName(driverClass);
			if (configure == 1)
				c = DriverManager.getConnection(url, name, pass);
			else if (configure == 2)
				c = DriverManager.getConnection(url);
			return c;
		} //end try
		catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}
	
	// without declarations in 
	public void testManuallyConfiguredConnection() throws SQLException {
	    Connection c=manuallyConfiguredConnection();
		// String query = simplestQuery;
		// String query = "select PaperID from Papers";
		String query = "SELECT Papers.PaperID, Papers.Year FROM Papers WHERE Papers.Year=2002 AND Papers.PaperID = 2 AND Papers.Publish = 1;";
    
		String query_results = performQuery(c, query);
		System.out.println(query_results);
		c.close();
		assertEquals(query_results,"2 2002");
	}
	
	public void testDistinct() throws SQLException {
	    // there seems to be a problem with MSAccess databases
	    // when using the DISTINCT keyword, Strings are truncated to 256 chars
	    Connection c=manuallyConfiguredConnection();
		String nonDistinct = "SELECT T0_Papers.Abstract FROM Papers AS T0_Papers WHERE T0_Papers.PaperID=1 AND T0_Papers.Publish = 1;";
		String distinct = "SELECT DISTINCT T0_Papers.Abstract FROM Papers AS T0_Papers WHERE T0_Papers.PaperID=1 AND T0_Papers.Publish = 1;";
		String distinctResult = performQuery(c, distinct);
		String nonDistinctResult = performQuery(c, nonDistinct);
		c.close();
		assertEquals(distinctResult,nonDistinctResult);
	}
	
}