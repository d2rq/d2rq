package de.fuberlin.wiwiss.pubby.negotiation;

import java.util.regex.Pattern;

public class PubbyNegotiator {
	private final static ContentTypeNegotiator pubbyNegotiator;
	private final static ContentTypeNegotiator dataNegotiator;
	
	static {
		pubbyNegotiator = new ContentTypeNegotiator();
		pubbyNegotiator.setDefaultAccept("text/html");
		
		// Send HTML to clients that indicate they accept everything.
		// This is specifically so that cURL sees HTML, and also catches
		// various browsers that send "*/*" in some circumstances.
		pubbyNegotiator.addUserAgentOverride(null, "*/*", "text/html");

		// MSIE (7.0) sends either */*, or */* with a list of other random types,
		// but always without q values. That's useless. We will simply send
		// HTML to MSIE, no matter what. Boy, do I hate IE.
		pubbyNegotiator.addUserAgentOverride(Pattern.compile("MSIE"), null, "text/html");
		
		pubbyNegotiator.addVariant("text/html;q=0.81")
				.addAliasMediaType("application/xhtml+xml;q=0.81");
		pubbyNegotiator.addVariant("application/rdf+xml")
				.addAliasMediaType("application/xml;q=0.45")
				.addAliasMediaType("text/xml;q=0.4");
		pubbyNegotiator.addVariant("text/rdf+n3;charset=utf-8;q=0.95")
				.addAliasMediaType("text/n3;q=0.5")
				.addAliasMediaType("application/n3;q=0.5");
		pubbyNegotiator.addVariant("application/x-turtle;q=0.95")
				.addAliasMediaType("application/turtle;q=0.8")
				.addAliasMediaType("text/turtle;q=0.5");
		pubbyNegotiator.addVariant("text/plain;q=0.2");

		dataNegotiator = new ContentTypeNegotiator();
		dataNegotiator.addVariant("application/rdf+xml;q=0.99")
				.addAliasMediaType("application/xml;q=0.45")
				.addAliasMediaType("text/xml;q=0.4");
		dataNegotiator.addVariant("text/rdf+n3;charset=utf-8")
				.addAliasMediaType("text/n3;q=0.5")
				.addAliasMediaType("application/n3;q=0.5");
		dataNegotiator.addVariant("application/x-turtle;q=0.99")
				.addAliasMediaType("application/turtle;q=0.8")
				.addAliasMediaType("text/turtle;q=0.5");
		dataNegotiator.addVariant("text/plain;q=0.2");
	}
	
	public static ContentTypeNegotiator getPubbyNegotiator() {
		return pubbyNegotiator;
	}
	
	public static ContentTypeNegotiator getDataNegotiator() {
		return dataNegotiator;
	}
}
