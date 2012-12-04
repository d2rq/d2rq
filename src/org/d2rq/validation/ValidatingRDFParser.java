package org.d2rq.validation;

import org.d2rq.validation.Message.Problem;
import org.openjena.riot.RiotException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;


public class ValidatingRDFParser {
	private final String file;
	private final Report report;
	
	public ValidatingRDFParser(String file, Report report) {
		this.file = file;
		this.report = report;
	}
	
	public Model parse() {
		try {
			return FileManager.get().loadModel(file);
		} catch (JenaException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof RiotException) {
				report.report(Problem.SYNTAX_ERROR, cause.getMessage());
			} else {
				report.report(Problem.IO_ERROR, ex.getMessage());
			}
			return null;
		}
	}
}
