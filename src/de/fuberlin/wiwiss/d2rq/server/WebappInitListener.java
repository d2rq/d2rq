package de.fuberlin.wiwiss.d2rq.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Initialize D2R server on startup of an appserver such as Tomcat. This listener should
 * be included in the web.xml. This is compatible with Servlet 2.3 spec compliant appservers.
 *
 * @author Inigo Surguy
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class WebappInitListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent event) {
		ServletContext context = event.getServletContext();
		D2RServer server = new D2RServer();
		String configFile = context.getInitParameter("overrideConfigFile");
		if (configFile == null) {
			if (context.getInitParameter("configFile") == null) {
				throw new RuntimeException("No configFile configured in web.xml");
			}
			configFile = absolutize(context.getInitParameter("configFile"), context);
		}
		if (context.getInitParameter("port") != null) {
			server.overridePort(Integer.parseInt(context.getInitParameter("port")));
		}
		if (context.getInitParameter("baseURI") != null) {
			server.overrideBaseURI(context.getInitParameter("baseURI"));
		}
		if (context.getInitParameter("useAllOptimizations") != null) {
			server.overrideUseAllOptimizations(context.getInitParameter("useAllOptimizations").equalsIgnoreCase("true"));
		}
		server.setConfigFile(configFile);
		server.start();
		server.putIntoServletContext(context);
		VelocityWrapper.initEngine(server, context);
	}

	public void contextDestroyed(ServletContextEvent event) {
		// Do nothing
	}
	
	private String absolutize(String fileName, ServletContext context) {
		if (!fileName.matches("[a-zA-Z0-9]+:.*")) {
			fileName = context.getRealPath("WEB-INF/" + fileName);
		}
		return ConfigLoader.toAbsoluteURI(fileName);
	}
}
