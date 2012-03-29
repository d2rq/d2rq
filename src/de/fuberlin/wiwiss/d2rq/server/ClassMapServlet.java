package de.fuberlin.wiwiss.d2rq.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.ClassMapLister;

public class ClassMapServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		server.checkMappingFileChanged();
		if (request.getPathInfo() == null) {
			new ModelResponse(classMapListModel(), request, response).serve();
			return;
		}
		String classMapName = request.getPathInfo().substring(1);
		Model resourceList = getClassMapLister().classMapInventory(classMapName);
		if (resourceList == null) {
			response.sendError(404, "Sorry, class map '" + classMapName + "' not found.");
			return;
		}
    	Resource classMap = resourceList.getResource(server.baseURI() + "all/" + classMapName);
    	Resource directory = resourceList.createResource(server.baseURI() + "all");
    	classMap.addProperty(RDFS.seeAlso, directory);
    	classMap.addProperty(RDFS.label, "List of all instances: " + classMapName);
    	directory.addProperty(RDFS.label, "D2R Server contents");
    	server.addDocumentMetadata(resourceList, classMap);
		new ModelResponse(resourceList, request, response).serve();
	}

	private ClassMapLister getClassMapLister() {
		return D2RServer.retrieveSystemLoader(getServletContext()).getClassMapLister();
	}
	
	private Model classMapListModel() {
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		Model result = ModelFactory.createDefaultModel();
		Resource list = result.createResource(server.baseURI() + "all");
		list.addProperty(RDFS.label, "D2R Server contents");
		for (String classMapName: getClassMapLister().classMapNames()) {
			Resource instances = result.createResource(server.baseURI() + "all/" + classMapName);
			list.addProperty(RDFS.seeAlso, instances);
			instances.addProperty(RDFS.label, "List of all instances: " + classMapName);
		}
		server.addDocumentMetadata(result, list);
		return result;
	}
	
	private static final long serialVersionUID = 6467361762380096163L;
}
