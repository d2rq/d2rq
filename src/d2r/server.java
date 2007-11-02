package d2r;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.shared.JenaException;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rs.JettyLauncher;

/**
 * Command line launcher for D2R Server.
 * 
 * @version $Id: server.java,v 1.6 2007/11/02 14:46:24 cyganiak Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class server {
	private final static String usage = "usage: d2r-server [-p port] [-b serverBaseURI] mappingFileName";
	private static JettyLauncher server;
	private final static Log log = LogFactory.getLog(server.class);
	
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
		server = new JettyLauncher();
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
		setMappingFileURL(absolutize(mappingFileName));
		startServer();
	}
	
	public static void setPort(int port) {
		server.overridePort(port);
	}

	public static void setServerBaseURI(String baseURI) {
		server.overrideBaseURI(baseURI);
	}
	
	public static void setMappingFileURL(String mappingFileURL) {
		try {
			server.setConfigFile(mappingFileURL);
		} catch (JenaException ex) {
			Throwable t = ex;
			if (ex.getCause() != null) {
				t = ex.getCause();
			}
			System.err.println(mappingFileURL + ": " + t.getMessage());
			System.exit(1);
		}
	}
	
	public static void startServer() {
		server.start();
		log.info("[[[ Server started at " + server.getHomeURI() + " ]]]");
	}
	
	private static String absolutize(String relativeURI) {
		try {
			if (new URI(relativeURI).isAbsolute()) {
				return relativeURI;
			}
			return new File(relativeURI).getAbsoluteFile().toURL().toExternalForm();
		} catch (URISyntaxException ex) {
			throw new D2RQException(ex);
		} catch (MalformedURLException ex) {
			throw new D2RQException(ex);
		}
	}
}
