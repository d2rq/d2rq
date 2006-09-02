package de.fuberlin.wiwiss.d2rq.pp;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.shared.PrefixMapping;

public class PrettyPrinter {

	/**
	 * Pretty-prints an RDF node.
	 * @param n An RDF node
	 * @return An N-Triples style textual representation
	 */
	public static String toString(Node n) {
		return toString(n, null);
	}
	
	/**
	 * Pretty-prints an RDF node and shortens URIs into QNames according to a
	 * {@link PrefixMapping}.
	 * @param n An RDF node
	 * @return An N-Triples style textual representation with URIs shortened to QNames
	 */
	public static String toString(Node n, PrefixMapping prefixes) {
		if (n.isURI()) {
			return qNameOrURI(n.getURI(), prefixes);
		}
		if (n.isBlank()) {
			return "_:" + n.getBlankNodeLabel();
		}
		// Literal
		String s = "\"" + n.getLiteralLexicalForm() + "\"";
		if (!"".equals(n.getLiteralLanguage())) {
			s += "@" + n.getLiteralLanguage();
		}
		if (n.getLiteralDatatype() != null) {
			s += "^^" + qNameOrURI(n.getLiteralDatatypeURI(), prefixes);
		}
		return s;
	}
	
	private static String qNameOrURI(String uri, PrefixMapping prefixes) {
		if (prefixes == null) {
			return "<" + uri + ">";
		}
		String qName = prefixes.qnameFor(uri);
		if (qName != null) {
			return qName;
		}
		return "<" + uri + ">";
		
	}
	
	public static String toString(Triple t) {
		return toString(t, null);
	}
	
	public static String toString(Triple t, PrefixMapping prefixes) {
		return toString(t.getSubject(), prefixes) + " "
				+ toString(t.getPredicate(), prefixes) + " "
				+ toString(t.getObject(), prefixes) + " .";
	}
}
