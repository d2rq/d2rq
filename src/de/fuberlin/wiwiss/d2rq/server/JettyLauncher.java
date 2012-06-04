package de.fuberlin.wiwiss.d2rq.server;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.webapp.WebAppContext;

import de.fuberlin.wiwiss.d2rq.SystemLoader;

/**
 * Starts a Jetty instance with D2R Server as the
 * root web application.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class JettyLauncher {
	private final static Log log = LogFactory.getLog(JettyLauncher.class);

	private final SystemLoader loader;
	private final int port;
	
	public JettyLauncher(SystemLoader loader, int port) {
		this.loader = loader;
		this.port = port;
	}

	/**
	 * Starts a Jetty server with D2R Server as root webapp.
	 * 
	 * @return <code>true</code> on success, <code>false</code> if webapp init failed 
	 */
	public boolean start() {
		Server jetty = new Server(port);

		// use Random (/dev/urandom) instead of SecureRandom to generate session keys - otherwise Jetty may hang during startup waiting for enough entropy
		// see http://jira.codehaus.org/browse/JETTY-331 and http://docs.codehaus.org/display/JETTY/Connectors+slow+to+startup
		jetty.setSessionIdManager(new HashSessionIdManager(new Random()));
		WebAppContext context = new WebAppContext(jetty, "webapp", "");
		// Place the system loader into the servlet context. The webapp init
		// listener will find it there and create the D2RServer instance.
		D2RServer.storeSystemLoader(loader, context.getServletContext());
		try {
			jetty.start();
			D2RServer server = D2RServer.fromServletContext(context.getServletContext());
			if (server == null || server.errorOnStartup()) {
				jetty.stop();
				log.warn("[[[ Server startup failed, see messages above ]]]");
				return false;
			}
			log.info("[[[ Server started at " + loader.getSystemBaseURI() + " ]]]");
			return true;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}