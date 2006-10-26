package d2rq;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.mapgen.MappingGenerator;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;

/**
 * Command line utility for dumping a database to RDF, using the
 * {@link MappingGenerator} or a mapping file.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: dump_rdf.java,v 1.5 2006/10/26 13:50:42 cyganiak Exp $
 */
public class dump_rdf {
	private final static String[] includedDrivers = {
			"com.mysql.jdbc.Driver"
	};
	
	public static void main(String[] args) {
		for (int i = 0; i < includedDrivers.length; i++) {
			Database.registerJDBCDriverIfPresent(includedDrivers[i]);
		}
		CommandLine cmd = new CommandLine();
		ArgDecl userArg = new ArgDecl(true, "u", "user", "username");
		ArgDecl passArg = new ArgDecl(true, "p", "pass", "password");
		ArgDecl driverArg = new ArgDecl(true, "d", "driver");
		ArgDecl jdbcArg = new ArgDecl(true, "j", "jdbc");
		ArgDecl mapArg = new ArgDecl(true, "m", "map", "mapping");
		ArgDecl baseArg = new ArgDecl(true, "b", "base");
		ArgDecl formatArg = new ArgDecl(true, "f", "format");
		ArgDecl outfileArg = new ArgDecl(true, "o", "out", "outfile");
		cmd.add(userArg);
		cmd.add(passArg);
		cmd.add(driverArg);
		cmd.add(jdbcArg);
		cmd.add(mapArg);
		cmd.add(baseArg);
		cmd.add(formatArg);
		cmd.add(outfileArg);
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
			if (ex.getCause() != null) {
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
		System.out.println("  Database connection parameters (may be omitted if specified in mapping file)");
		System.out.println("    -u username     Database user for connecting to the DB");
		System.out.println("    -p password     Database password for connecting to the DB");
		System.out.println("    -d driverclass  Java class name of the JDBC driver for the DB");
		System.out.println("    -j jdbcURL      JDBC URL for the DB, e.g. jdbc:mysql://localhost/dbname");
		System.out.println();
		System.out.println("  RDF output parameters");
		System.out.println("    -m mapURL       URL of a D2RQ mapping file (optional)");
		System.out.println("    -b baseURI      Base URI for generated RDF (optional)");
		System.out.println("    -f format       One of RDF/XML (default), RDF/XML-ABBREV, N3, N-TRIPLE");
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
		private String format = "RDF/XML";
		private String outputFile = null;
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
		boolean hasParameter() {
			return this.hasParameter;
		}
		void doDump() throws DumpParameterException {
			Model mapModel = makeMapModel();
			Model d2rqModel = new ModelD2RQ(mapModel, baseURI());
			PrintStream out = makeDestinationStream();
			RDFWriter writer = d2rqModel.getWriter(this.format);
			if (this.baseURI != null && 
					(this.format.equals("RDF/XML") || this.format.equals("RDF/XML-ABBREV"))) {
				writer.setProperty("xmlbase", this.baseURI);
			}
			writer.write(d2rqModel, out, MapParser.absolutizeURI(baseURI()));
		}
		private Model makeMapModel() throws DumpParameterException {
			if (hasMappingFile()) {
				return FileManager.get().loadModel(this.mapURL, baseURI(), null);
			}
			if (this.jdbcURL == null) {
				throw new DumpParameterException("Must specify either -j or -m parameter");
			}
			MappingGenerator gen = new MappingGenerator(this.jdbcURL);
			if (this.user != null) {
				gen.setDatabaseUser(this.user);
			}
			if (this.password != null) {
				gen.setDatabasePassword(this.password);
			}
			if (this.driverClass != null) {
				gen.setJDBCDriverClass(this.driverClass);
			}
			gen.setMapNamespaceURI("file:tmp#");
			gen.setInstanceNamespaceURI("");
			gen.setVocabNamespaceURI("http://localhost/vocab/");
			return gen.mappingModel(baseURI());
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
