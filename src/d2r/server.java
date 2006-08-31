package d2r;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;

import com.hp.hpl.jena.n3.N3Exception;
import com.hp.hpl.jena.shared.NotFoundException;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rs.D2RServer;

/**
 * Command line launcher for D2R Server.
 * 
 * @version $Id: server.java,v 1.3 2006/08/31 14:53:22 cyganiak Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class server {
	private final static String usage = "usage: d2r-server [-p port] [-b serverBaseURI] mappingFileName";
	private static D2RServer server;
	
	public static void main(String[] args) {
		CommandLine cmd = new CommandLine();
		cmd.setUsage(usage);
		ArgDecl portArg = new ArgDecl(true, "p", "port");
		cmd.add(portArg);
		ArgDecl baseURIArg = new ArgDecl(true, "b", "base");
		cmd.add(baseURIArg);
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
		server = D2RServer.instance();
		if (cmd.contains(portArg)) {
			setPort(Integer.parseInt(cmd.getArg(portArg).getValue()));
		}
		if (cmd.contains(baseURIArg)) {
			setServerBaseURI(cmd.getArg(baseURIArg).getValue());
		}
		String mappingFileName = cmd.getItem(0);
		// Windows? Convert \ to / in mapping file name
		// because we treat it as a URL, not a file name
		if (System.getProperty("os.name").toLowerCase().indexOf("win") != -1) {
			mappingFileName = mappingFileName.replaceAll("\\\\", "/");
		}
		setMappingFileURL(mappingFileName);
		startServer();
	}
	
	public static void setPort(int port) {
		server.setPort(port);
	}

	public static void setServerBaseURI(String baseURI) {
		server.setBaseURI(baseURI);
	}
	
	public static void setMappingFileURL(String mappingFileURL) {
		try {
			server.initFromMappingFile(mappingFileURL);
		} catch (NotFoundException ex) {
			System.err.println(ex.getMessage());
			System.exit(1);
		} catch (N3Exception ex) {
			System.err.println(mappingFileURL + ": " + ex.getMessage());
			System.exit(1);
		} catch (D2RQException ex) {
			System.err.println(mappingFileURL + ": " + ex.getMessage());
			System.exit(1);
		}
	}
	
	public static void startServer() {
		server.start();
	}
}
