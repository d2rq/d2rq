package de.fuberlin.wiwiss.d2rq.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.download.DownloadContentQuery;
import de.fuberlin.wiwiss.d2rq.map.DownloadMap;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.pubby.negotiation.ContentTypeNegotiator;
import de.fuberlin.wiwiss.pubby.negotiation.MediaRangeSpec;
import de.fuberlin.wiwiss.pubby.negotiation.PubbyNegotiator;

public class ResourceServlet extends HttpServlet {

    public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		D2RServer server = D2RServer.fromServletContext(getServletContext());
		server.checkMappingFileChanged();
		String relativeResourceURI = request.getRequestURI().substring(
				request.getContextPath().length() + request.getServletPath().length());
		// Some servlet containers keep the leading slash, some don't
		if (!"".equals(relativeResourceURI) && "/".equals(relativeResourceURI.substring(0, 1))) {
			relativeResourceURI = relativeResourceURI.substring(1);
		}
		if (request.getQueryString() != null) {
			relativeResourceURI = relativeResourceURI + "?" + request.getQueryString();
		}
		
		/* Determine service stem, i.e. vocab/ in /[vocab/]resource */
		int servicePos;
		if (-1 == (servicePos = request.getServletPath().indexOf("/" + D2RServer.getResourceServiceName())))
				throw new ServletException("Expected to find service name /" + D2RServer.getResourceServiceName());
		String serviceStem = request.getServletPath().substring(1, servicePos + 1);

		String resourceURI = server.resourceBaseURI(serviceStem) + relativeResourceURI;
		if (handleDownload(resourceURI, response, server)) {
			return;
		}

		response.addHeader("Vary", "Accept, User-Agent");
		ContentTypeNegotiator negotiator = PubbyNegotiator.getPubbyNegotiator();
		MediaRangeSpec bestMatch = negotiator.getBestMatch(
				request.getHeader("Accept"), request.getHeader("User-Agent"));
		if (bestMatch == null) {
			response.setStatus(406);
			response.setContentType("text/plain");
			response.getOutputStream().println(
					"406 Not Acceptable: The requested data format is not supported. " +
					"Only HTML and RDF are available.");
			return;
		}
		
		response.setStatus(303);
		response.setContentType("text/plain");
		String location;
		if ("text/html".equals(bestMatch.getMediaType())) {
			location = server.pageURL(serviceStem, relativeResourceURI);
		} else {
			location = server.dataURL(serviceStem, relativeResourceURI);
		}
		response.addHeader("Location", location);
		response.getOutputStream().println(
				"303 See Other: For a description of this item, see " + location);
	}

	private boolean handleDownload(String resourceURI, HttpServletResponse response, D2RServer server) throws IOException {
		Mapping m = D2RServer.retrieveSystemLoader(getServletContext()).getMapping();
		for (Resource r: m.downloadMapResources()) {
			DownloadMap d = m.downloadMap(r);
			DownloadContentQuery q = new DownloadContentQuery(d, resourceURI);
			if (q.hasContent()) {
				response.setContentType(q.getMediaType() != null ? q.getMediaType() : "application/octet-stream");
				InputStream is = q.getContentStream();
				OutputStream os = response.getOutputStream();
				final byte[] buffer = new byte[0x10000];
				int read;
				do {
					read = is.read(buffer, 0, buffer.length);
					if (read>0) {
						os.write(buffer, 0, read);
					}
				} while (read >= 0);
				is.close();
				q.close();
				return true;
			}
		}
		return false;
	}
}
