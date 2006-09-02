package de.fuberlin.wiwiss.d2rs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentNegotiator {
	private final static String RDF = "application/rdf+xml";
	private final static String HTML = "text/html";
	private final static Set rdfMIMETypes = new HashSet(
			Arrays.asList(new String[]{
					"application/rdf+xml",
					"text/rdf"
			}));
	private final static Set htmlMIMETypes = new HashSet(
			Arrays.asList(new String[]{
					"text/html",
					"application/xhtml+xml",
					"text/*"
			}));
	
	private String acceptHeader;
	
	public ContentNegotiator(String acceptHeader) {
		this.acceptHeader = acceptHeader;
	}

	public String bestFormat() {
		List acceptedTypes = parse(this.acceptHeader);
		Iterator it = acceptedTypes.iterator();
		double bestHTMLMatch = 0.0d;
		double bestRDFMatch = 0.0d;
		while (it.hasNext()) {
			AcceptSpec spec = (AcceptSpec) it.next();
			bestHTMLMatch = Math.max(bestHTMLMatch, spec.matchesAny(htmlMIMETypes));
			bestRDFMatch = Math.max(bestRDFMatch, spec.matchesAny(rdfMIMETypes));
		}
		return (bestHTMLMatch >= bestRDFMatch) ? HTML : RDF;
	}

	private List parse(String acceptSpec) {
		if (acceptSpec == null) {
			return Collections.EMPTY_LIST;
		}
		List results = new ArrayList();
		Iterator it = splitClauses(acceptSpec).iterator();
		while (it.hasNext()) {
			String clause = (String) it.next();
			results.add(new AcceptSpec(
					stripParameters(clause), quality(clause)));
			
		}
		return results;
	}
	
	private List splitClauses(String acceptSpec) {
		List results = new ArrayList();
		StringTokenizer tokenizer = new StringTokenizer(acceptSpec, ",");
		while (tokenizer.hasMoreTokens()) {
			results.add(tokenizer.nextToken());
		}
		return results;
	}
	
	private String stripParameters(String clause) {
		if (clause.indexOf(";") == -1) {
			return clause;
		}
		return clause.substring(0, clause.indexOf(";"));
	}
	
	private double quality(String clause) {
		Matcher match = Pattern.compile(".*;q=(0|0?\\.[0-9]+)(;.*)?").matcher(clause);
		if (!match.matches()) {
			return 1.0d;
		}
		String s = match.group(1);
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException ex) {
			return 1.0d;
		}
	}
	
	private class AcceptSpec {
		private String mimeType;
		private double quality;
		AcceptSpec(String mimeType, double quality) {
			this.mimeType = mimeType;
			this.quality = quality;
		}
		double matchesAny(Collection mimeTypes) {
			return mimeTypes.contains(this.mimeType)
					? this.quality
					: 0.0d;
		}
	}
}
