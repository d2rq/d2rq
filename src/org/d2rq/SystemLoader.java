package org.d2rq;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.SQLScriptLoader;
import org.d2rq.jena.GraphD2RQ;
import org.d2rq.lang.D2RQCompiler;
import org.d2rq.lang.D2RQReader;
import org.d2rq.lang.D2RQWriter;
import org.d2rq.mapgen.D2RQMappingStyle;
import org.d2rq.mapgen.Filter;
import org.d2rq.mapgen.MappingGenerator;
import org.d2rq.mapgen.DirectMappingStyle;
import org.d2rq.r2rml.MappingValidator;
import org.d2rq.r2rml.R2RMLCompiler;
import org.d2rq.r2rml.R2RMLReader;
import org.d2rq.r2rml.R2RMLWriter;
import org.d2rq.server.ConfigLoader;
import org.d2rq.server.D2RServer;
import org.d2rq.server.JettyLauncher;
import org.d2rq.validation.Report;
import org.d2rq.vocab.D2RQ;
import org.d2rq.vocab.RR;
import org.d2rq.vocab.VocabularySummarizer;
import org.d2rq.writer.MappingWriter;
import org.openjena.atlas.AtlasException;
import org.openjena.riot.RiotException;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.n3.turtle.TurtleParseException;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;


