package de.fuberlin.wiwiss.d2rs;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceServlet extends HttpServlet {

	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String relativeResourceURI = request.getPathInfo().substring(1);
		if (request.getQueryString() != null) {
			relativeResourceURI = relativeResourceURI + "?" + request.getQueryString();
		}
		response.setStatus(303);
		if ("application/rdf+xml".equals(new ContentNegotiator(request.getHeader("Accept")).bestFormat())) {
			response.addHeader("Location",
					D2RServer.instance().graphURLDescribingResource(relativeResourceURI));
		} else {
			response.addHeader("Location",
					D2RServer.instance().baseURI() + "page/" + relativeResourceURI);
		}
	}

	private static final long serialVersionUID = 2752377911405801794L;
}
