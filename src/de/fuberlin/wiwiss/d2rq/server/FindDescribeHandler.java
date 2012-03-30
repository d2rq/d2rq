package de.fuberlin.wiwiss.d2rq.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandler;
import com.hp.hpl.jena.sparql.util.Context;

/**
 * A custom {@link DescribeHandler} that returns the results of a two-way
 * find.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class FindDescribeHandler implements DescribeHandler {
	private static final Log log = LogFactory.getLog(FindDescribeHandler.class);
	
	private final D2RServer server;
	private final Model d2rqModel;
	private Model resultModel;
	
	public FindDescribeHandler(D2RServer server) {
		this.server = server;
		this.d2rqModel = server.dataset().getDefaultModel();
	}
	
	public void start(Model accumulateResultModel, Context qContext) {
		resultModel = accumulateResultModel;
	}
	
	public void describe(Resource resource) {
		log.info("DESCRIBE <" + resource + ">");
		try {
			resultModel.add(d2rqModel.listStatements(resource, null, (RDFNode) null));
			if (!server.isVocabularyResource(resource)) {
				resultModel.add(d2rqModel.listStatements(null, null, resource));
			} else if (server.getConfig().getVocabularyIncludeInstances()) {
				resultModel.add(d2rqModel.listStatements(null, null, resource));
				resultModel.add(d2rqModel.listStatements(null, (Property) resource.as(Property.class), (RDFNode) null));
			}
		} catch (RuntimeException ex) {
			log.debug("Caught and re-threw RuntimeException", ex);
			throw ex;
		}
	}

	public void finish() {
		// do nothing
	}
}
