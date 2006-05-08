package d2r;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;
import de.fuberlin.wiwiss.d2rs.D2RServer;

/**
 * Command line launcher for D2R Server.
 * 
 * @version $Id: server.java,v 1.1 2006/05/08 16:42:48 cyganiak Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class server {
	private final static String usage = "usage: d2r-server [-p port] mappingFileName";
	private static D2RServer server;
	
	public static void main(String[] args) {
		CommandLine cmd = new CommandLine();
		cmd.setUsage(usage);
		ArgDecl portArg = new ArgDecl(true, "p", "port");
		cmd.add(portArg);
		cmd.process(args);
		
		if (cmd.numItems() == 0) {
			System.err.println(usage);
			System.exit(1);
		}
		if (cmd.numItems() > 1) {
			System.err.println("too many arguments");
			System.err.println(usage);
			System.exit(1);
		}
		server = new D2RServer();
		if (cmd.contains(portArg)) {
			setPort(Integer.parseInt(cmd.getArg(portArg).getValue()));
		}
		String mappingFileName = cmd.getItem(0);
		setMappingFileURL(mappingFileName);
		startServer();
	}
	
	public static void setPort(int port) {
		server.setPort(port);
	}
	
	public static void setMappingFileURL(String mappingFileURL) {
		server.initFromMappingFile(mappingFileURL);
	}
	
	public static void startServer() {
		server.start();
	}
}
