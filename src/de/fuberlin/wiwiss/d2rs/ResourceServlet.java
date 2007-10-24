package de.fuberlin.wiwiss.d2rs;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joseki.Joseki;
import org.joseki.http.AcceptItem;
import org.joseki.http.AcceptList;

public class ResourceServlet extends HttpServlet {
	private static String contentTypeHTML = "text/html";
	private static String contentTypeXHTML = "application/xhtml+xml";

	private static AcceptItem defaultContentType = new AcceptItem(contentTypeHTML);
    
	private static AcceptList supportedContentTypes = new AcceptList(new String[]{
			// HTML types
			contentTypeHTML,
			contentTypeXHTML,

			// RDF types should mirror Joseki's accepted formats from ResponseHttp.java
//			Joseki.contentTypeXML,
			Joseki.contentTypeRDFXML,
			Joseki.contentTypeTurtle,
			Joseki.contentTypeN3,
			Joseki.contentTypeN3Alt,
			Joseki.contentTypeNTriples
	});

    public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String relativeResourceURI = request.getRequestURI().substring(request.getServletPath().length() + 1);
		if (request.getQueryString() != null) {
			relativeResourceURI = relativeResourceURI + "?" + request.getQueryString();
		}
		if (clientPrefersHTML(request)) {
			response.addHeader("Location",
					D2RServer.instance().pageURL(relativeResourceURI));
		} else {
			response.addHeader("Location",
					D2RServer.instance().dataURL(relativeResourceURI));
		}
		response.setStatus(303);
	}

	private boolean clientPrefersHTML(HttpServletRequest request) {
		// This should use Joseki's HttpUtils.chooseContentType(...), but it
		// is buggy, so we use our own implementation until Joseki is fixed.
		// The Joseki version ignores ";q=x.x" values in the "Accept:" header.
		AcceptItem bestFormat = chooseContentType(
				request, supportedContentTypes, defaultContentType);

		return contentTypeHTML.equals(bestFormat.getAcceptType())
				|| contentTypeXHTML.equals(bestFormat.getAcceptType());
	}

	private AcceptItem chooseContentType(HttpServletRequest request, 
			AcceptList offeredContentTypes, AcceptItem defaultContentType) {
		String acceptHeader = request.getHeader("Accept");
		if (acceptHeader == null) {
			return defaultContentType;
		}
		AcceptList acceptedContentTypes = new AcceptList(acceptHeader);
		AcceptItem bestMatch = match(acceptedContentTypes, offeredContentTypes) ;
		if (bestMatch == null) {
			return defaultContentType;
		}
		return bestMatch;
	}
	
	private AcceptItem match(AcceptList accepted, AcceptList offered) {
        for ( Iterator iter = accepted.iterator() ; iter.hasNext() ; )
        {
            AcceptItem i2 = (AcceptItem)iter.next() ;
            AcceptItem m = offered.match(i2) ;
            if ( m != null )
                return m ;
        }
        return null ;
	}
	
	private static final long serialVersionUID = 2752377911405801794L;
}
