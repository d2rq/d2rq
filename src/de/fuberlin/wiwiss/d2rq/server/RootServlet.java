package de.fuberlin.wiwiss.d2rq.server;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import de.fuberlin.wiwiss.d2rq.ClassMapLister;

public class RootServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		server.checkMappingFileChanged();
		Map<String,String> classMapLinks = new TreeMap<String,String>();
		ClassMapLister lister = D2RServer.retrieveSystemLoader(getServletContext()).getClassMapLister();
		for (String name: lister.classMapNames()) {
			classMapLinks.put(name, server.baseURI() + "directory/" + name);
		}
		VelocityWrapper velocity = new VelocityWrapper(this, request, response);
		Context context = velocity.getContext();
		context.put("rdf_link", server.baseURI() + "all");
		context.put("classmap_links", classMapLinks);
		velocity.mergeTemplateXHTML("root_page.vm");
	}

	private static final long serialVersionUID = 8398973058486421941L;
}
