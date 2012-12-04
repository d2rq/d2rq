package org.d2rq.r2rml;

import java.util.Comparator;

import com.hp.hpl.jena.rdf.model.RDFNode;

public class RDFComparator {

	public static Comparator<RDFNode> getRDFNodeComparator() {
		return RDF_NODE;
	}
	
	private final static Comparator<RDFNode> RDF_NODE = new Comparator<RDFNode>() {
		public int compare(RDFNode n1, RDFNode n2) {
			if (n1.isURIResource()) {
				if (!n2.isURIResource()) return -1;
				return n1.asResource().getURI().compareTo(n2.asResource().getURI());
			}
			if (n1.isAnon()) {
				if (n2.isURIResource()) return 1;
				if (n2.isLiteral()) return -1;
				return n1.asResource().getId().getLabelString().compareTo(n2.asResource().getId().getLabelString());
			}
			if (!n2.isLiteral()) return 1;
			int cmpLex = n1.asLiteral().getLexicalForm().compareTo(n2.asLiteral().getLexicalForm());
			if (cmpLex != 0) return cmpLex;
			if (n1.asLiteral().getDatatypeURI() == null) {
				if (n2.asLiteral().getDatatypeURI() != null) return -1;
				return n1.asLiteral().getLanguage().compareTo(n2.asLiteral().getLanguage());
			}
			if (n2.asLiteral().getDatatypeURI() == null) return 1;
			return n1.asLiteral().getDatatypeURI().compareTo(n2.asLiteral().getDatatypeURI()); 
		}
	};
}