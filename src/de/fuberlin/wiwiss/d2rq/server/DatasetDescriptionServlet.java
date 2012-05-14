package de.fuberlin.wiwiss.d2rq.server;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import de.fuberlin.wiwiss.d2rq.ClassMapLister;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import de.fuberlin.wiwiss.d2rq.vocab.SD;
import de.fuberlin.wiwiss.d2rq.vocab.VoID;
import de.fuberlin.wiwiss.pubby.negotiation.ContentTypeNegotiator;
import de.fuberlin.wiwiss.pubby.negotiation.MediaRangeSpec;
import de.fuberlin.wiwiss.pubby.negotiation.PubbyNegotiator;

/**
 * Serves a VoID Dataset Description for the virtual resources offered by this
 * instance of D2R Server
 * 
 * @author Hannes Mühleisen <hannes@muehleisen.org>
 * 
 */
public class DatasetDescriptionServlet extends HttpServlet {
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		D2RServer server = D2RServer.fromServletContext(getServletContext());

		if (!server.getConfig().serveMetadata()) {
			response.setStatus(404);
			response.setContentType("text/plain");
			response.getOutputStream().println(
					"404 Not Found: Dataset description has been disabled.");
			return;
		}

		Model dDesc = ModelFactory.createDefaultModel();
		dDesc.setNsPrefix("void", VoID.NS);

		Resource sparqlService = dDesc.createResource(server.getSparqlUrl());
		Resource datasetIRI = dDesc.createResource(server.getDatasetIri());

		// this is SPARTA, err... a VoID dataset!
		dDesc.add(datasetIRI, RDF.type, VoID.Dataset);

		dDesc.add(datasetIRI, VoID.sparqlEndpoint, sparqlService);
		// tell everyone we support these formats
		dDesc.add(datasetIRI, VoID.feature,
				dDesc.createResource("http://www.w3.org/ns/formats/RDF_XML"));
		dDesc.add(datasetIRI, VoID.feature,
				dDesc.createResource("http://www.w3.org/ns/formats/N3"));
		dDesc.add(datasetIRI, VoID.feature,
				dDesc.createResource("http://www.w3.org/ns/formats/Turtle"));

		// add a root resource for all classes
		ClassMapLister lister = D2RServer.retrieveSystemLoader(
				getServletContext()).getClassMapLister();
		for (String classMapName : lister.classMapNames()) {
			dDesc.add(
					datasetIRI,
					VoID.rootResource,
					dDesc.createResource(server.baseURI() + "all/"
							+ classMapName));
		}

		// describe our URI space (all under /resource/
		dDesc.add(datasetIRI, VoID.uriSpace,
				dDesc.createLiteral(server.resourceBaseURI()));

		Model mapping = D2RServer.retrieveSystemLoader(getServletContext())
				.getMappingModel();

		Set<String> prefixes = new HashSet<String>();

		// for all d2rq:ClassMap instances, create a void:classPartition
		for (Resource partClass : generatePartitions(mapping, D2RQ.ClassMap,
				D2RQ.class_)) {
			Resource classPartition = dDesc.createResource();
			dDesc.add(classPartition, VoID.class_, partClass);
			dDesc.add(datasetIRI, VoID.classPartition, classPartition);

			prefixes.add(findPrefix(partClass.getURI()));
		}
		// for all d2rq:PropertyBridge, create a void:propertyPartition
		for (Resource partProp : generatePartitions(mapping,
				D2RQ.PropertyBridge, D2RQ.property)) {
			Resource propertyPartition = dDesc.createResource();
			dDesc.add(propertyPartition, VoID.property, partProp);
			dDesc.add(datasetIRI, VoID.propertyPartition, propertyPartition);

			prefixes.add(findPrefix(partProp.getURI()));
		}

		// describe vocabularies
		for (String prefix : prefixes) {
			dDesc.add(datasetIRI, VoID.vocabulary, dDesc.createResource(prefix));
		}

		// add SPARQL endpoint description
		dDesc.setNsPrefix("sd", SD.NS);
		dDesc.add(sparqlService, RDF.type, SD.Service);

