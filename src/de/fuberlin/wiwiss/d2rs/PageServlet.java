package de.fuberlin.wiwiss.d2rs;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

public class PageServlet extends HttpServlet {
	private PrefixMapping prefixes;
	
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
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
		String resourceURI = server.resourceBaseURI() + relativeResourceURI;
		Model description = QueryExecutionFactory.create(
				"DESCRIBE <" + resourceURI + ">",
				server.dataset()).execDescribe();
		if (description.size() == 0) {
			response.sendError(404);
			return;
		}
		this.prefixes = server.getPrefixes(); // model();
		Resource resource = description.getResource(resourceURI);
		VelocityWrapper velocity = new VelocityWrapper(this, response);
		Context context = velocity.getContext();
		context.put("uri", resourceURI);
		context.put("rdf_link", server.dataURL(relativeResourceURI));
		context.put("label", resource.getProperty(RDFS.label));
		context.put("properties", collectProperties(description, resource));
		context.put("classmap_links", classmapLinks(resource));
		velocity.mergeTemplateXHTML("resource_page.vm");
	}

	private Collection collectProperties(Model m, Resource r) {
		Collection result = new TreeSet();
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

	private Map classmapLinks(Resource resource) {
		Map result = new HashMap();
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		GraphD2RQ g = server.currentGraph();
		Iterator it = g.classMapNamesForResource(resource.asNode()).iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			result.put(name, server.baseURI() + "directory/" + name);
		}
		return result;
	}
	
	private static final long serialVersionUID = 2752377911405801794L;
	
	public class Property implements Comparable {
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
			String qname = prefixes.qnameFor(this.property.getURI());
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
			String qname = prefixes.qnameFor(this.value.getLiteralDatatypeURI());
			if (qname == null) {
				return "<" + this.value.getLiteralDatatypeURI() + ">";
			}
			return qname;
		}
		public int compareTo(Object otherObject) {
			if (!(otherObject instanceof Property)) {
				return 0;
			}
			Property other = (Property) otherObject;
			String propertyLocalName = this.property.getLocalName();
			String otherLocalName = other.property.getLocalName();
			if (propertyLocalName.compareTo(otherLocalName) != 0) {
				return propertyLocalName.compareTo(otherLocalName);
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
				return this.value.getBlankNodeLabel().compareTo(other.value.getBlankNodeLabel());
			}
			// TODO Typed literals, language literals
			return this.value.getLiteralLexicalForm().compareTo(other.value.getLiteralLexicalForm());
		}
	}
}
