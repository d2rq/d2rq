package de.fuberlin.wiwiss.d2rq.server;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryCancelledException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.ClassMapLister;
import de.fuberlin.wiwiss.d2rq.ResourceDescriber;
import de.fuberlin.wiwiss.d2rq.vocab.SKOS;

public class PageServlet extends HttpServlet {
	private PrefixMapping prefixes;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
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

		/* Determine service stem, i.e. vocab/ in /[vocab/]page */
		int servicePos;
		if (-1 == (servicePos = request.getServletPath().indexOf(
				"/" + D2RServer.getPageServiceName())))
			throw new ServletException("Expected to find service path /"
					+ D2RServer.getPageServiceName());
		String serviceStem = request.getServletPath().substring(1,
				servicePos + 1);

		String resourceURI = server.resourceBaseURI(serviceStem)
				+ relativeResourceURI;
		String documentURL = server.dataURL(serviceStem, relativeResourceURI);
		String pageURL = server.pageURL(serviceStem, relativeResourceURI);

		VelocityWrapper velocity = new VelocityWrapper(this, request, response);
		Context context = velocity.getContext();
		context.put("uri", resourceURI);

		// Build resource description
		Resource resource = ResourceFactory.createResource(resourceURI);
		boolean outgoingTriplesOnly = server.isVocabularyResource(resource)
				&& !server.getConfig().getVocabularyIncludeInstances();
		int limit = server.getConfig().getLimitPerPropertyBridge();
		Model description = null;
		try {
			ResourceDescriber describer = new ResourceDescriber(
					server.getMapping(), resource.asNode(), outgoingTriplesOnly,
					limit, Math.round(server.getConfig().getPageTimeout() * 1000));
			description = ModelFactory.createModelForGraph(describer.description());
		} catch (QueryCancelledException ex) {
			velocity.reportError(
					504, "504 Gateway Timeout", "The operation timed out.");
			return;
		}
		if (description.size() == 0) {
			velocity.reportError(404, "404 Not Found", "No resource with this identifier exists in the database.");
			return;
		}
		// Get a Resource that is attached to the description model
		resource = description.getResource(resourceURI);

		this.prefixes = server.getPrefixes(); // model();

		if (server.getConfig().serveMetadata()) {
			// create and add metadata to context
			MetadataCreator resourceMetadataCreator = new MetadataCreator(
					server, server.getConfig().getResourceMetadataTemplate(
							server, getServletContext()));

			Model metadata = resourceMetadataCreator.addMetadataFromTemplate(
					resourceURI, documentURL, pageURL);
			if (!metadata.isEmpty()) {
				List<Statement> mList = metadata.getResource(documentURL)
						.listProperties().toList();
				Collections.sort(mList, MetadataCreator.subjectSorter);
				context.put("metadata", mList);

				context.put("metadataroot", metadata.getResource(documentURL));

				// add prefixes to context
				Map<String, String> nsSet = metadata.getNsPrefixMap();
				nsSet.putAll(description.getNsPrefixMap());

				context.put("prefixes", nsSet.entrySet());
				context.put("renderedNodesMap",
						new HashMap<Resource, Boolean>());

				// add a empty map for keeping track of blank nodes aliases
				context.put("blankNodesMap", new HashMap<Resource, String>());
			} else {
				context.put("metadata", Boolean.FALSE);
			}
		} else {
			context.put("metadata", Boolean.FALSE);
		}

