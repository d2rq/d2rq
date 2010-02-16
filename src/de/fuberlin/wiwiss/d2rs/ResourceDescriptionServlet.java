package de.fuberlin.wiwiss.d2rs;

import java.io.IOException;

import javax.servlet.ServletException;
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
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		server.checkMappingFileChanged();
		String relativeResourceURI = request.getRequestURI().substring(
				request.getContextPath().length() + request.getServletPath().length());
		// Some servlet containers keep the leading slash, some don't
		if (!"".equals(relativeResourceURI) && "/".equals(relativeResourceURI.substring(0, 1))) {
			relativeResourceURI = relativeResourceURI.substring(1);
		}
		if (request.getQueryString() != null) {
			relativeResourceURI = relativeResourceURI + "?" + request.getQueryString();
		}
		
		/* Determine service stem, i.e. vocab/ in /[vocab/]data */
		int servicePos;
		if (-1 == (servicePos = request.getServletPath().indexOf("/" + D2RServer.getDataServiceName())))
				throw new ServletException("Expected to find service path /" + D2RServer.getDataServiceName());
		String serviceStem = request.getServletPath().substring(1, servicePos + 1);		
				
		String resourceURI = RequestParamHandler.removeOutputRequestParam(
				server.resourceBaseURI(serviceStem) + relativeResourceURI);
		String documentURL = server.dataURL(serviceStem, relativeResourceURI);

		String sparqlQuery = "DESCRIBE <" + resourceURI + ">";
		Model description = QueryExecutionFactory.create(sparqlQuery, server.dataset()).execDescribe();	
			
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
		
		MetadataCreator mc = new MetadataCreator(server, new VelocityWrapper(this, request, response));
		mc.setResourceURI(resourceURI);
		mc.setDocumentURL(documentURL);
		mc.setSparqlQuery(sparqlQuery);

		description.add(mc.addMetadataFromTemplate(description.createResource(resourceURI), getServletContext()));
				
// TODO: Add a Content-Location header
		new ModelResponse(description, request, response).serve();
//		Resource resource = description.getResource(resourceURI);
	}

	private static final long serialVersionUID = -4898674928803998210L;
}