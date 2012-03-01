package de.fuberlin.wiwiss.d2rq.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Starts a Jetty instance with D2R Server as the
 * root web application. Configuration values are
 * funnelled from the command line to the web app
 * by pre-populating the servlet context's init 
 * parameters with values taken from the command line.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class JettyLauncher {
	public final static int DEFAULT_PORT = 2020;

	private String configFile;
	private int cmdLinePort = -1;
	private int configFilePort = -1;
	private String baseURI = null;
	private String homeURI;
	private boolean useAllOptimizations = false;
	
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
	
	public void overrideUseAllOptimizations(boolean useAllOptimizations) {
		this.useAllOptimizations = useAllOptimizations;
	}

	/**
	 * Starts a Jetty server with D2R Server as root webapp.
	 * 
	 * @return <code>true</code> on success, <code>false</code> if webapp init failed 
	 */
	public boolean start() {
		Server jetty = new Server(getPort());
		
		// use Random (/dev/urandom) instead of SecureRandom to generate session keys - otherwise Jetty may hang during startup waiting for enough entropy
		// see http://jira.codehaus.org/browse/JETTY-331 and http://docs.codehaus.org/display/JETTY/Connectors+slow+to+startup
		jetty.setSessionIdManager(new HashSessionIdManager(new Random()));
		WebAppContext context = new WebAppContext(jetty, "webapp", "");
		for (Entry<String,String> entry : getInitParams().entrySet()) {
			context.setInitParameter(entry.getKey(), entry.getValue());
		}
		try {
			jetty.start();
			D2RServer server = D2RServer.fromServletContext(context.getServletContext());
			if (server == null) {
				jetty.stop();
				return false;
			}
			homeURI = server.baseURI();
			return true;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
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
	
	private Map<String,String> getInitParams() {
		Map<String,String> result = new HashMap<String,String>();
		if (cmdLinePort != -1) {
			result.put("port", Integer.toString(cmdLinePort));
		}
		if (baseURI != null) {
			result.put("baseURI", baseURI);
		}
		if (useAllOptimizations) {
			result.put("useAllOptimizations", "true");
		}
		result.put("overrideConfigFile", configFile);
		return result;
	}
}