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

import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;

/**
 * @author jgarbers
 * @version $Id: DBConnectionTest.java,v 1.21 2006/09/06 21:48:47 cyganiak Exp $
 */
public class DBConnectionTest extends TestCase {

	private Model mapModel;

	private MapParser parser;

	private Collection databases;
	private Database firstDatabase;

	private String simplestQuery;
	private String mediumQuery;
	private String complexQuery;

	protected void setUp() throws Exception {
		mapModel = ModelFactory.createDefaultModel();
		mapModel.read(D2RQTestSuite.ISWC_MAP, "http://test/", "N3");
		parser = new MapParser(mapModel, null);
		parser.parse();
		databases = parser.getDatabases();
		firstDatabase = (Database)databases.iterator().next();
		simplestQuery = "SELECT 1;";
		// mediumQuery = "SELECT PaperID from PAPERS";
		mediumQuery = "SELECT DISTINCT Papers.PaperID FROM Rel_Paper_Topic , Papers , Topics WHERE Papers.PaperID=Rel_Paper_Topic.PaperID AND Rel_Paper_Topic.TopicID=Topics.TopicID AND Topics.TopicID=3 AND Rel_Paper_Topic.RelationType = 1 AND Papers.Publish = 1;";
		complexQuery = "SELECT T0_Papers.PaperID, T0_Persons.URI, T1_Persons.Email, T1_Persons.URI FROM Persons AS T1_Persons , Papers AS T0_Papers , Rel_Person_Paper AS T0_Rel_Person_Paper , Persons AS T0_Persons WHERE T0_Persons.PerID=T0_Rel_Person_Paper.PersonID AND T0_Papers.PaperID=T0_Rel_Person_Paper.PaperID AND T0_Papers.Publish = 1 AND T0_Persons.URI=T1_Persons.URI AND (NOT (CONCAT('http://www.conference.org/conf02004/paper#Paper' , CAST(T0_Papers.PaperID AS char) , '') = T0_Persons.URI));";

	}

	protected void tearDown() throws Exception {
		mapModel.close();
	}

	public void testConnections() throws SQLException {
		Iterator it = databases.iterator();
		while (it.hasNext()) {
			Database db = (Database) it.next();
			Connection c = db.getConnnection();
			String result = performQuery(c, simplestQuery); // 
			assertEquals(result, "1");
		}
	}
	
	private static String performQuery(Connection c, String theQuery) throws SQLException {
		String query_results = "";
		Statement s;
		ResultSet rs;
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
		return query_results;
	}

	public Connection manuallyConfiguredConnection() {
		final int configure = 3; // TODO change this to your local DB configuration
		String driverClass;
		String url;
		String name;
		String pass;

		if (configure == 1) { // omit ODBC. (You must install jdbc driver in advance)
			driverClass = "com.mysql.jdbc.Driver";
			url = "jdbc:mysql:///iswc";
			name = "root"; //  "@localhost";
			pass = ""; // "";
		} else if (configure == 2) { // use Mysql on ODBC (Windows) (name and passwd may be not necessary)
			driverClass = "sun.jdbc.odbc.JdbcOdbcDriver";
			url = "jdbc:odbc:myiswc";
			name = "jg";
			pass = "";
		} else if (configure == 3) { // use MSAccess ODBC (Windows)
			driverClass = "sun.jdbc.odbc.JdbcOdbcDriver";
			url = "jdbc:odbc:IswcDB";
		} else
			return null;

		Connection c=null;
		try {
			Class.forName(driverClass);
			if (configure == 1)
				c = DriverManager.getConnection(url, name, pass);
			else if (configure == 2 || configure == 3)
				c = DriverManager.getConnection(url);
			return c;
		} //end try
		catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}
	
	// without declarations in 
	public void xtestManuallyConfiguredConnection() throws SQLException {
	    Connection c=manuallyConfiguredConnection();
		// String query = simplestQuery;
		// String query = "select PaperID from Papers";
		String query = "SELECT Papers.PaperID, Papers.Year FROM Papers WHERE Papers.Year=2002 AND Papers.PaperID = 2 AND Papers.Publish = 1;";
    
		String query_results = performQuery(c, query);
		c.close();
		assertEquals(query_results,"2 2002");
	}
	
	public void testDistinct() throws SQLException {
	    // there seems to be a problem with MSAccess databases
	    // when using the DISTINCT keyword, Strings are truncated to 256 chars
	    Connection c = firstDatabase.getConnnection(); 
	    //Connection c=manuallyConfiguredConnection();
		String nonDistinct = "SELECT T0_Papers.Abstract FROM Papers AS T0_Papers WHERE T0_Papers.PaperID=1 AND T0_Papers.Publish = 1;";
		String distinct = "SELECT DISTINCT T0_Papers.Abstract FROM Papers AS T0_Papers WHERE T0_Papers.PaperID=1 AND T0_Papers.Publish = 1;";
		String distinctResult = performQuery(c, distinct);
		String nonDistinctResult = performQuery(c, nonDistinct);
		c.close();
		if (!distinctResult.equals(nonDistinctResult)) {
		    if (firstDatabase.correctlyHandlesDistinct()) {
		    	fail("testDistinct() has a mismatch." +
		               " Please use a better Database or " +
		               "put into your Database specification " +
		               "d2rq:allowDistinct \"true\".");
		       assertEquals(distinctResult,nonDistinctResult);
		    }
		}
	}
	
	// fails with wrong MSAccess Iswc DB (doc/manual/ISWC.mdb revision < 1.5)
	// succeeds with revision 1.5
	public void testMedium() throws SQLException {
	     Connection c = firstDatabase.getConnnection(); 
	    //Connection c=manuallyConfiguredConnection(); // 2 is ok, 1 fails
		String query = mediumQuery;
		String query_results = performQuery(c, query);
		c.close();
		assertNotNull(query_results);
	}

	// fails with MSAccess
	public void testLongComplexSQLQuery() throws SQLException {
	     Connection c = firstDatabase.getConnnection(); 
	    //Connection c=manuallyConfiguredConnection(); // 2 is ok, 1 fails
		String query = complexQuery;
		try {
			performQuery(c, query);
		} catch (SQLException e) {
			fail("DBConnectionTest.testLong() is known to fail with MSAccess");
		} finally {
			c.close();
		}
		// assertEquals(query_results,"2 2002");
	}

	
	
}