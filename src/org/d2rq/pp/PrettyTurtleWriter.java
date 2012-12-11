package org.d2rq.pp;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.d2rq.lang.MapObject;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.PrefixMapping;

public class PrettyTurtleWriter {
	
	private static Writer toWriter(OutputStream out) {
		try {
			return new OutputStreamWriter(out, "utf-8");
		} catch (UnsupportedEncodingException ex) {
			// Can't happen, UTF-8 is always supported
			throw new RuntimeException(ex);
		}
	}
	
	private final PrintWriter out;
	private final PrefixMapping prefixes;
	private final Map<Resource,String> blankNodeMap = new HashMap<Resource,String>();
	private final Stack<Boolean> compactStack = new Stack<Boolean>();
	private int blankNodeCounter = 1;
	private int indent = 0;
	
	public PrettyTurtleWriter(PrefixMapping prefixes, Writer out) {
		this.out = new PrintWriter(out);
		this.prefixes = prefixes;
		printPrefixes(prefixes);
		compactStack.push(false);
	}
	
	public PrettyTurtleWriter(PrefixMapping prefixes, OutputStream out) {
		this(prefixes, toWriter(out));
	}
	
	public void flush() {
		out.flush();
	}
	
	public void println() {
		out.println();
	}
	
	public void printComment(String comment) {
		printIndent();
		out.print("# ");
		out.print(comment);
	}
	
	public void printPrefixes(PrefixMapping prefixes) {
		List<String> p = new ArrayList<String>(prefixes.getNsPrefixMap().keySet());
		Collections.sort(p);
		for (String prefix: p) {
			printPrefix(prefix, prefixes.getNsPrefixURI(prefix));
		}
	}
	
	public void printPrefix(String prefix, String uri) {
		out.println("@prefix " + prefix + ": <" + uri + ">.");
	}
	
	public void printProperty(Property property, MapObject object) {
		if (object == null) return;
		printProperty(property, object.resource());
	}
	
	public void printResourceStart(Resource resource) {
		printIndent();
		out.print(toTurtle(resource));
		indent += 4;
	}
	
	public void printResourceStart(Resource resource, Resource class_) {
		printIndent();
		out.print(toTurtle(resource));
		out.print(" a ");
		out.print(toTurtle(class_));
		out.print(";");
		indent += 4;
	}
	
	public void printResourceEnd() {
		printIndent();
		out.print(".");
		indent -= 4;
	}
	
	public void printPropertyStart(Property property) {
		printPropertyStart(property, false);
	}
	
	public void printPropertyStart(Property property, boolean compact) {
		printIndent();
		out.print(toTurtle(property));
		out.print(" [");
		indent += 4;
		compactStack.push(compact);
	}
	
	public void printPropertyEnd() {
		indent -= 4;
		printIndent();
		out.print("];");
		compactStack.pop();
	}
	
	public void printIndent() {
		if (compactStack.peek()) {
			out.print(' ');
		} else {
			out.println();
			for (int i = 0; i < indent; i++) out.print(' ');
		}
	}
	
	public void printProperty(boolean writeIt, Property property, boolean value) {
		if (!writeIt) return;
		printPropertyTurtle(writeIt, property, value ? "true" : "false");
	}
	
	public void printProperty(boolean writeIt, Property property, int value) {
		if (!writeIt) return;
		printPropertyTurtle(writeIt, property, Integer.toString(value));
	}
	
	public void printProperty(boolean writeIt, Property property, String value) {
		if (!writeIt) return;
		printPropertyTurtle(writeIt, property, quote(value));
	}
	
	public void printProperty(Property property, String value) {
		printProperty(value != null, property, value);
	}
	
	public void printLongStringProperty(Property property, String value) {
		printPropertyTurtle(value != null, property, quoteLong(value));
	}
	
	public void printURIProperty(Property property, String uri) {
		printProperty(property, uri == null ? null : ResourceFactory.createResource(uri));
	}
	
	public void printProperty(Property property, RDFNode term) {
		if (term == null) return;
		if (term.isResource()) {
			printPropertyTurtle(term != null, property, toTurtle(term.asResource()));
		} else {
			printPropertyTurtle(term != null, property, toTurtle(term.asLiteral()));
		}
	}
	
	public void printRDFNodeProperties(Property property, Collection<? extends RDFNode> terms) {
		for (RDFNode term: terms) {
			printProperty(property, term);
		}
	}
	
	public void printStringProperties(Property property, Collection<String> values) {
		for (String value: values) {
			printProperty(property, value);
		}
	}
	
	public void printProperty(Property property, Map<Property,RDFNode> value) {
		printPropertyTurtle(value != null && !value.isEmpty(), property, 
				toTurtleCompact(value));
	}
	
	public void printCompactBlankNodeProperties(Property property, 
			List<Map<Property,RDFNode>> values) {
		boolean hasNonEmpty = false;
		for (Map<Property,RDFNode> value: values) {
			if (value.isEmpty()) continue;
			hasNonEmpty = true;
		}
		if (!hasNonEmpty) return;
		printIndent();
		out.print(toTurtle(property));
		indent += 4;
		int count = 0;
		for (Map<Property,RDFNode> value: values) {
			printIndent();
			out.print(toTurtleCompact(value));
			count++;
			out.print(count == values.size() ? ";" : ",");
		}
		indent -= 4;
	}

	public void printPropertyTurtle(boolean writeIt, Property property, String turtleSnippet) {
		if (!writeIt) return;
		printIndent();
		out.print(toTurtle(property));
		out.print(" ");
		out.print(turtleSnippet);
		out.print(";");
	}

	private String quote(String s) {
		if (s.contains("\n") || s.contains("\r")) {
			return quoteLong(s);
		}
		return "\"" + s.replaceAll("\"", "\\\"") + "\"";
	}
	
	private String quoteLong(String s) {
		if (s.endsWith("\"")) {
			s = s.substring(0, s.length() - 1) + "\\\"";
		}
		return "\"\"\"" + s.replaceAll("\"\"\"", "\\\"\\\"\\\"") + "\"\"\"";
	}
	
	private String toTurtle(RDFNode r) {
		if (r.isURIResource()) {
			return PrettyPrinter.toString(r.asNode(), prefixes);
		} else if (r.isLiteral()) {
			StringBuffer result = new StringBuffer(quote(r.asLiteral().getLexicalForm()));
			if (!"".equals(r.asLiteral().getLanguage())) {
				result.append("@");
				result.append(r.asLiteral().getLanguage());
			} else if (r.asLiteral().getDatatype() != null) {
				result.append("^^");
				result.append(toTurtle(ResourceFactory.createResource(r.asLiteral().getDatatypeURI())));
			}
			return result.toString();
		} else {
			if (!blankNodeMap.containsKey(r)) {
				blankNodeMap.put(r.asResource(), "_:b" + blankNodeCounter++);
			}
			return blankNodeMap.get(r);
		}
	}
	
	private String toTurtleCompact(Map<Property, RDFNode> resource) {
		if (resource.isEmpty()) return "[]";
		StringBuilder result = new StringBuilder("[ ");
		for (Property property: resource.keySet()) {
			result.append(toTurtle(property));
			result.append(" ");
			result.append(toTurtle(resource.get(property)));
			result.append("; ");
		}
		result.append("]");
		return result.toString();
	}
}