/**
 * Factory for MappingGenerators, ModelD2RQs and the like.
 * Many of these artifacts can be configured in multiple ways
 * (from the command line, from configuration files, etc.), and
 * creating one may require that others are previously created
 * and configured correctly. This class helps setting everything
 * up correctly.
 * 
 * TODO: {@link D2RQReader#absolutizeURI(String)} and {@link ConfigLoader#toAbsoluteURI(String)} and {WebappInitListener#absolutize} need to be consolidated and/or folded into this class
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

	public static CompiledMapping loadMapping(String mappingFileNameOrURL, String baseIRI) {
		SystemLoader loader = new SystemLoader();
		loader.setSystemBaseURI(baseIRI);
		loader.setMappingFile(mappingFileNameOrURL);
		return loader.getMapping();
	}
	
	public static CompiledMapping createMapping(Model mappingModel, String baseIRI) {
		SystemLoader loader = new SystemLoader();
		loader.setSystemBaseURI(baseIRI);
		loader.setMappingModel(mappingModel);
		return loader.getMapping();
	}
	
	public enum MappingLanguage {
		D2RQ("D2RQ Mapping Language"),
		R2RML("R2RML 1.0");
		private String name;
		MappingLanguage(String name) { this.name = name; }
		public String toString() { return name; }
	}
	
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
	private boolean useServerConfig = true;
	private Boolean serveVocabulary = null;
	
	private SQLConnection sqlConnection = null;
	private MappingGenerator generator = null;
	private Model mapModel = null;
	private CompiledMapping mapping = null;
	private R2RMLReader r2rmlReader = null;
	private org.d2rq.r2rml.Mapping r2rmlMapping = null;
	private D2RQReader d2rqReader = null;
	private org.d2rq.lang.Mapping d2rqMapping = null;
	private Model dataModel = null;
	private GraphD2RQ dataGraph = null;
	private JettyLauncher jettyLauncher = null;
	private ConfigLoader serverConfig = null;
	private D2RServer d2rServer = null;
	private Report report = null;
	private MappingWriter writer = null;
	private MappingLanguage mappingLanguage = null;
	
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
	
	public String getJdbcURL() {
		return jdbcURL;
	}
	
	public void setMappingFileOrJdbcURL(String value) {
		if (value.toLowerCase().startsWith("jdbc:")) {
			jdbcURL = value;
		} else {
			mappingFile = value;
		}
	}
	
	public void setUseServerConfig(boolean flag) {
		this.useServerConfig = flag;
	}
	
	public void setServeVocabulary(boolean flag) {
		this.serveVocabulary = flag;
	}
	
	public void setSystemBaseURI(String baseURI) {
		if (baseURI == null) return;
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
			return D2RQReader.absolutizeURI(baseURI);
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
	
	public void setMappingFile(String mappingFile) {
		this.mappingFile = mappingFile;
	}

	public SQLConnection getSQLConnection() {
		if (sqlConnection == null) {
			if (jdbcURL == null) {
				throw new D2RQException("no JDBC URL or SQL script specified");
			}
			sqlConnection = new SQLConnection(jdbcURL, jdbcDriverClass, username, password);
			if (sqlScript != null) {
				try {
					SQLScriptLoader.loadFile(new File(sqlScript), sqlConnection.connection());
				} catch (IOException ex) {
					sqlConnection.close();
					throw new D2RQException(
							"Error accessing SQL startup script: " + sqlScript,
							D2RQException.STARTUP_SQL_SCRIPT_ACCESS);
				} catch (SQLException ex) {
					sqlConnection.close();
					throw new D2RQException(
							"Error importing " + sqlScript + " " + ex.getMessage(),
							D2RQException.STARTUP_SQL_SCRIPT_SYNTAX);
				}
			}
		}
		return sqlConnection;
	}

	public MappingGenerator getMappingGenerator() {
		if (generator == null) {
			generator = generateDirectMapping ?
					new DirectMappingStyle(getSQLConnection()).getMappingGenerator() :
					new D2RQMappingStyle(getSQLConnection()).getMappingGenerator();
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

	public void setMappingModel(Model mapModel) {
		this.mapModel = mapModel;
		mappingLanguage = null;
	}
	
	public Model getMappingModel() {
		if (mapModel == null) {
			if (jdbcURL == null && mappingFile == null) {
				throw new D2RQException("no mapping file or JDBC URL specified");
			}
			if (mappingFile == null) {
				mapModel = getMappingGenerator().getMappingModel(getResourceBaseURI());
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

	public CompiledMapping getMapping() {
		if (mapping == null) {
			if (getMappingLanguage() == MappingLanguage.R2RML) {
				validateR2RML();
				R2RMLCompiler compiler = new R2RMLCompiler(
						getR2RMLMapping(), getSQLConnection());
				compiler.setFastMode(fastMode);
				mapping = compiler;
			} else {
				D2RQCompiler compiler = new D2RQCompiler(getD2RQMapping());
				if (sqlConnection != null || jdbcURL != null) {
					compiler.useConnection(getSQLConnection());
				}
				if (report != null) {
					compiler.setReport(report);
				}
				mapping = compiler.getResult();
			}
		}
		return mapping;
	}

	public void setReport(Report report) {
		this.report = report;
	}
	
	public R2RMLReader getR2RMLReader() {
		if (r2rmlReader == null) {
			r2rmlReader = new R2RMLReader(getMappingModel(), getResourceBaseURI());
			if (report != null) {
				r2rmlReader.setReport(report);
			}
		}
		return r2rmlReader;
	}
	
	public D2RQReader getD2RQReader() {
		if (d2rqReader == null) {
			d2rqReader = new D2RQReader(getMappingModel(), getResourceBaseURI());
		}
		return d2rqReader;
	}
	
	public org.d2rq.r2rml.Mapping getR2RMLMapping() {
		if (r2rmlMapping == null) {
			r2rmlMapping = getR2RMLReader().getMapping();
		}
		return r2rmlMapping;
	}
	
	public org.d2rq.lang.Mapping getD2RQMapping() {
		if (d2rqMapping == null) {
			d2rqMapping = getD2RQReader().getMapping();
			d2rqMapping.configuration().setUseAllOptimizations(fastMode);
			if (serveVocabulary != null) {
				d2rqMapping.configuration().setServeVocabulary(serveVocabulary);
			}
		}
		return d2rqMapping;
	}
	
	private void validateR2RML() {
		if (getR2RMLMapping() == null) return;
		SQLConnection connection = getJdbcURL() == null ? null : getSQLConnection();
		MappingValidator validator = new MappingValidator(
				getR2RMLMapping(), connection);
		if (report != null) {
			validator.setReport(report);
		}
		validator.run();
	}

	public void validate() {
		if (getMappingLanguage() == MappingLanguage.R2RML) {
			validateR2RML();
		} else {
			getMapping();
		}
	}
	
	public Model getModelD2RQ() {
		if (dataModel == null) {
			dataModel = ModelFactory.createModelForGraph(getGraphD2RQ());
		}
		return dataModel;
	}

	public Graph getGraphD2RQ() {
		if (dataGraph == null) {
			dataGraph = new GraphD2RQ(getMapping());
		}
		return dataGraph;
	}
	
	public MappingWriter getWriter() {
		if (writer == null) {
			if (getMappingLanguage() == MappingLanguage.R2RML) {
				writer = new R2RMLWriter(getR2RMLMapping());
			} else {
				writer = new D2RQWriter(getD2RQMapping());
			}
		}
		return writer;
	}
	
	public JettyLauncher getJettyLauncher() {
		if (jettyLauncher == null) {
			jettyLauncher = new JettyLauncher(this, getPort());
		}
		return jettyLauncher;
	}
	
	public ConfigLoader getServerConfig() {
		if (useServerConfig && serverConfig == null) {
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
	}

	/**
	 * Closes any created {@link SQLConnection}s and any other created resources.
	 */
	public void close() {
		if (sqlConnection != null) {
			sqlConnection.close();
		}
	}

	public MappingLanguage getMappingLanguage() {
		if (mappingLanguage == null) {
			boolean isD2RQ = new VocabularySummarizer(D2RQ.class).usesVocabulary(getMappingModel());
			boolean isR2RML = new VocabularySummarizer(RR.class).usesVocabulary(getMappingModel());
			if (isD2RQ && isR2RML) {
				throw new D2RQException("Mapping uses both D2RQ and R2RML terms");
			}
			boolean isR2RMLMapping = isR2RML || (!isR2RML && !isD2RQ);
			if (isR2RMLMapping) {
				mappingLanguage = MappingLanguage.R2RML;
				log.debug("Identified mapping model as an R2RML mapping");
			} else {
				mappingLanguage = MappingLanguage.D2RQ;
				log.debug("Identified mapping model as a D2RQ mapping");
			}
		}
		return mappingLanguage;
	}
}