		context.put("rdf_link", documentURL);
		context.put("label", getBestLabel(resource));
		context.put("properties", collectProperties(description, resource));
		context.put("classmap_links", classmapLinks(resource));
		context.put("limit_per_property_bridge", limit > 0 ? limit : null);
		velocity.mergeTemplateXHTML("resource_page.vm");
	}

	private Collection<Property> collectProperties(Model m, Resource r) {
		Collection<Property> result = new TreeSet<Property>();
		StmtIterator it = r.listProperties();
		while (it.hasNext()) {
			result.add(new Property(it.nextStatement(), false));
		}
		it = m.listStatements(null, null, r);
		while (it.hasNext()) {
			result.add(new Property(it.nextStatement(), true));
		}
		return result;
	}

	private Map<String, String> classmapLinks(Resource resource) {
		Map<String, String> result = new HashMap<String, String>();
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		for (String name : getClassMapLister().classMapNamesForResource(
				resource.asNode())) {
			result.put(name, server.baseURI() + "directory/" + name);
		}
		return result;
	}

	private ClassMapLister getClassMapLister() {
		return D2RServer.retrieveSystemLoader(getServletContext())
				.getClassMapLister();
	}

	private static final long serialVersionUID = 2752377911405801794L;

	public class Property implements Comparable<Property> {
		private Node property;
		private Node value;
		private boolean isInverse;

		Property(Statement stmt, boolean isInverse) {
			this.property = stmt.getPredicate().asNode();
			if (isInverse) {
				this.value = stmt.getSubject().asNode();
			} else {
				this.value = stmt.getObject().asNode();
			}
			this.isInverse = isInverse;
		}

		public boolean isInverse() {
			return this.isInverse;
		}

		public String propertyURI() {
			return this.property.getURI();
		}

		public String propertyQName() {
			String qname = prefixes.shortForm(this.property.getURI());
			if (qname == null) {
				return "<" + this.property.getURI() + ">";
			}
			return qname;
		}

		public String propertyPrefix() {
			String qname = propertyQName();
			if (qname.startsWith("<")) {
				return null;
			}
			return qname.substring(0, qname.indexOf(":") + 1);
		}

		public String propertyLocalName() {
			String qname = propertyQName();
			if (qname.startsWith("<")) {
				return this.property.getLocalName();
			}
			return qname.substring(qname.indexOf(":") + 1);
		}

		public Node value() {
			return this.value;
		}

		public String valueQName() {
			if (!this.value.isURI()) {
				return null;
			}
			return prefixes.qnameFor(this.value.getURI());
		}

		public String datatypeQName() {
			String qname = prefixes
					.qnameFor(this.value.getLiteralDatatypeURI());
			if (qname == null) {
				return "<" + this.value.getLiteralDatatypeURI() + ">";
			}
			return qname;
		}

		public boolean isImg() {
			return FOAF.img.asNode().equals(property)
					|| FOAF.depiction.asNode().equals(property)
					|| FOAF.thumbnail.asNode().equals(property);
		}

		public int compareTo(Property other) {
			String propertyLocalName = this.property.getLocalName();
			String otherLocalName = other.property.getLocalName();
			if (propertyLocalName.compareTo(otherLocalName) != 0) {
				return propertyLocalName.compareTo(otherLocalName);
			}
			if (propertyPrefix().compareTo(other.propertyPrefix()) != 0) {
				return propertyPrefix().compareTo(other.propertyPrefix());
			}
			if (propertyURI().compareTo(other.propertyURI()) != 0) {
				return propertyURI().compareTo(other.propertyURI());
			}
			if (this.isInverse != other.isInverse) {
				return (this.isInverse) ? -1 : 1;
			}
			if (this.value.isURI() || other.value.isURI()) {
				if (!other.value.isURI()) {
					return 1;
				}
				if (!this.value.isURI()) {
					return -1;
				}
				return this.value.getURI().compareTo(other.value.getURI());
			}
			if (this.value.isBlank() || other.value.isBlank()) {
				if (!other.value.isBlank()) {
					return -1;
				}
				if (!this.value.isBlank()) {
					return 1;
				}
				return this.value.getBlankNodeLabel().compareTo(
						other.value.getBlankNodeLabel());
			}
			// TODO Typed literals, language literals
			return this.value.getLiteralLexicalForm().compareTo(
					other.value.getLiteralLexicalForm());
		}
	}

	public static Statement getBestLabel(Resource resource) {
		Statement label = resource.getProperty(RDFS.label);
		if (label == null)
			label = resource.getProperty(SKOS.prefLabel);
		if (label == null)
			label = resource.getProperty(DC.title);
		if (label == null)
			label = resource.getProperty(DCTerms.title);
		if (label == null)
			label = resource.getProperty(FOAF.name);
		return label;
	}
}