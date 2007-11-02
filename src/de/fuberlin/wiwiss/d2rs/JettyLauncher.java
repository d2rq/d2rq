package de.fuberlin.wiwiss.d2rs;

import java.util.HashMap;
import java.util.Map;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Starts a Jetty instance with D2R Server as the
 * root web application. Configuration values are
 * funnelled from the command line to the web app
 * by pre-populating the servlet context's init 
 * parameters with values taken from the command line.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: JettyLauncher.java,v 1.1 2007/11/02 14:46:25 cyganiak Exp $
 */
public class JettyLauncher {
	public final static int DEFAULT_PORT = 2020;

	private String configFile;
	private int cmdLinePort = -1;
	private int configFilePort = -1;
	private String baseURI = null;
	private String homeURI;
	
	public void setConfigFile(String configFile) {
		this.configFile = configFile;
		// We must parse the config file here to check if there's a port
		// specified inside, because we need to start up the Jetty on that
		// port.
		ConfigLoader config = new ConfigLoader(configFile);
		config.load();
		if (config.port() != -1) {
			configFilePort = config.port();
		}
	}
	
	public void overridePort(int port) {
		cmdLinePort = port;
	}

	public void overrideBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}
	
	public void start() {
		Server jetty = new Server(getPort());
		WebAppContext context = new WebAppContext(jetty, "webapp", "");
		context.setInitParams(getInitParams());
		try {
			jetty.start();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		homeURI = D2RServer.fromServletContext(context.getServletContext()).baseURI();
	}

	public String getHomeURI() {
		return homeURI;
	}

	public int getPort() {
		if (cmdLinePort != -1) {
			return cmdLinePort;
		}
		if (configFilePort != -1) {
			return configFilePort;
		}
		return DEFAULT_PORT;
	}
	
	private Map getInitParams() {
		Map result = new HashMap();
		if (cmdLinePort != -1) {
			result.put("port", Integer.toString(cmdLinePort));
		}
		if (baseURI != null) {
			result.put("baseURI", baseURI);
		}
		result.put("overrideConfigFile", configFile);
		return result;
	}
}