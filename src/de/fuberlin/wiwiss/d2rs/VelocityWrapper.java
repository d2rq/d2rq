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

	public static synchronized void initEngine(ServletContext servletContext) {
		try {
			VelocityEngine engine = new VelocityEngine(servletContext.getRealPath("/WEB-INF/velocity.properties"));
			engine.init();
			servletContext.setAttribute(VELOCITY_ENGINE_INSTANCE, engine);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private final VelocityEngine engine;
	private final Context context;
	private final HttpServletResponse response;
	
	public VelocityWrapper(HttpServlet servlet, HttpServletResponse response) {
		engine = (VelocityEngine) servlet.getServletContext().getAttribute(VELOCITY_ENGINE_INSTANCE);
		// TODO: Init context with default variables shared by all/many servlets
		context = new VelocityContext();
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
