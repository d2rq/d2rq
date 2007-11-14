package de.fuberlin.wiwiss.d2rs;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

public class VelocityWrapper {
	private final static String VELOCITY_ENGINE_INSTANCE = 
			"de.fuberlin.wiwiss.d2rs.VelocityHelper.VELOCITY_ENGINE_INSTANCE";
	private final static String VELOCITY_DEFAULT_CONTEXT = 
			"de.fuberlin.wiwiss.d2rs.VelocityHelper.VELOCITY_DEFAULT_CONTEXT";
	
	public static synchronized void initEngine(D2RServer d2r, ServletContext servletContext) {
		try {
			VelocityEngine engine = new VelocityEngine(servletContext.getRealPath("/WEB-INF/velocity.properties"));
			engine.init();
			servletContext.setAttribute(VELOCITY_ENGINE_INSTANCE, engine);
			servletContext.setAttribute(VELOCITY_DEFAULT_CONTEXT, initDefaultContext(d2r));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private static Context initDefaultContext(D2RServer server) {
		Context context = new VelocityContext();
		context.put("truncated_results", new Boolean(server.hasTruncatedResults()));
		context.put("server_name", server.serverName());
		context.put("home_link", server.baseURI());
		return context;
	}
	
	private final VelocityEngine engine;
	private final Context context;
	private final HttpServletResponse response;
	
	public VelocityWrapper(HttpServlet servlet, HttpServletResponse response) {
		engine = (VelocityEngine) servlet.getServletContext().getAttribute(VELOCITY_ENGINE_INSTANCE);
		// TODO: Init context with default variables shared by all/many servlets
		Context defaultContext = (Context) servlet.getServletContext().getAttribute(VELOCITY_DEFAULT_CONTEXT);
		context = new VelocityContext(defaultContext);
		this.response = response;
	}
	
	public Context getContext() {
		return context;
	}

	public void mergeTemplateXHTML(String templateName) {
		response.addHeader("Content-Type", "application/xhtml+xml; charset=utf-8");
		response.addHeader("Cache-Control", "no-cache");
		response.addHeader("Pragma", "no-cache");
		try {
			engine.mergeTemplate(templateName, context, response.getWriter());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
