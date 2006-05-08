package de.fuberlin.wiwiss.d2rs;

import java.util.Map;

import org.joseki.DatasetDesc;

import com.hp.hpl.jena.query.DataSource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

public class D2RQDatasetDesc extends DatasetDesc {
	private ModelD2RQ modelD2RQ;
	private DataSource dataset;
	
	public D2RQDatasetDesc(ModelD2RQ modelD2RQ) {
		super(null);
		this.modelD2RQ = modelD2RQ;
		this.dataset = DatasetFactory.create();
		this.dataset.setDefaultModel(this.modelD2RQ);
	}

	public Dataset getDataset() {
		return this.dataset;
	}

	public void clearDataset() {
		this.dataset = null;
		this.modelD2RQ = null;
	}

	public void freeDataset() {
		this.dataset = null;
		this.modelD2RQ = null;
	}

	public void setDefaultGraph(Resource dftGraph) {
		throw new RuntimeException("D2RQDatasetDecl.setDefaultGraph is not implemented");
	}
	
	public Resource getDefaultGraph() {
		throw new RuntimeException("D2RQDatasetDecl.getDefaultGraph is not implemented");
	}

	public void addNamedGraph(String uri, Resource r) {
		throw new RuntimeException("D2RQDatasetDecl.addNamedGraph is not implemented");
	}

	public Map getNamedGraphs() {
		throw new RuntimeException("D2RQDatasetDecl.getNamedGraphs is not implemented");
	}

	public String toString() {
		return "D2RQDatasetDecl(" + this.modelD2RQ + ")";
	}
}
