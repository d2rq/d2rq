package d2r;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.shared.JenaException;

import de.fuberlin.wiwiss.d2rs.ConfigLoader;
import de.fuberlin.wiwiss.d2rs.JettyLauncher;

/**
 * Command line launcher for D2R Server.
 * 
 * @version $Id: server.java,v 1.8 2009/08/02 09:12:05 fatorange Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class server {
	private final static String usage = "usage: d2r-server [-p port] [-b serverBaseURI] [--fast] mappingFileName";
	private static JettyLauncher server;
	private final static Log log = LogFactory.getLog(server.class);
	
	public static void main(String[] args) {
		CommandLine cmd = new CommandLine();
		cmd.setUsage(usage);
		ArgDecl portArg = new ArgDecl(true, "p", "port");
		cmd.add(portArg);
		ArgDecl baseURIArg = new ArgDecl(true, "b", "base");
		cmd.add(baseURIArg);
		ArgDecl fastArg = new ArgDecl(false, "fast");
		cmd.add(fastArg);
		cmd.process(args);
		
		if (cmd.numItems() == 0) {
			System.err.println(usage);
			System.exit(1);
		}
		if (cmd.numItems() > 2) {
			System.err.println("too many arguments");
			System.err.println(usage);
			System.exit(1);
		}
		server = new JettyLauncher();
		if (cmd.contains(portArg)) {
			setPort(Integer.parseInt(cmd.getArg(portArg).getValue()));
		}
		if (cmd.contains(baseURIArg)) {
			setServerBaseURI(cmd.getArg(baseURIArg).getValue());
		}
		if (cmd.contains(fastArg)) {
			setUseAllOptimizations(true);
		}
		String mappingFileName = cmd.getItem(0);
		setMappingFileName(mappingFileName);
		startServer();
	}
	
	public static void setPort(int port) {
		server.overridePort(port);
	}

	public static void setServerBaseURI(String baseURI) {
		server.overrideBaseURI(baseURI);
	}
	
	public static void setUseAllOptimizations(boolean useAllOptimizations) {
		server.overrideUseAllOptimizations(useAllOptimizations);
	}
	
	public static void setMappingFileName(String mappingFileName) {
		try {
			server.setConfigFile(ConfigLoader.toAbsoluteURI(mappingFileName));
		} catch (JenaException ex) {
			Throwable t = ex;
			if (ex.getCause() != null) {
				t = ex.getCause();
			}
			System.err.println(mappingFileName + ": " + t.getMessage());
			System.exit(1);
		}
	}
	
	public static void startServer() {
		server.start();
		log.info("[[[ Server started at " + server.getHomeURI() + " ]]]");
	}
}
