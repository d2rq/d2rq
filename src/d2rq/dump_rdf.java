package d2rq;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;

import com.hp.hpl.jena.n3.turtle.TurtleParseException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.mapgen.MappingGenerator;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.SQLScriptLoader;

/**
 * Command line utility for dumping a database to RDF, using the
 * {@link MappingGenerator} or a mapping file.
 * 
 * TODO: Factor out the parts that are shared with {@link generate_mapping}
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class dump_rdf {
	private final static String[] includedDrivers = {
			"com.mysql.jdbc.Driver"
	};
	
	private final static int DEFAULT_DUMP_FETCH_SIZE = 500;
	
	public static void main(String[] args) {
		for (int i = 0; i < includedDrivers.length; i++) {
			ConnectedDB.registerJDBCDriverIfPresent(includedDrivers[i]);
		}
		CommandLine cmd = new CommandLine();
		ArgDecl userArg = new ArgDecl(true, "u", "user", "username");
		ArgDecl passArg = new ArgDecl(true, "p", "pass", "password");
		ArgDecl driverArg = new ArgDecl(true, "d", "driver");
		ArgDecl jdbcArg = new ArgDecl(true, "j", "jdbc");
		ArgDecl fetchsizeArg = new ArgDecl(true, "s", "fetchsize");
		ArgDecl mapArg = new ArgDecl(true, "m", "map", "mapping");
		ArgDecl baseArg = new ArgDecl(true, "b", "base");
		ArgDecl formatArg = new ArgDecl(true, "f", "format");
		ArgDecl outfileArg = new ArgDecl(true, "o", "out", "outfile");
		ArgDecl loadSQLArg = new ArgDecl(true, "l", "load-sql");
		cmd.add(userArg);
		cmd.add(passArg);
		cmd.add(driverArg);
		cmd.add(jdbcArg);
		cmd.add(fetchsizeArg);
		cmd.add(mapArg);
		cmd.add(baseArg);
		cmd.add(formatArg);
		cmd.add(outfileArg);
		cmd.add(loadSQLArg);
		cmd.process(args);

		RDFDump dump = new RDFDump();
		if (cmd.contains(userArg)) {
			dump.setUser(cmd.getArg(userArg).getValue());
		}
		if (cmd.contains(passArg)) {
			dump.setPassword(cmd.getArg(passArg).getValue());
		}
		if (cmd.contains(driverArg)) {
			dump.setDriverClass(cmd.getArg(driverArg).getValue());
		}
		if (cmd.contains(jdbcArg)) {
			dump.setJDBCURL(cmd.getArg(jdbcArg).getValue());
		}
		if (cmd.contains(fetchsizeArg)) {
			dump.setFetchSize(Integer.valueOf(cmd.getArg(fetchsizeArg).getValue()));
		}
		if (cmd.contains(mapArg)) {
			dump.setMapURL(cmd.getArg(mapArg).getValue());
		}
		if (cmd.contains(baseArg)) {
			dump.setBaseURI(cmd.getArg(baseArg).getValue());
		}
		if (cmd.contains(formatArg)) {
			dump.setFormat(cmd.getArg(formatArg).getValue());
		}
		if (cmd.contains(outfileArg)) {
			dump.setOutputFile(cmd.getArg(outfileArg).getValue());
		}
		if (cmd.contains(loadSQLArg)) {
			dump.setSQLScript(cmd.getArg(loadSQLArg).getValue());
		}
		if (cmd.numItems() > 0 || !dump.hasParameter()) {
			usage();
			return;
		}
		try {
			dump.doDump();
		} catch (DumpParameterException ex) {
			System.err.println(ex.getMessage());
			System.exit(1);
		} catch (D2RQException ex) {
			if (ex.getMessage() == null && ex.getCause() != null) {
				System.err.println(ex.getCause().getMessage());
			} else {
				System.err.println(ex.getMessage());
			}
			System.exit(1);
		} catch (NotFoundException ex) {
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}
	
	private static void usage() {
		System.out.println("usage: dump-rdf [parameters]");
		System.out.println();
		System.out.println("  Database connection parameters (ignored if specified in mapping file)");
		System.out.println("    -j jdbcURL      JDBC URL for the DB, e.g. jdbc:mysql://localhost/dbname");
		System.out.println("    -d driverclass  Java class name of the JDBC driver for the DB");
		System.out.println("    -u username     Database user for connecting to the DB");
		System.out.println("    -p password     Database password for connecting to the DB");
		System.out.println("    -l script.sql   Load a SQL script before startup");
		System.out.println("    -s fetchSize    JDBC fetch size. Defaults to " + DEFAULT_DUMP_FETCH_SIZE + " or for MySQL, to Integer.MIN_VALUE for streaming mode");
		System.out.println();
		System.out.println("  RDF output parameters");
		System.out.println("    -m mapURL       URL of a D2RQ mapping file (optional)");
		System.out.println("    -b baseURI      Base URI for generated RDF (optional)");
		System.out.println("    -f format       One of N-TRIPLE (default), RDF/XML, RDF/XML-ABBREV, TURTLE");
		System.out.println("    -o outfile      Output file name (default: stdout)");
		System.out.println();
	}
	
	static class RDFDump {
		private String user = null;
		private String password = null;
		private String driverClass = null;
		private String jdbcURL = null;
		private String mapURL = null;
		private String baseURI = null;
		private String format = "N-TRIPLE";
		private String outputFile = null;
		private String sqlScript = null;
		private Integer fetchSize = null;
		
		private boolean hasParameter = false;
		void setUser(String user) {
			this.user = user;
			this.hasParameter = true;
		}
		void setPassword(String password) {
			this.password = password;
			this.hasParameter = true;
		}
		void setDriverClass(String driverClass) {
			this.driverClass = driverClass;
			this.hasParameter = true;
		}
		void setJDBCURL(String jdbcURL) {
			this.jdbcURL = jdbcURL;
			this.hasParameter = true;
		}
		void setFetchSize(Integer fetchSize) {
			this.fetchSize = fetchSize;
			this.hasParameter = true;
		}
		void setMapURL(String mapURL) {
			this.mapURL = mapURL;
			this.hasParameter = true;
		}
		void setBaseURI(String baseURI) {
			this.baseURI = baseURI;
			this.hasParameter = true;
		}
		void setFormat(String format) {
			this.format = format;
			this.hasParameter = true;
		}
		void setOutputFile(String outputFile) {
			this.outputFile = outputFile;
			this.hasParameter = true;
		}
		void setSQLScript(String sqlScript) {
			this.sqlScript = sqlScript;
			hasParameter = true;
		}
		boolean hasParameter() {
			return this.hasParameter;
		}
		void doDump() throws DumpParameterException {
			Model mapModel = makeMapModel();
			
			// Override the d2rq:resultSizeLimit given in the mapping and set fetchSize
			Mapping mapping = new MapParser(mapModel, baseURI()).parse();
			for (Database db : mapping.databases()) {
				db.setResultSizeLimit(Database.NO_LIMIT);
				if (this.fetchSize != null)
					db.setFetchSize(this.fetchSize.intValue());
				else {
					/* Supply useful fetch sizes if none set so far */
					if (db.getFetchSize() == Database.NO_FETCH_SIZE)
						db.setFetchSize(db.getJDBCDSN() != null && db.getJDBCDSN().contains(":mysql:") ? Integer.MIN_VALUE : DEFAULT_DUMP_FETCH_SIZE);
				}
				if (!hasMappingFile() && sqlScript != null) {
					db.setStartupSQLScript(ResourceFactory.createResource(
							new File(sqlScript).toURI().toString()));
				}
			}		
			
			Model d2rqModel = new ModelD2RQ(mapping);
			String absoluteBaseURI = MapParser.absolutizeURI(baseURI());
			PrintStream out = makeDestinationStream();
			RDFWriter writer = d2rqModel.getWriter(this.format);
			if (this.format.equals("RDF/XML") || this.format.equals("RDF/XML-ABBREV")) {
				writer.setProperty("showXmlDeclaration", "true");
				if (this.baseURI != null) {
					writer.setProperty("xmlbase", this.baseURI);
				}
				try {
					writer.write(d2rqModel, new OutputStreamWriter(out, "utf-8"), absoluteBaseURI);
				} catch (UnsupportedEncodingException ex) {
					throw new RuntimeException("Can't happen -- utf-8 is always supported");
				}
			} else {
				writer.write(d2rqModel, out, absoluteBaseURI);
			}
			d2rqModel.close();
		}
		private Model makeMapModel() throws DumpParameterException {
			if (hasMappingFile()) {
				try {
					// if file extension is not among the known types then assume that the file has TURTLE syntax
					if(FileUtils.guessLang(this.mapURL,"unknown").equals("unknown"))
						return FileManager.get().loadModel(this.mapURL, baseURI(), "TURTLE");
					else
						return FileManager.get().loadModel(this.mapURL, baseURI(), null);
				} catch (TurtleParseException ex) {
					throw new D2RQException(
							"Error parsing " + mapURL + ": " + ex.getMessage(), ex, 77);
				}
			}
			if (this.jdbcURL == null) {
				throw new DumpParameterException("Must specify either -j or -m parameter");
			}
			ConnectedDB.registerJDBCDriver(this.driverClass);
			ConnectedDB db = new ConnectedDB(this.jdbcURL, this.user, this.password);			
			try {
				if (sqlScript != null) {
					try {
						SQLScriptLoader.loadFile(new File(sqlScript), db.connection());
					} catch (IOException ex) {
						throw new D2RQException("Error accessing SQL startup script: " + sqlScript);
					} catch (SQLException ex) {
						throw new D2RQException("Error importing " + sqlScript + " " + ex.getMessage());
					}
				}
				MappingGenerator gen = new MappingGenerator(db);
				if (this.driverClass != null) {
					gen.setJDBCDriverClass(this.driverClass);
				}
				gen.setMapNamespaceURI("file:tmp#");
				gen.setInstanceNamespaceURI("");
				gen.setVocabNamespaceURI("http://localhost/vocab/");
				return gen.mappingModel(baseURI(), System.err);
			} finally {
				db.close();
			}
		}
		private boolean hasMappingFile() {
			return this.mapURL != null;
		}
		private String baseURI() {
			if (this.baseURI != null) {
				return this.baseURI;
			}
			if (this.outputFile != null) {
				return "file:" + this.outputFile + "#";
			}
			return "http://localhost/";
		}
		private PrintStream makeDestinationStream() {
			if (this.outputFile == null) {
				return System.out;
			}
			File f = new File(this.outputFile);
			try {
				return new PrintStream(new FileOutputStream(f));
			} catch (FileNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	static class DumpParameterException extends Exception {
		DumpParameterException(String message) {
			super(message);
		}
	}
}
