package de.fuberlin.wiwiss.d2rs;

import java.util.Collection;
import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.describe.DescribeHandler;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A custom {@link DescribeHandler} that returns the results of a two-way
 * find.
 * 
 * TODO Is this thread-safe? ARQ uses just a single instance of this class.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: FindDescribeHandler.java,v 1.2 2006/09/01 08:51:09 cyganiak Exp $
 */
public class FindDescribeHandler implements DescribeHandler {
	private Model dataModel;
	private Model resultModel;
	
	public FindDescribeHandler(Model dataModel) {
		this.dataModel = dataModel;
	}
	
	public void start(Model accumulateResultModel, ExecutionContext qContext) {
		this.resultModel = accumulateResultModel;
		this.resultModel.setNsPrefix("rdfs", RDFS.getURI());
	}
	
	public void describe(Resource resource) {
		try {
		Model description = ModelFactory.createDefaultModel();
		Model seeAlsos = ModelFactory.createDefaultModel();
		description.add(this.dataModel.listStatements(resource, null, (RDFNode) null));
		description.add(this.dataModel.listStatements(null, null, resource));
		ResIterator rit = description.listSubjects();
		while (rit.hasNext()) {
			addSeeAlsoStatement(rit.nextResource(), seeAlsos, resource.getURI());
		}
		NodeIterator nit = description.listObjects();
		while (nit.hasNext()) {
			addSeeAlsoStatement(nit.nextNode(), seeAlsos, resource.getURI());
		}
		Collection classMapNames = D2RServer.instance().currentGraph().classMapNamesForResource(resource.asNode());
		if (!classMapNames.isEmpty()) {
			Resource r2 = seeAlsos.createResource(resource.getURI());
			Iterator it = classMapNames.iterator();
			while (it.hasNext()) {
				String classMapName = (String) it.next();
				r2.addProperty(RDFS.seeAlso, Node.createURI(D2RServer.instance().baseURI() + "all/" + classMapName));
			}
		}
		resultModel.add(description);
		resultModel.add(seeAlsos);
		} catch (RuntimeException ex) {
			ex.printStackTrace(System.out);
			throw ex;
		}
	}

	public void finish() {
		// do nothing
	}

	private void addSeeAlsoStatement(RDFNode n, Model m, String currentResourceURI) {
		if (!n.isURIResource()) {
			return;
		}
		String resourceURI = n.asNode().getURI();
		if (currentResourceURI.equals(resourceURI)) {
			// Don't add seeAlso for the current resource pointing to its own description 
			return;
		}
		if (resourceURI.startsWith(D2RServer.instance().resourceBaseURI())) {
			// Don't add seeAlso for dereferenceable URIs
			return;
		}
		String seeAlsoURI = D2RServer.instance().graphURLDescribingResource(resourceURI);
		Resource nAsResource = m.getResource(resourceURI);
		nAsResource.addProperty(RDFS.seeAlso, m.createResource(seeAlsoURI));
	}
}
