package de.fuberlin.wiwiss.d2rs;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.servlet.VelocityServlet;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

public class PageServlet extends VelocityServlet {
	private PrefixMapping prefixes;
	
	public Template handleRequest(HttpServletRequest request,
			HttpServletResponse response,
			Context context) throws IOException {
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
			return null;
		}
		this.prefixes = D2RServer.instance().model();
		Resource resource = description.getResource(resourceURI);
		response.addHeader("Content-Type", "application/xhtml+xml; charset=utf-8");
		response.addHeader("Cache-Control", "no-cache");
		response.addHeader("Pragma", "no-cache");
		setContentType(request, response);
		context.put("truncated_results", new Boolean(D2RServer.instance().hasTruncatedResults()));
		context.put("uri", resourceURI);
		context.put("server_name", D2RServer.instance().serverName());
		context.put("home_link", D2RServer.instance().baseURI());
		context.put("rdf_link", D2RServer.instance().dataURL(relativeResourceURI));
		context.put("label", resource.getProperty(RDFS.label));
		context.put("properties", collectProperties(description, resource));
		context.put("classmap_links", classmapLinks(resource));
		try {
			return getTemplate("resource_page.vm");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
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
		GraphD2RQ g = D2RServer.instance().currentGraph();
		Iterator it = g.classMapNamesForResource(resource.asNode()).iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			result.put(name, D2RServer.instance().baseURI() + "directory/" + name);
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
			if (this.value.isURI()) {
				if (!other.value.isURI()) {
					return 1;
				}
				return this.value.getURI().compareTo(other.value.getURI());
			}
			if (this.value.isBlank()) {
				if (!other.value.isBlank()) {
					return -1;
				}
				return this.value.getBlankNodeLabel().compareTo(other.value.getBlankNodeLabel());
			}
			// TODO Typed literals, language literals
			return this.value.getLiteralLexicalForm().compareTo(other.value.getLiteralLexicalForm());
		}
	}
}
