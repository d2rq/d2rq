package d2rq;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.shared.NoWriterForLangException;

import de.fuberlin.wiwiss.d2rq.CommandLineTool;
import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.SystemLoader;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.mapgen.MappingGenerator;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;

/**
 * Command line utility for dumping a database to RDF, using the
 * {@link MappingGenerator} or a mapping file.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class dump_rdf extends CommandLineTool {
	private final static Log log = LogFactory.getLog(dump_rdf.class);
	
	private final static int DUMP_DEFAULT_FETCH_SIZE = 500;

	public static void main(String[] args) {
		new dump_rdf().process(args);
	}
	
	public void usage() {
		System.err.println("usage:");
		System.err.println("  dump-rdf [output-options] mappingFile");
		System.err.println("  dump-rdf [output-options] [connection-options] jdbcURL");
		System.err.println("  dump-rdf [output-options] [connection-options] -l script.sql");
		System.err.println();
		printStandardArguments(true);
		System.err.println();
		System.err.println("  RDF output options:");
		System.err.println("    -b baseURI      Base URI for RDF output (default: " + SystemLoader.DEFAULT_BASE_URI + ")");
		System.err.println("    -f format       One of N-TRIPLE (default), RDF/XML, RDF/XML-ABBREV, TURTLE");
		System.err.println("    -o outfile      Output file name (default: stdout)");
		System.err.println("    --verbose       Print debug information");
		System.err.println();
		System.err.println("  Database connection options (only with jdbcURL):");
		printConnectionOptions();
		System.err.println();
		System.exit(1);
	}
	
	private ArgDecl baseArg = new ArgDecl(true, "b", "base");
	private ArgDecl formatArg = new ArgDecl(true, "f", "format");
	private ArgDecl outfileArg = new ArgDecl(true, "o", "out", "outfile");

	public void initArgs(CommandLine cmd) {
		cmd.add(baseArg);
		cmd.add(formatArg);
		cmd.add(outfileArg);
	}
	
	public void run(CommandLine cmd, SystemLoader loader) throws IOException {
		if (cmd.numItems() == 1) {
			loader.setMappingFileOrJdbcURL(cmd.getItem(0));
		}

		String format = "N-TRIPLE";
		if (cmd.hasArg(formatArg)) {
			format = cmd.getArg(formatArg).getValue();
		}
		PrintStream out;
		if (cmd.hasArg(outfileArg)) {
			File f = new File(cmd.getArg(outfileArg).getValue());
			log.info("Writing to " + f);
			out = new PrintStream(new FileOutputStream(f));
			loader.setSystemBaseURI(MapParser.absolutizeURI(f.toURI().toString() + "#"));
		} else {
			log.info("Writing to stdout");
			out = System.out;
		}
		if (cmd.hasArg(baseArg)) {
			loader.setSystemBaseURI(cmd.getArg(baseArg).getValue());
		}

		loader.setResultSizeLimit(Database.NO_LIMIT);
		Mapping mapping = loader.getMapping();
		try {
			// Trigger compilation
			mapping.compiledPropertyBridges();
			// Override the d2rq:resultSizeLimit given in the mapping
			for (Database db: mapping.databases()) {
				db.connectedDB().setDefaultFetchSize(DUMP_DEFAULT_FETCH_SIZE);
			}	

			Model d2rqModel = loader.getModelD2RQ();

			try {
				RDFWriter writer = d2rqModel.getWriter(format.toUpperCase());
				if (format.equals("RDF/XML") || format.equals("RDF/XML-ABBREV")) {
					writer.setProperty("showXmlDeclaration", "true");
					if (loader.getResourceBaseURI() != null) {
						writer.setProperty("xmlbase", loader.getResourceBaseURI());
					}
				}
				writer.write(d2rqModel, new OutputStreamWriter(out, "utf-8"), loader.getResourceBaseURI());
			} catch (NoWriterForLangException ex) {
				throw new D2RQException("Unknown format '" + format + "'", D2RQException.STARTUP_UNKNOWN_FORMAT);
			} catch (UnsupportedEncodingException ex) {
				throw new RuntimeException("Can't happen -- utf-8 is always supported");
			}
		} finally {
			out.close();
			mapping.close();
		}
	}
}
