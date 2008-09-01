package de.fuberlin.wiwiss.d2rs;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rs.vocab.FOAF;

public class ResourceDescriptionServlet extends HttpServlet {
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		String relativeResourceURI = request.getRequestURI().substring(
				request.getContextPath().length() + request.getServletPath().length());
		// Some servlet containers keep the leading slash, some don't
		if (!"".equals(relativeResourceURI) && "/".equals(relativeResourceURI.substring(0, 1))) {
			relativeResourceURI = relativeResourceURI.substring(1);
		}
		if (request.getQueryString() != null) {
			relativeResourceURI = relativeResourceURI + "?" + request.getQueryString();
		}
		String resourceURI = RequestParamHandler.removeOutputRequestParam(
				server.resourceBaseURI() + relativeResourceURI);
		String documentURL = server.dataURL(relativeResourceURI);

		Model description = QueryExecutionFactory.create(
				"DESCRIBE <" + resourceURI + ">",
				server.dataset()).execDescribe();
		if (description.size() == 0) {
			response.sendError(404);
		}
		if (description.qnameFor(FOAF.primaryTopic.getURI()) == null
				&& description.getNsPrefixURI("foaf") == null) {
			description.setNsPrefix("foaf", FOAF.NS);
		}
		Resource resource = description.getResource(resourceURI);
		Resource document = description.getResource(documentURL);
		document.addProperty(FOAF.primaryTopic, resource);
		Statement label = resource.getProperty(RDFS.label);
		if (label != null) {
			document.addProperty(RDFS.label, "RDF Description of " + label.getString());
		}
		server.addDocumentMetadata(description, document);
		new ModelResponse(description, request, response).serve();
//		Resource resource = description.getResource(resourceURI);
	}

	private static final long serialVersionUID = -4898674928803998210L;
}