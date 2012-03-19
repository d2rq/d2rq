package de.fuberlin.wiwiss.d2rq.server;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.shared.JenaException;

import de.fuberlin.wiwiss.pubby.negotiation.ContentTypeNegotiator;
import de.fuberlin.wiwiss.pubby.negotiation.MediaRangeSpec;
import de.fuberlin.wiwiss.pubby.negotiation.PubbyNegotiator;

/**
 * Calls into Joseki to send a Jena model over HTTP. This gives us
 * content negotiation and all the other tricks supported by Joseki
 * for free. This has to be in the Joseki package because some
 * required methods are not visible.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ModelResponse {
	private final Model model;
	private final HttpServletRequest request;
	private final HttpServletResponse response;
	
	public ModelResponse(Model model, HttpServletRequest request, 
			HttpServletResponse response) {

		// Handle ?output=format request parameter
		RequestParamHandler handler = new RequestParamHandler(request);
		if (handler.isMatchingRequest()) {
			request = handler.getModifiedRequest();
		}
		
		this.model = model;
		this.request = request;
		this.response = response;
	}
	
	public void serve() {
		// Error handling is still quite a mess here.
		try {
			doResponseModel();
		} catch (IOException ioEx) {
			throw new RuntimeException(ioEx);
		} catch (JenaException jEx) {
			try {
				response.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"JenaException: " + jEx.getMessage());
            } catch (IOException e) {
            	throw new RuntimeException(e);
            }
		}
	}
	
	private void doResponseModel() throws IOException {
		response.addHeader("Vary", "Accept");
		ContentTypeNegotiator negotiator = PubbyNegotiator.getDataNegotiator();
		MediaRangeSpec bestMatch = negotiator.getBestMatch(
				request.getHeader("Accept"), request.getHeader("User-Agent"));
		if (bestMatch == null) {
			response.setStatus(406);
			response.setContentType("text/plain");
			ServletOutputStream out = response.getOutputStream();
			out.println("406 Not Acceptable: The requested data format is not supported.");
			out.println("Supported formats are RDF/XML, Turtle, N3, and N-Triples.");
			return;
		}
		response.setContentType(bestMatch.getMediaType());
		getWriter(bestMatch.getMediaType()).write(model, response);
		response.getOutputStream().flush();
    }
	
	private ModelWriter getWriter(String mediaType) {
		if ("application/rdf+xml".equals(mediaType)) {
			return new RDFXMLWriter();
		}
		if ("text/turtle;charset=utf-8".equals(mediaType)) {
			return new TurtleWriter();
		}
		if ("text/rdf+n3;charset=utf-8".equals(mediaType)) {
			return new N3Writer();
		}
		return new NTriplesWriter();
	}
	
	private interface ModelWriter {
		void write(Model model, HttpServletResponse response) throws IOException;
	}
	
	private class NTriplesWriter implements ModelWriter {
		public void write(Model model, HttpServletResponse response) throws IOException {
			model.getWriter("N-TRIPLES").write(model, response.getOutputStream(), null);
		}
	}
	
	private class N3Writer implements ModelWriter {
		public void write(Model model, HttpServletResponse response) throws IOException {
			model.getWriter("N3").write(model, response.getOutputStream(), null);
		}
	}
	
	private class TurtleWriter implements ModelWriter {
		public void write(Model model, HttpServletResponse response) throws IOException {
			model.getWriter("TURTLE").write(model, response.getOutputStream(), null);
		}
	}
	
	private class RDFXMLWriter implements ModelWriter {
		public void write(Model model, HttpServletResponse response) throws IOException {
			RDFWriter writer = model.getWriter("RDF/XML-ABBREV");
			writer.setProperty("showXmlDeclaration", "true");
			// From Joseki -- workaround for the j.cook.up bug.
			writer.setProperty("blockRules", "propertyAttr");
			writer.write(model, 
					new OutputStreamWriter(response.getOutputStream(), "utf-8"), null);
		}
	}
}
