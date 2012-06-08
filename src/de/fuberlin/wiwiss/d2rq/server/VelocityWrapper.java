package de.fuberlin.wiwiss.d2rq.server;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import de.fuberlin.wiwiss.pubby.negotiation.ContentTypeNegotiator;
import de.fuberlin.wiwiss.pubby.negotiation.MediaRangeSpec;

public class VelocityWrapper {
	private final static String VELOCITY_ENGINE_INSTANCE = 
			"de.fuberlin.wiwiss.d2rs.VelocityHelper.VELOCITY_ENGINE_INSTANCE";
	private final static String VELOCITY_DEFAULT_CONTEXT = 
			"de.fuberlin.wiwiss.d2rs.VelocityHelper.VELOCITY_DEFAULT_CONTEXT";
	
	private final static String TEXTHTML_CONTENTTYPE = "text/html; charset=utf-8";
	private final static String APPLICATIONXML_CONTENTTYPE = "application/xhtml+xml; charset=utf-8";

	private final static ContentTypeNegotiator xhtmlNegotiator;
	
	static {
		xhtmlNegotiator = new ContentTypeNegotiator();

		// for clients that send nothing
		xhtmlNegotiator.setDefaultAccept(TEXTHTML_CONTENTTYPE);
		
		// for MSIE that sends */* without q
		xhtmlNegotiator.addUserAgentOverride(Pattern.compile("MSIE"), null, TEXTHTML_CONTENTTYPE);
		
		xhtmlNegotiator.addVariant(APPLICATIONXML_CONTENTTYPE + "; q=0.9");
		xhtmlNegotiator.addVariant(TEXTHTML_CONTENTTYPE + "; q=0.8");
	}
	
	
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
	private final HttpServletRequest request;
	private final HttpServletResponse response;
	
	public VelocityWrapper(HttpServlet servlet, HttpServletRequest request, HttpServletResponse response) {
		engine = (VelocityEngine) servlet.getServletContext().getAttribute(VELOCITY_ENGINE_INSTANCE);
		// TODO: Init context with default variables shared by all/many servlets
		Context defaultContext = (Context) servlet.getServletContext().getAttribute(VELOCITY_DEFAULT_CONTEXT);
		context = new VelocityContext(defaultContext);
		this.request = request;
		this.response = response;
	}
	
	public Context getContext() {
		return context;
	}
	
	public VelocityEngine getEngine() {
		return engine;
	}

	public void mergeTemplateXHTML(String templateName) {
		MediaRangeSpec bestMatch = xhtmlNegotiator.getBestMatch(
				request.getHeader("Accept"), request.getHeader("User-Agent"));
		response.addHeader("Content-Type", bestMatch != null ? bestMatch.getMediaType() : TEXTHTML_CONTENTTYPE);
		response.addHeader("Vary", "Accept, User-Agent");

		response.addHeader("Cache-Control", "no-cache");
		response.addHeader("Pragma", "no-cache");
		try {
			engine.mergeTemplate(templateName, "utf-8", context, response.getWriter());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void reportError(int statusCode, String title, String details) throws IOException {
		response.setStatus(statusCode);
		context.put("title", title);
		context.put("details", details);
		mergeTemplateXHTML("error.vm");
	}
}
