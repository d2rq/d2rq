package de.fuberlin.wiwiss.d2rq.server;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NamespaceServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		D2RServer d2r = D2RServer.fromServletContext(getServletContext());
		d2r.checkMappingFileChanged();
		response.setContentType("text/javascript");
		ServletOutputStream out = response.getOutputStream();
		d2r.getPrefixes().getNsPrefixMap();
		out.println("// Generated dynamically from the mapping file");
		out.println("var D2R_namespacePrefixes = {");
		Iterator<Entry<String,String>> it = d2r.getPrefixes().getNsPrefixMap().entrySet().iterator();
		while (it.hasNext()) {
			Entry<String,String> entry = it.next();
			out.print("\t\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"");
			if (it.hasNext()) {
				out.print(",");
			}
			out.println();
		}
		out.println("};");
	}

	private static final long serialVersionUID = -1172261621574174019L;
}
