package de.fuberlin.wiwiss.d2rs;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String relativeResourceURI = request.getPathInfo().substring(1);
		if (request.getQueryString() != null) {
			relativeResourceURI = relativeResourceURI + "?" + request.getQueryString();
		}
		response.setStatus(303);
		response.addHeader("Location",
				D2RServer.instance().graphURLDescribingResource(relativeResourceURI));
	}

	private static final long serialVersionUID = 2752377911405801794L;
}
