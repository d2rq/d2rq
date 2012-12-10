package org.d2rq.validation;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import org.d2rq.pp.PrettyPrinter;
import org.d2rq.validation.Message.Renderer;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.XSD;


public class PlainTextMessageRenderer implements Renderer {
	private final static String NEWLINE = System.getProperty("line.separator");
	
	private final PrintWriter out;
	
	public PlainTextMessageRenderer(OutputStream out) {
		this.out = new PrintWriter(new OutputStreamWriter(out));
	}
	public PlainTextMessageRenderer(Writer out) {
		this.out = new PrintWriter(out);
	}
	
	public void render(Message message) {
		StringBuilder s = new StringBuilder();
		s.append(message.getLevel().name().toUpperCase());
		s.append(": ");
		s.append(populateTemplate(message.getProblemTitle(), message));
		s.append(NEWLINE);
		s.append(NEWLINE);
		boolean needsNewline = false;
/*
		if (message.getSubject() != null) {
			s.append("    resource: ");
			s.append(PrettyPrinter.toString(message.getSubject()));
			s.append(NEWLINE);
			needsNewline = true;
		}
		if (!message.getPredicates().isEmpty()) {
			s.append(message.getPredicates().size() > 1 ? "  predicates" : "   predicate");
			s.append(":");
			for (Property property: message.getPredicates()) {
				s.append(" ");
				s.append(PrettyPrinter.toString(property));
			}
			s.append(NEWLINE);
			needsNewline = true;
		}
		if (!message.getObjects().isEmpty()) {
			s.append(message.getObjects().size() > 1 ? "     objects" : "      object");
			s.append(":");
			for (RDFNode object: message.getObjects()) {
				s.append(" ");
				s.append(PrettyPrinter.toString(object));
			}
			s.append(NEWLINE);
			needsNewline = true;
		}
		if (message.getDetailCode() != null) {
			s.append("        code: " + message.getDetailCode());
			s.append(NEWLINE);
			needsNewline = true;
		}
*/
		if (message.getTemplate() != null) {
			if (needsNewline) {
				s.append(NEWLINE);
			}
			s.append(wordWrap(populateTemplate(message.getTemplate(), message), "  "));
			needsNewline = true;
		}
/*
		if (message.getDetails() != null) {
			if (needsNewline) {
				s.append(NEWLINE);
			}
			s.append(wordWrap(message.getDetails(), "  "));
		}
*/
		out.println(s);
		out.flush();
	}
	
	private String wordWrap(String s, String indent) {
		s = s.replaceAll("[ \\n\\r]{2,}", " ");
		if (s.isEmpty()) return "";
		String[] words = s.split(" ");
		StringBuilder result = new StringBuilder(indent);
		int column = indent.length();
		boolean firstWord = true;
		for (String word: words) {
			if (column + word.length() > 74) {
				result.append(NEWLINE);
				result.append(indent);
				column = indent.length();
			} else if (!firstWord) {
				result.append(" ");
				column++;
			}
			result.append(word);
			column += word.length();
			firstWord = false;
		}
		result.append(NEWLINE);
		return result.toString();
	}
	
	private String populateTemplate(String template, Message message) {
		if (message.getPredicates().size() == 1) {
			template = template.replace("{property}", 
					PrettyPrinter.toString(message.getPredicate()));
		}
		if (message.getPredicates().size() >= 1) {
			StringBuilder and = new StringBuilder();
			StringBuilder or = new StringBuilder();
			for (int i = 0; i < message.getPredicates().size(); i++) {
				and.append(PrettyPrinter.toString(message.getPredicates().get(i)));
				or.append(PrettyPrinter.toString(message.getPredicates().get(i)));
				if (i < message.getPredicates().size() - 2) {
					and.append(", ");
					or.append(", ");
				} else if (i < message.getPredicates().size() - 1) {
					and.append(" and ");
					or.append(" or ");
				}
			}
			template = template.replace("{properties}", and);
			template = template.replace("{properties|or}", or);
		}
		if (message.getObjects().size() == 1) {
			template = template.replace("{object}", 
					PrettyPrinter.toString(message.getObject()));
			if (message.getObjects().get(0).isLiteral()) {
				template = template.replace("{string}",
						message.getValue());
			}
			template = template.replace("{object|nodetype}", 
					nodeType(message.getObject()));
		}
		if (message.getObjects().size() >= 1) {
			StringBuilder and = new StringBuilder();
			StringBuilder or = new StringBuilder();
			for (int i = 0; i < message.getObjects().size(); i++) {
				and.append(PrettyPrinter.toString(message.getObjects().get(i)));
				or.append(PrettyPrinter.toString(message.getObjects().get(i)));
				if (i < message.getObjects().size() - 2) {
					and.append(", ");
					or.append(", ");
				} else if (i < message.getObjects().size() - 1) {
					and.append(" and ");
					or.append(" or ");
				}
			}
			template = template.replace("{objects}", and);
			template = template.replace("{objects|or}", or);
		}
		if (message.getSubject() != null) {
			template = template.replace("{resource}", 
					PrettyPrinter.toString(message.getSubject()));
		}
		if (message.getDetails() != null) {
			template = template.replace("{details}", message.getDetails());
		}
		if (message.getDetails() != null) {
			template = template.replace("{details|withcode}", 
					(message.getDetailCode() == null)
						? message.getDetails()
						: (message.getDetails() + " (" + message.getDetailCode() + ")"));
		}
		if (message.getDetailCode() != null) {
			template = template.replace("{detailcode}", message.getDetailCode());
		}
		return template;
	}
	
	private String nodeType(RDFNode node) {
		if (node.isURIResource()) {
			return "IRI";
		}
		if (node.isAnon()) {
			return "blank node";
		}
		if (!"".equals(node.asLiteral().getLanguage())) {
			return "language-tagged string";
		}
		if (node.asLiteral().getDatatypeURI() == null) {
			return "plain literal";
		}
		if (XSD.xstring.getURI().equals(node.asLiteral().getDatatypeURI())) {
			return "string literal";
		}
		return "non-string typed literal";
	}
}
