package de.fuberlin.wiwiss.d2rs;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joseki.http.ModelResponse;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;

public class ResourceDescriptionServlet extends HttpServlet {
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String relativeResourceURI = request.getRequestURI().substring(request.getServletPath().length() + 1);
		if (request.getQueryString() != null) {
			relativeResourceURI = relativeResourceURI + "?" + request.getQueryString();
		}
		String resourceURI = D2RServer.instance().resourceBaseURI() + relativeResourceURI;
		Model description = QueryExecutionFactory.create(
				"DESCRIBE <" + resourceURI + ">",
				D2RServer.instance().dataset()).execDescribe();
		if (description.size() == 0) {
			response.sendError(404);
		}
		new ModelResponse(description, request, response).serve();
//		Resource resource = description.getResource(resourceURI);
	}

	private static final long serialVersionUID = -4898674928803998210L;
}