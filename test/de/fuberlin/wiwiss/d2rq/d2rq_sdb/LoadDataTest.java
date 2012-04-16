package de.fuberlin.wiwiss.d2rq.d2rq_sdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sdb.SDBFactory;
import com.hp.hpl.jena.sdb.Store;
import com.hp.hpl.jena.sdb.StoreDesc;
import com.hp.hpl.jena.sdb.layout2.index.StoreTriplesNodesIndexHSQL;
import com.hp.hpl.jena.sdb.sql.JDBC;
import com.hp.hpl.jena.sdb.sql.SDBConnection;
import com.hp.hpl.jena.sdb.store.DatabaseType;
import com.hp.hpl.jena.sdb.store.LayoutType;

import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import junit.framework.TestCase;

/**
 * Test for loading the sql-data in the derby-database and the turtle-data in the sdb
 * 
 * @author Herwig Leimer
 *
 */
public abstract class LoadDataTest extends TestCase 
{
	private static boolean loadHsqlData = true;
	private static boolean loadSDBData = true;
	// directories and data-files and config-files
	protected static final String CURR_DIR = "test/de/fuberlin/wiwiss/d2rq/d2rq_sdb";
	private static final String DATA_DIR = "dataset";
	private static final String CONFIG_DIR = "config";
	private static final String FILENAME_TTL_DATA = "dataset.ttl.zip";
	private static final String FILENAME_SQL_DATA = "dataset.sql.zip";
	private static final String MAPPING_FILE_HSQL = "d2r-hsql-mapping.n3";
	// sdb-config
	private static final String SDB_URL = "jdbc:hsqldb:mem:sdbdata";
	private static final String SDB_USER = "sa";
	private static final String SDB_PASS = "";
	protected Model sdbDataModel;
	// hsql-config
	private static final String HSQL_DRIVER_NAME = "org.hsqldb.jdbcDriver";
	private static final String HSQL_URL = "jdbc:hsqldb:mem:hsqldata;create=true";
	private static final String HSQL_USER = "sa";
	private static final String HSQL_PASS = "";
	
	protected Model hsqlDataModel;
	
	
	/**
	 * Constructor
	 * Inits the database and loads the data
	 */
	public LoadDataTest()
	{
		initDatabases();
	}
	
	/**
	 * Inits the databases.
	 * The init-process can be managed with the boolean 
	 * flags loadDerbyData and loadSDBData
	 */
	private void initDatabases()
	{
		try
		{
			if (loadHsqlData)
			{
				createHsqlDatabase();
				assertNotNull("Hsql-DataModel is not null", hsqlDataModel);
			}
			
			if (loadSDBData)
			{
				createSemanticDatabase();
				assertNotNull("SDBDataModel is not null", sdbDataModel);
				assertTrue("There is some data in the SDBDataModel", sdbDataModel.size() > 0);
			}
			
			System.out.println("-----------------------------------------------------------");
			
		} catch (SQLException e) 
		{
			e.printStackTrace();
			fail();
		} catch (IOException e) 
		{
			e.printStackTrace();
			fail();
		}
	}
	
	/**
	 * Creates a new in-memory-hsql-database and puts all data that  
	 * the files in the zip-archive contain into.
	 */
	private void createHsqlDatabase() throws IOException, SQLException
	{	
		Connection hsqlConnection;
		File zipFile;
        ZipEntry entry;
        ZipInputStream zipInputStream = null; 
        String sqlData;
        Statement statement;
        
		try 
		{
            Class.forName(HSQL_DRIVER_NAME);
        } catch (ClassNotFoundException e) 
        {
            throw new SQLException(e.getMessage());
        }
        
        hsqlConnection = DriverManager.getConnection(HSQL_URL, HSQL_USER, HSQL_PASS);
         
        // load all data from dataset.ttl.zip
		zipFile = new File(CURR_DIR + "/" + DATA_DIR + "/" + FILENAME_SQL_DATA);        
        zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
        
        if (zipInputStream != null)
        {
            try
            {
	            // so that only one ttl file could be into the zip-file
	        	while ((entry = zipInputStream.getNextEntry()) != null) 
	    		{ 
	            	System.out.println("Loading Data from " + entry.getName());
	        	    sqlData = convertStreamToString(zipInputStream);
	    			statement = hsqlConnection.createStatement();
	    			statement.execute(sqlData);
	    			statement.close();
	    		}
            }finally
            {
            	zipInputStream.close();
            }
        }
        
        hsqlDataModel = new ModelD2RQ(CURR_DIR + "/" + CONFIG_DIR + "/" + MAPPING_FILE_HSQL, "N3", "http://test/");
     
        System.out.println("Loaded SQL-Data in HSQL-DATABASE!");
	}
	
	
	/**
	 * Creates a new sdb and put the data from dataset.ttl.zip into.
	 */
	private void createSemanticDatabase() throws IOException
	{
	    File zipFile;
        ZipEntry entry;
        ZipInputStream zipInputStream = null;      
        SDBConnection sdbConnection;
        StoreDesc sdbStoreDesc;
        Store sdbStore;

        // create hsql-in-memory-database
        JDBC.loadDriverHSQL();
        sdbConnection = SDBFactory.createConnection(SDB_URL, SDB_USER, SDB_PASS);
		sdbStoreDesc = new StoreDesc(LayoutType.LayoutTripleNodesIndex, DatabaseType.HSQLDB) ;
		sdbStore = new StoreTriplesNodesIndexHSQL(sdbConnection, sdbStoreDesc);
		sdbStore.getTableFormatter().create();
		sdbDataModel = SDBFactory.connectDefaultModel(sdbStore);
        
		// load all data from dataset.ttl.zip
		zipFile = new File(CURR_DIR + "/" + DATA_DIR + "/" + FILENAME_TTL_DATA);        
        zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
        
        if (zipInputStream != null)
        {
            // NOTE: sdbModel.read closes the inputstream !!!!
            // so that only one ttl file could be into the zip-file
            entry = zipInputStream.getNextEntry();
            
            if (entry != null)
            { 
            	assertFalse("Entry-Name is not empty", "".equals(entry.getName()));
            	sdbDataModel = sdbDataModel.read(zipInputStream, null, "TTL");
                assertTrue("sdbModel is not emtpy", sdbDataModel.size() > 0);
            }
            zipInputStream.close();
        }
        
        System.out.println("Loaded " + sdbDataModel.size() + " Tripples into SDB-Database!");
	}
	
	
	private String convertStreamToString(InputStream inputStream) throws IOException 
	{
        BufferedReader bufferedReader;
        StringBuilder stringBuilder;
        String line = null;
        
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        stringBuilder = new StringBuilder();
 
        
        while ((line = bufferedReader.readLine()) != null) 
        {
        	stringBuilder.append(line);
        	stringBuilder.append("\n");
        }
 
        return stringBuilder.toString();
    }
}
