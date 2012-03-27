package de.fuberlin.wiwiss.d2rq;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * @author jgarbers
 */
public class DBConnectionTest extends TestCase {

	private Model mapModel;

	private Collection<Database> databases;
	private Database firstDatabase;
	private ConnectedDB cdb;
	
	private String simplestQuery;
	private String mediumQuery;
	private String complexQuery;

	protected void setUp() throws Exception {
		mapModel = ModelFactory.createDefaultModel();
		mapModel.read(D2RQTestSuite.ISWC_MAP, "http://test/", "TURTLE");
		MapParser parser = new MapParser(mapModel, null);
		databases = parser.parse().databases();
		firstDatabase = (Database)databases.iterator().next();
		simplestQuery = "SELECT 1;";
		// mediumQuery = "SELECT PaperID from papers";
		mediumQuery = "SELECT DISTINCT papers.PaperID FROM rel_paper_topic, papers, topics WHERE papers.PaperID=rel_paper_topic.PaperID AND rel_paper_topic.TopicID=topics.TopicID AND topics.TopicID=3 AND rel_paper_topic.RelationType = 1 AND papers.Publish = 1;";
		complexQuery = "SELECT T0_Papers.PaperID, T0_Persons.URI, T1_Persons.Email, T1_Persons.URI FROM persons AS T1_Persons, papers AS T0_Papers, rel_person_paper AS T0_Rel_Person_Paper, persons AS T0_Persons WHERE T0_Persons.PerID=T0_Rel_Person_Paper.PersonID AND T0_Papers.PaperID=T0_Rel_Person_Paper.PaperID AND T0_Papers.Publish = 1 AND T0_Persons.URI=T1_Persons.URI AND (NOT (CONCAT('http://www.conference.org/conf02004/paper#Paper' , CAST(T0_Papers.PaperID AS char) , '') = T0_Persons.URI));";

	}

	protected void tearDown() throws Exception {
		mapModel.close();
		if (cdb != null) cdb.close();
	}

	public void testConnections() throws SQLException {
		for (Database db: databases) {
			cdb = db.connectedDB();
			Connection c = cdb.connection();
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
		
		rs.close();
		s.close();
		return query_results;
	}

	public Connection manuallyConfiguredConnection() {
		String driverClass;
		String url;
		String name;
		String pass;

		driverClass = "com.mysql.jdbc.Driver";
		url = "jdbc:mysql:///iswc";
		name = "root"; //  "@localhost";
		pass = ""; // "";

		Connection c=null;
		try {
			Class.forName(driverClass);
			c = DriverManager.getConnection(url, name, pass);
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
		// String query = "select PaperID from papers";
		String query = "SELECT papers.PaperID, papers.Year FROM papers WHERE papers.Year=2002 AND papers.PaperID = 2 AND papers.Publish = 1;";
    
		String query_results = performQuery(c, query);
		c.close();
		assertEquals(query_results,"2 2002");
	}
	
	public void testDistinct() throws SQLException {
	    // there seems to be a problem with MSAccess databases
	    // when using the DISTINCT keyword, Strings are truncated to 256 chars
		cdb = firstDatabase.connectedDB();
		Connection c = cdb.connection();
	    //Connection c=manuallyConfiguredConnection();
		String nonDistinct = "SELECT T0_Papers.Abstract FROM papers AS T0_Papers WHERE T0_Papers.PaperID=1 AND T0_Papers.Publish = 1;";
		String distinct = "SELECT DISTINCT T0_Papers.Abstract FROM papers AS T0_Papers WHERE T0_Papers.PaperID=1 AND T0_Papers.Publish = 1;";
		String distinctResult = performQuery(c, distinct);
		String nonDistinctResult = performQuery(c, nonDistinct);
		c.close();
		assertEquals(distinctResult,nonDistinctResult);
	}
	
	// fails with wrong MSAccess Iswc DB (doc/manual/ISWC.mdb revision < 1.5)
	// succeeds with revision 1.5
	public void testMedium() throws SQLException {
		cdb = firstDatabase.connectedDB();
		Connection c = cdb.connection();
	    //Connection c=manuallyConfiguredConnection(); // 2 is ok, 1 fails
		String query = mediumQuery;
		String query_results = performQuery(c, query);
		c.close();
		assertNotNull(query_results);
	}

	// fails with MSAccess
	public void testLongComplexSQLQuery() throws SQLException {
		cdb = firstDatabase.connectedDB();
		Connection c = cdb.connection();
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