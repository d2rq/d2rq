package org.d2rq.r2rml;

import java.util.HashMap;
import java.util.Map;

import org.d2rq.validation.Report;
import org.d2rq.validation.Message.Problem;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;


public class ConflictChecker {
	private final Report report;
	private final Map<Resource,Property> properties = new HashMap<Resource,Property>();
	
	public ConflictChecker(Report report) {
		this.report = report;
	}
	
	public void add(Resource resource, Property property) {
		if (properties.get(resource) == null) {
			properties.put(resource, property);
		}
		if (!properties.get(resource).equals(property)) {
			report.report(Problem.CONFLICTING_PROPERTIES, 
					resource, new Property[]{properties.get(resource), property});
		}
	}
}
