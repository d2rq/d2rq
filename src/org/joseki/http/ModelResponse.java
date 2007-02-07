package org.joseki.http;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joseki.QueryExecutionException;
import org.joseki.Request;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Calls into Joseki to send a Jena model over HTTP. This gives us
 * content negotiation and all the other tricks supported by Joseki
 * for free. This has to be in the Joseki package because some
 * required methods are not visible.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ModelResponse.java,v 1.1 2007/02/07 13:49:24 cyganiak Exp $
 */
public class ModelResponse {
	private ResponseHttp josekiResponse;
	private Model model;
	
	public ModelResponse(Model model, HttpServletRequest request, 
			HttpServletResponse response) {
		this.josekiResponse = new ResponseHttp(
				createJosekiRequest(request), request, response);
		this.model = model;
	}
	
	public void serve() {
		try {
			this.josekiResponse.doResponseModel(this.model);
		} catch (QueryExecutionException ex) {
			this.josekiResponse.doException(ex);
		}
	}
	
	private Request createJosekiRequest(HttpServletRequest request) {
		Request result = new Request(request.getRequestURI());
		for (Enumeration en = request.getParameterNames(); en.hasMoreElements(); ) {
			String k = (String) en.nextElement();
			String[] x = request.getParameterValues(k);
            for(int i = 0; i < x.length; i++) {
            	String s = x[i];
            	result.setParam(k, s);
            }
		}
		return result;
	}
}
