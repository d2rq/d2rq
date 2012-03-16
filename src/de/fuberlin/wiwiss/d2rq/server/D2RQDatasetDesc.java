package de.fuberlin.wiwiss.d2rq.server;

import org.joseki.DatasetDesc;
import org.joseki.Request;
import org.joseki.Response;

import com.hp.hpl.jena.query.Dataset;

/**
 * A Joseki dataset description that returns a dataset
 * consisting only of the one ModelD2RQ passed in. We need
 * this because regular Joseki dataset descriptions are
 * initialized from a configuration Model, and we want
 * to initialize programmatically.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class D2RQDatasetDesc extends DatasetDesc {
	private AutoReloadableDataset dataset;
	
	public D2RQDatasetDesc(AutoReloadableDataset dataset) {
		super(null);
		this.dataset = dataset;
	}

	@Override
	public Dataset acquireDataset(Request request, Response response) {
		dataset.checkMappingFileChanged();
		return this.dataset;
	}

	@Override
    public void returnDataset(Dataset ds) {
		// do nothing
	}

	public String toString() {
		return "D2RQDatasetDesc(" + this.dataset + ")";
	}
}
