package d2rq;

import java.io.IOException;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryCancelledException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import com.hp.hpl.jena.sparql.util.QueryExecUtils;

import de.fuberlin.wiwiss.d2rq.CommandLineTool;
import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.SystemLoader;
import de.fuberlin.wiwiss.d2rq.engine.QueryEngineD2RQ;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;

/**
 * Command line utility for executing SPARQL queries
 * against a D2RQ-mapped database
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class d2r_query extends CommandLineTool {
	private static final Log log = LogFactory.getLog(d2r_query.class);
	
	public static void main(String[] args) {
		new d2r_query().process(args);
	}
	
	public void usage() {
		System.err.println("usage:");
		System.err.println("  d2r-query [query-options] mappingFile query");
		System.err.println("  d2r-query [query-options] [connection-options] jdbcURL query");
		System.err.println("  d2r-query [query-options] [connection-options] -l script.sql query");
		System.err.println();
		printStandardArguments(true);
		System.err.println("    query           A SPARQL query, e.g., \"SELECT * {?s rdf:type ?o} LIMIT 10\"");
		System.err.println("                    A value of @file.sparql reads the query from a file.");
		System.err.println("  Query options:");
		System.err.println("    -b baseURI      Base URI for RDF output (default: " + SystemLoader.DEFAULT_BASE_URI + ")");
		System.err.println("    -f format       One of text (default), xml, json, csv, tsv, srb, ttl");
		System.err.println("    -t timeout      Query timeout in seconds");
		System.err.println("    --verbose       Print debug information");
		System.err.println();
		System.err.println("  Database connection options (only with jdbcURL):");
		printConnectionOptions();
		System.err.println();
		System.exit(1);
	}
	
	private ArgDecl baseArg = new ArgDecl(true, "b", "base");
	private ArgDecl formatArg = new ArgDecl(true, "f", "format");
	private ArgDecl timeoutArg = new ArgDecl(true, "t", "timeout");

	public void initArgs(CommandLine cmd) {
		cmd.add(baseArg);
		cmd.add(formatArg);
		cmd.add(timeoutArg);
		setMinMaxArguments(1, 2);
		setSupportImplicitJdbcURL(true);
	}
	
	public void run(CommandLine cmd, SystemLoader loader) throws IOException {
		String query = null;
		if (cmd.numItems() == 1) {
			query = cmd.getItem(0, true);
		} else if (cmd.numItems() == 2) {
			loader.setMappingFileOrJdbcURL(cmd.getItem(0));
			query = cmd.getItem(1, true);
		}

		String format = null;
		if (cmd.hasArg(formatArg)) {
			format = cmd.getArg(formatArg).getValue();
		}
		if (cmd.hasArg(baseArg)) {
			loader.setSystemBaseURI(cmd.getArg(baseArg).getValue());
		}
		double timeout = -1;
		if (cmd.hasArg(timeoutArg)) {
			try {
				timeout = Double.parseDouble(cmd.getArg(timeoutArg).getValue());
			} catch (NumberFormatException ex) {
				throw new D2RQException("Timeout must be a number in seconds: '" 
						+ cmd.getArg(timeoutArg).getValue() + "'", D2RQException.MUST_BE_NUMERIC);
			}
		}
		
		loader.setFastMode(true);
		ModelD2RQ d2rqModel = loader.getModelD2RQ();

		String prefixes = "";
		for (String prefix: d2rqModel.getNsPrefixMap().keySet()) {
			prefixes += "PREFIX " + prefix + ": <" + d2rqModel.getNsPrefixURI(prefix) + ">\n";
		}
		query = prefixes + query;
		log.info("Query:\n" + query);
		
		try {
			QueryEngineD2RQ.register();
			Query q = QueryFactory.create(query, loader.getResourceBaseURI());
			QueryExecution qe = QueryExecutionFactory.create(q, d2rqModel);
			if (timeout > 0) {
				qe.setTimeout(Math.round(timeout * 1000));
			}
			QueryExecUtils.executeQuery(q, qe, ResultsFormat.lookup(format));
		} catch(QueryCancelledException ex) {
			throw new D2RQException("Query timeout", ex, D2RQException.QUERY_TIMEOUT);
		} finally {
			d2rqModel.close();
		}
	}
}