		// the SPARQL service URL
		dDesc.add(sparqlService, SD.url, sparqlService);

		// the supported formats (conservative, RDF-XML and SPARQL-Res)
		dDesc.add(sparqlService, SD.resultFormat,
				dDesc.createResource("http://www.w3.org/ns/formats/RDF_XML"));
		dDesc.add(
				sparqlService,
				SD.resultFormat,
				dDesc.createResource("http://www.w3.org/ns/formats/SPARQL_Results_XML"));

		// the dataset description, wich loops back to the VoID description
		Resource defaultDatasetDesc = dDesc.createResource();
		dDesc.add(sparqlService, SD.defaultDatasetDescription,
				defaultDatasetDesc);
		dDesc.add(defaultDatasetDesc, RDF.type, SD.Dataset);
		dDesc.add(defaultDatasetDesc, SD.defaultGraph, datasetIRI);
		dDesc.add(datasetIRI, RDF.type, SD.Graph);

		// add user-specified dataset metadata, either from default or
		// user-specified metadata template
		Model datasetMetadataTemplate = server.getConfig()
				.getDatasetMetadataTemplate(server, getServletContext());
		MetadataCreator datasetMetadataCreator = new MetadataCreator(server,
				datasetMetadataTemplate);
		dDesc.add(datasetMetadataCreator.addMetadataFromTemplate(
				server.getDatasetIri(), server.getDatasetIri(),
				server.getDatasetIri()));
		Map<String, String> dDescPrefixes = dDesc.getNsPrefixMap();
		dDescPrefixes.putAll(datasetMetadataTemplate.getNsPrefixMap());
		dDesc.setNsPrefixes(dDescPrefixes);

		// decide whether to serve RDF or HTML
		ContentTypeNegotiator negotiator = PubbyNegotiator.getPubbyNegotiator();
		MediaRangeSpec bestMatch = negotiator.getBestMatch(
				request.getHeader("Accept"), request.getHeader("User-Agent"));
		if (bestMatch == null) {
			response.setStatus(406);
			response.setContentType("text/plain");
			response.getOutputStream().println(
					"406 Not Acceptable: The requested data format is not supported. "
							+ "Only HTML and RDF are available.");
			return;
		}

		if ("text/html".equals(bestMatch.getMediaType())) {
			// render HTML response
			VelocityWrapper velocity = new VelocityWrapper(this, request,
					response);
			Context context = velocity.getContext();
			// context.put("classmap_links", classMapLinks);

			List<Statement> mList = datasetIRI.listProperties().toList();
			Collections.sort(mList, MetadataCreator.subjectSorter);
			context.put("metadata", mList);

			// add prefixes to context
			Map<String, String> nsSet = dDesc.getNsPrefixMap();
			context.put("prefixes", nsSet.entrySet());

			context.put("dataset_iri", datasetIRI);

			// add a empty map for keeping track of blank nodes aliases
			context.put("blankNodesMap", new HashMap<Resource, String>());
			context.put("renderedNodesMap", new HashMap<Resource, Boolean>());

			velocity.mergeTemplateXHTML("dataset_page.vm");
		} else {
			// render RDF response
			new ModelResponse(dDesc, request, response).serve();
		}
	}

	// http://www.w3.org/TR/void/#dublin-core

	private static Set<Resource> generatePartitions(Model m, Resource type,
			Property p) {
		Set<Resource> partitions = new HashSet<Resource>();
		ResIterator classIt = m.listResourcesWithProperty(RDF.type, type);
		while (classIt.hasNext()) {
			Resource classMap = classIt.next();
			StmtIterator pIt = classMap.listProperties(p);
			while (pIt.hasNext()) {
				partitions.add((Resource) pIt.next().getObject());
			}
		}
		return partitions;
	}

	private static String findPrefix(String r) {
		if (r == null) {
			// silently fail, impact of inaccurate prefixes is limited
			return "";
		}
		if (r.lastIndexOf("#") > -1) {
			return r.substring(0, r.lastIndexOf("#") + 1);
		}
		if (r.lastIndexOf("/") > -1) {
			return r.substring(0, r.lastIndexOf("/") + 1);
		}
		return "";
	}
}
