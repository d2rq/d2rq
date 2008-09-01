package de.fuberlin.wiwiss.d2rs;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Analyzes an HttpServletRequest to check for the presence
 * of an ?output=n3 or ?output=rdfxml request parameter in
 * the URI. If present, returns a modified HttpServletRequest
 * that has the appropriate MIME type in the Accept: header.
 * This request can then be fed into the rest of our content
 * negotiation based tooling.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: RequestParamHandler.java,v 1.2 2008/09/01 23:56:31 cyganiak Exp $
 */
public class RequestParamHandler {
	private static final String ATTRIBUTE_NAME_IS_HANDLED =
		"OutputRequestParamHandler.isHandled";
	private final static HashMap mimeTypes = new HashMap();
	static {
		mimeTypes.put("rdfxml", "application/rdf+xml");
		mimeTypes.put("xml", "application/rdf+xml");
		mimeTypes.put("turtle", "application/x-turtle");
		mimeTypes.put("ttl", "application/x-turtle");
		mimeTypes.put("n3", "text/rdf+n3;charset=utf-8");
		mimeTypes.put("nt", "text/plain");
		mimeTypes.put("text", "text/plain");
	}
	
	/**
	 * Removes the "output=foobar" part of a URI if present.
	 */
	public static String removeOutputRequestParam(String uri) {
		// Remove the param: output=[a-z0-9]*
		// There are two cases. The param can occur at the end of the URI,
		// this is matched by the first part: [?&]param$
		// Or it can occur elsewhere, then it's matched by param& with a
		// lookbehind that requires [?&] to occur before the pattern.
		return uri.replaceFirst("([?&]output=[a-z0-9]*$)|((?<=[?&])output=[a-z0-9]*&)", "");
	}
	
	private final HttpServletRequest request;
	private final String requestedType;

	public RequestParamHandler(HttpServletRequest request) {
		this.request = request;
		requestedType = identifyRequestedType(request.getParameter("output"));
	}
	
	public boolean isMatchingRequest() {
		if ("true".equals(request.getAttribute(ATTRIBUTE_NAME_IS_HANDLED))) {
			return false;
		}
		return requestedType != null;
	}

	public HttpServletRequest getModifiedRequest() {
		return new WrappedRequest();
	}
	
	private String identifyRequestedType(String parameterValue) {
		if (mimeTypes.containsKey(parameterValue)) {
			return parameterValue;
		}
		return null;
	}
	
	private class WrappedRequest extends HttpServletRequestWrapper {
		WrappedRequest() {
			super(request);
			setAttribute(ATTRIBUTE_NAME_IS_HANDLED, "true");
		}
		public String getHeader(String name) {
			if ("accept".equals(name.toLowerCase())) {
				return (String) mimeTypes.get(requestedType);
			}
			return super.getHeader(name);
		}
		public Enumeration getHeaderNames() {
			final Enumeration realHeaders = super.getHeaderNames();
			return new Enumeration() {
				private String prefetched = null;
				public boolean hasMoreElements() {
					while (prefetched == null && realHeaders.hasMoreElements()) {
						String next = (String) realHeaders.nextElement();
						if (!"accept".equals(next.toLowerCase())) {
							prefetched = next;
						}
					}
					return (prefetched != null);
				}
				public Object nextElement() {
					return prefetched;
				}
			};
		}
		public Enumeration getHeaders(String name) {
			if ("accept".equals(name.toLowerCase())) {
				Vector v = new Vector();
				v.add(getHeader(name));
				return v.elements();
			}
			return super.getHeaders(name);
		}
	}
}
