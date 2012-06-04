package de.fuberlin.wiwiss.d2rq.server;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class ResourceDescriptionServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		server.checkMappingFileChanged();
		String relativeResourceURI = request.getRequestURI().substring(
				request.getContextPath().length()
						+ request.getServletPath().length());
		// Some servlet containers keep the leading slash, some don't
		if (!"".equals(relativeResourceURI)
				&& "/".equals(relativeResourceURI.substring(0, 1))) {
			relativeResourceURI = relativeResourceURI.substring(1);
		}
		if (request.getQueryString() != null) {
			relativeResourceURI = relativeResourceURI + "?"
					+ request.getQueryString();
		}

		/* Determine service stem, i.e. vocab/ in /[vocab/]data */
		int servicePos;
		if (-1 == (servicePos = request.getServletPath().indexOf(
				"/" + D2RServer.getDataServiceName())))
			throw new ServletException("Expected to find service path /"
					+ D2RServer.getDataServiceName());
		String serviceStem = request.getServletPath().substring(1,
				servicePos + 1);

		String resourceURI = RequestParamHandler
				.removeOutputRequestParam(server.resourceBaseURI(serviceStem)
						+ relativeResourceURI);
		String documentURL = server.dataURL(serviceStem, relativeResourceURI);

		String pageURL = server.pageURL(serviceStem, relativeResourceURI);

		String sparqlQuery = "DESCRIBE <" + resourceURI + ">";
		QueryExecution qe = QueryExecutionFactory.create(sparqlQuery,
				server.dataset());
		if (server.getConfig().getPageTimeout() > 0) {
			qe.setTimeout(Math.round(server.getConfig().getPageTimeout() * 1000));
		}
		Model description = qe.execDescribe();
		qe.close();
		
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
			document.addProperty(RDFS.label,
					"RDF Description of " + label.getString());
		}
		server.addDocumentMetadata(description, document);
		if (server.getConfig().serveMetadata()) {
			// add document metadata from template
			Model resourceMetadataTemplate = server.getConfig().getResourceMetadataTemplate(
					server, getServletContext());
			MetadataCreator resourceMetadataCreator = new MetadataCreator(
					server, resourceMetadataTemplate);
			description.add(resourceMetadataCreator.addMetadataFromTemplate(
					resourceURI, documentURL, pageURL));
			
			Map<String, String> descPrefixes = description.getNsPrefixMap();
			descPrefixes.putAll(resourceMetadataTemplate.getNsPrefixMap());
			description.setNsPrefixes(descPrefixes);
		}
		// TODO: Add a Content-Location header
		new ModelResponse(description, request, response).serve();
	}
}