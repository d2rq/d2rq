package de.fuberlin.wiwiss.d2rs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import de.fuberlin.wiwiss.d2rq.D2RQException;

/**
 * Initialize D2R server on startup of an appserver such as Tomcat. This listener should
 * be included in the web.xml. This is compatible with Servlet 2.3 spec compliant appservers.
 *
 * @version $Id: WebappInitListener.java,v 1.1 2007/11/02 14:46:25 cyganiak Exp $
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
		server.setConfigFile(configFile);
		server.start();
		server.putIntoServletContext(context);
	}

	public void contextDestroyed(ServletContextEvent event) {
		// Do nothing
	}
	
	private String absolutize(String fileName, ServletContext context) {
		try {
			URI uri = new URI(fileName);
			if (uri.isAbsolute()) {
				return fileName;
			}
			return new File(context.getRealPath("WEB-INF/" + fileName)).getAbsoluteFile().toURL().toExternalForm();
		} catch (URISyntaxException ex) {
			throw new D2RQException(ex);
		} catch (MalformedURLException ex) {
			throw new D2RQException(ex);
		}
	}
}
