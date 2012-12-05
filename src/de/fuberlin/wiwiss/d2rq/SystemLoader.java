package de.fuberlin.wiwiss.d2rq;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openjena.atlas.AtlasException;
import org.openjena.riot.RiotException;

import com.hp.hpl.jena.n3.turtle.TurtleParseException;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;

import de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.mapgen.Filter;
import de.fuberlin.wiwiss.d2rq.mapgen.MappingGenerator;
import de.fuberlin.wiwiss.d2rq.mapgen.W3CMappingGenerator;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.server.ConfigLoader;
import de.fuberlin.wiwiss.d2rq.server.D2RServer;
import de.fuberlin.wiwiss.d2rq.server.JettyLauncher;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.SQLScriptLoader;

/**
 * Factory for MappingGenerators, ModelD2RQs and the like.
 * Many of these artifacts can be configured in multiple ways
 * (from the command line, from configuration files, etc.), and
 * creating one may require that others are previously created
 * and configured correctly. This class helps setting everything
 * up correctly.
 * 
 * TODO: {@link MapParser#absolutizeURI(String)} and {@link ConfigLoader#toAbsoluteURI(String)} and {WebappInitListener#absolutize} need to be consolidated and/or folded into this class
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class SystemLoader {
	
	static {
		ARQ.init();	// Wire RIOT into Jena, etc.
	}
	
	private final static Log log = LogFactory.getLog(SystemLoader.class);
	
	private static final String DEFAULT_PROTOCOL = "http";
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 2020;

	public static final String DEFAULT_BASE_URI = DEFAULT_PROTOCOL + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/";
	public static final String DEFAULT_JDBC_URL = "jdbc:hsqldb:mem:temp";

	private String username = null;
	private String password = null;
	private String jdbcDriverClass = null;
	private String sqlScript = null;
	private boolean generateDirectMapping = false;
	private String jdbcURL = null;
	private String mappingFile = null;
	private String baseURI = null;
	private String resourceStem = "";
	private Filter filter = null;
	private boolean fastMode = false;
	private int port = -1;
	private int resultSizeLimit = Database.NO_LIMIT;
	
	private ConnectedDB connectedDB = null;
	private MappingGenerator generator = null;
	private Model mapModel = null;
	private Mapping mapping = null;
	private ModelD2RQ dataModel = null;
	private GraphD2RQ dataGraph = null;
	private JettyLauncher jettyLauncher = null;
	private ConfigLoader serverConfig = null;
	private D2RServer d2rServer = null;
	private ClassMapLister classMapLister = null;
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setFilter(Filter filter) {
		this.filter = filter;
	}
	
	public void setJDBCDriverClass(String driver) {
		this.jdbcDriverClass = driver;
		ConnectedDB.registerJDBCDriver(driver);
	}
	
	public void setStartupSQLScript(String sqlFile) {
		this.sqlScript = sqlFile;
	}

	public void setGenerateW3CDirectMapping(boolean flag) {
		this.generateDirectMapping = flag;
	}
	
	public void setJdbcURL(String jdbcURL) {
		this.jdbcURL = jdbcURL;
	}
	
	public void setMappingFileOrJdbcURL(String value) {
		if (value.toLowerCase().startsWith("jdbc:")) {
			jdbcURL = value;
		} else {
			mappingFile = value;
		}
	}
	
	public void setSystemBaseURI(String baseURI) {
		if (!java.net.URI.create(baseURI).isAbsolute()) {
			throw new D2RQException("Base URI '" + baseURI + "' must be an absolute URI",
					D2RQException.STARTUP_BASE_URI_NOT_ABSOLUTE);
		}
		this.baseURI = baseURI;
	}
	
	/**
	 * By default, the base URI for resolving relative URIs
	 * in data is the same as the system base URI where the server
	 * is assumed to run.
	 * 
	 * The resource stem can be set to something like <code>resource/</code>
	 * in order to put the resources into a subdirectory of the
	 * system base.
	 * 
	 * @param value A string relative to the system base URI
	 */
	public void setResourceStem(String value) {
		resourceStem = value;
	}
	
	/**
	 * @return Base URI where the server is assumed to run
	 */
	public String getSystemBaseURI() {
		if (baseURI != null) {
			return MapParser.absolutizeURI(baseURI);
		}
		if (getServerConfig() != null && serverConfig.baseURI() != null) {
			return serverConfig.baseURI();
		}
		if (getPort() == 80) {
			return DEFAULT_PROTOCOL + "://" + DEFAULT_HOST + "/";
		}
		return DEFAULT_PROTOCOL + "://" + DEFAULT_HOST + ":" + getPort() + "/";
	}
	
	/**
	 * @return Base URI for making relative URIs in the RDF data absolute
	 */
	public String getResourceBaseURI() {
		return getSystemBaseURI() + resourceStem;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public int getPort() {
		int effectivePort = port;
		if (effectivePort == -1 && getServerConfig() != null) {
			effectivePort = getServerConfig().port();
		}
		if (effectivePort == -1) {
			return DEFAULT_PORT;
		}
		return effectivePort;
	}
	
	public void setFastMode(boolean flag) {
		this.fastMode = flag;
	}
	
	public void setMappingURL(String mappingURL) {
		this.mappingFile = mappingURL;
	}

	public void setResultSizeLimit(int value) {
		this.resultSizeLimit = value;
	}
	
	private ConnectedDB getConnectedDB() {
		if (connectedDB == null) {
			connectedDB = new ConnectedDB(jdbcURL, username, password);
			if (sqlScript != null) {
				try {
					SQLScriptLoader.loadFile(new File(sqlScript), connectedDB.connection());
				} catch (IOException ex) {
					connectedDB.close();
					throw new D2RQException(
							"Error accessing SQL startup script: " + sqlScript,
							D2RQException.STARTUP_SQL_SCRIPT_ACCESS);
				} catch (SQLException ex) {
					connectedDB.close();
					throw new D2RQException(
							"Error importing " + sqlScript + " " + ex.getMessage(),
							D2RQException.STARTUP_SQL_SCRIPT_SYNTAX);
				}
			}
		}
		return connectedDB;
	}

	/**
	 * Returns a mapping generator. Needs to be explicitly closed
	 * using {@link #closeMappingGenerator()}.
	 */
	public MappingGenerator openMappingGenerator() {
		if (generator == null) {
			generator = generateDirectMapping ?
					new W3CMappingGenerator(getConnectedDB()) :
					new MappingGenerator(getConnectedDB());
			if (jdbcDriverClass != null) {
				generator.setJDBCDriverClass(jdbcDriverClass);
			}
			if (filter != null) {
				generator.setFilter(filter);
			}
			if (sqlScript != null) {
				// If there's a startup SQL script, copy its name into the generated mapping
				generator.setStartupSQLScript(new File(sqlScript).toURI());
			}
		}
		return generator;
	}

	public void closeMappingGenerator() {
		if (connectedDB != null) {
			connectedDB.close();
		}
	}

	public Model getMappingModel() {
		if (mapModel == null) {
			if (jdbcURL != null && mappingFile != null) {
				throw new D2RQException("conflicting mapping locations " + mappingFile + " and " + jdbcURL + "; specify at most one");
			}
			if (jdbcURL == null && mappingFile == null) {
				throw new D2RQException("no mapping file or JDBC URL specified");
			}
			if (jdbcURL != null) {
				mapModel = openMappingGenerator().mappingModel(getResourceBaseURI());
			} else {
				log.info("Reading mapping file from " + mappingFile);
				// Guess the language/type of mapping file based on file extension. If it is not among the known types then assume that the file has TURTLE syntax and force to use TURTLE parser
				String lang = FileUtils.guessLang(mappingFile, "unknown");
				try {
					if (lang.equals("unknown")) {
						mapModel = FileManager.get().loadModel(mappingFile, getResourceBaseURI(), "TURTLE");
					} else {
						// if the type is known then let Jena auto-detect it and load the appropriate parser
						mapModel = FileManager.get().loadModel(mappingFile, getResourceBaseURI(), null);
					}
				} catch (TurtleParseException ex) {
					// We have wired RIOT into Jena in the static initializer above,
					// so this should never happen (it's for the old Jena Turtle/N3 parser)
					throw new D2RQException(
							"Error parsing " + mappingFile + ": " + ex.getMessage(), ex, 77);
				} catch (JenaException ex) {
					if (ex.getCause() != null && ex.getCause() instanceof RiotException) {
						throw new D2RQException(
								"Error parsing " + mappingFile + ": " + ex.getCause().getMessage(), ex, 77);
					}
					throw ex;
				} catch (AtlasException ex) {
					// Detect the specific case of non-UTF-8 encoded input files
					// and do a custom error message
					if (FileUtils.langTurtle.equals(lang) 
							&& ex.getCause() != null && (ex.getCause() instanceof MalformedInputException)) {
						throw new D2RQException("Error parsing " + mappingFile + 
								": Turtle files must be in UTF-8 encoding; " +
								"bad encoding found at byte " + 
								((MalformedInputException) ex.getCause()).getInputLength(), ex, 77);
					}
					// Generic error message for other parse errors
					throw new D2RQException(
							"Error parsing " + mappingFile + ": " + ex.getMessage(), ex, 77);
				}
			}
		}
		return mapModel;
	}

	public Mapping getMapping() {
		if (mapping == null) {
			mapping = new MapParser(getMappingModel(), getResourceBaseURI()).parse();
			mapping.configuration().setUseAllOptimizations(fastMode);
			if (connectedDB != null) {
				// Hack! We don't want the Database to open another ConnectedDB,
				// so we check if it's connected to the same DB, and in that case
				// make it use the existing ConnectedDB that we already have opened.
				// Otherwise we get problems where D2RQ is trying to import a SQL
				// script twice on startup.
				for (Database db: mapping.databases()) {
					if (db.getJDBCDSN().equals(connectedDB.getJdbcURL())) {
						if (resultSizeLimit != Database.NO_LIMIT) {
							db.setResultSizeLimit(resultSizeLimit);
						}
						db.useConnectedDB(connectedDB);
					}
				}
			}
		}
		return mapping;
	}

	public ModelD2RQ getModelD2RQ() {
		if (dataModel == null) {
			dataModel = new ModelD2RQ(getMapping());
		}
		return dataModel;
	}

	public GraphD2RQ getGraphD2RQ() {
		if (dataGraph == null) {
			dataGraph = (GraphD2RQ) getModelD2RQ().getGraph();
		}
		return dataGraph;
	}
	
	public ClassMapLister getClassMapLister() {
		if (classMapLister == null) {
			classMapLister = new ClassMapLister(getMapping());
		}
		return classMapLister;
	}
	
	public JettyLauncher getJettyLauncher() {
		if (jettyLauncher == null) {
			jettyLauncher = new JettyLauncher(this, getPort());
		}
		return jettyLauncher;
	}
	
	public ConfigLoader getServerConfig() {
		if (serverConfig == null) {
			// TODO Use mapModel instead of parsing RDF again
			serverConfig = new ConfigLoader(
					mappingFile == null ? null : ConfigLoader.toAbsoluteURI(mappingFile));
			serverConfig.load();
		}
		return serverConfig;
	}
	
	public D2RServer getD2RServer() {
		if (d2rServer == null) {
			d2rServer = new D2RServer(this);
			if (baseURI != null || 
					(getServerConfig() != null && getServerConfig().baseURI() == null)) {
				d2rServer.overrideBaseURI(getSystemBaseURI());
			}
		}
		return d2rServer;
	}
	
	public void resetMappingFile() {
		mapModel = null;
		mapping = null;
		dataModel = null;
		if (dataGraph != null) dataGraph.close();
		dataGraph = null;
		classMapLister = null;
	}
}
