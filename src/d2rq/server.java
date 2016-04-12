package d2rq;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;
import de.fuberlin.wiwiss.d2rq.CommandLineTool;
import de.fuberlin.wiwiss.d2rq.SystemLoader;
import de.fuberlin.wiwiss.d2rq.server.JettyLauncher;

/**
 * Command line launcher for D2R Server.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class server extends CommandLineTool {

	public static void main(String[] args) {
		new server().process(args);
	}

	public void usage() {
		System.err.println("usage:");
		System.err.println("  d2r-server [server-options] mappingFile");
		System.err.println("  d2r-server [server-options] [connection-options] jdbcURL");
		System.err.println("  d2r-server [server-options] [connection-options] -l script.sql");
		System.err.println();
		printStandardArguments(true);
		System.err.println();
		System.err.println("  Server options:");
		System.err.println("    --port number   Port where to start up the server (default: 2020)");
		System.err.println("    -b baseURI      Server's base URI (default: " + SystemLoader.DEFAULT_BASE_URI + ")");
		System.err.println("    --fast          Use all engine optimizations (recommended)");
		System.err.println("    --verbose       Print debug information");
		System.err.println();
		System.err.println("  Database connection options (only with jdbcURL):");
		printConnectionOptions();
		System.err.println();
	}
	
	private ArgDecl portArg = new ArgDecl(true, "port");
	private ArgDecl baseArg = new ArgDecl(true, "b", "base");
	private ArgDecl fastArg = new ArgDecl(false, "fast");

	public void initArgs(CommandLine cmd) {
		cmd.add(portArg);
		cmd.add(baseArg);
		cmd.add(fastArg);
	}
	
	public void run(CommandLine cmd, SystemLoader loader) {
		if (cmd.numItems() == 1) {
			loader.setMappingFileOrJdbcURL(cmd.getItem(0));
		}

		loader.setResourceStem("resource/");
		if (cmd.contains(fastArg)) {
			loader.setFastMode(true);
		}
		if (cmd.contains(portArg)) {
			loader.setPort(Integer.parseInt(cmd.getArg(portArg).getValue()));
		}
		if (cmd.contains(baseArg)) {
			loader.setSystemBaseURI(cmd.getArg(baseArg).getValue());
		}

		loader.getModelD2RQ();
		JettyLauncher launcher = loader.getJettyLauncher();
		launcher.start();
	}
}
