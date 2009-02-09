package de.fuberlin.wiwiss.d2rs;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.engine.D2RQDatasetGraph;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

public class AutoReloadableDataset implements Dataset {
	private static Log log = LogFactory.getLog(AutoReloadableDataset.class);
	
	/** only reload any this mili seconds */
	private static long RELOAD_FREQUENCY_MS = 1000;

	private D2RServer server;
	private D2RQDatasetGraph datasetGraph = null;
    
	private String mappingFile;
	private long lastModified = Long.MAX_VALUE;
	private long lastReload = Long.MIN_VALUE;
	
	/** true if resultSizeLimit is used */
	private boolean hasTruncatedResults;
	
	/** (localFile) => auto-reloadable */
	private boolean localFile;
	
	public AutoReloadableDataset(String mappingFile, boolean localFile, D2RServer server) {
		this.mappingFile = mappingFile;
		this.localFile = localFile;	
		this.server = server;
	}

	/** re-init dsg */
	public void forceReload() {
		initD2RQDatasetGraph();		
	}
	
	/** re-init dsg if mapping file has changed */
	public void checkMappingFileChanged() {
		if (!localFile || this.mappingFile == null) return;
		
		// only reload again if lastReload is older than CHECK_FREQUENCY_MS
		long now = System.currentTimeMillis();
		if (now < this.lastReload + RELOAD_FREQUENCY_MS) return;
		
		long lastmod = new File(this.mappingFile).lastModified();
		if (lastmod == this.lastModified)
			return;
		
		initD2RQDatasetGraph();
	}
	
	private void initD2RQDatasetGraph() {
		if (this.datasetGraph != null)
			log.info("Reloading mapping file");
		
		Model mapModel = ModelFactory.createDefaultModel();
		mapModel.read((this.localFile) ? "file:" + this.mappingFile : this.mappingFile, server.resourceBaseURI(), "N3");
		
		this.hasTruncatedResults = mapModel.contains(null, D2RQ.resultSizeLimit, (RDFNode) null);
		ModelD2RQ result = new ModelD2RQ(mapModel, server.resourceBaseURI());
		GraphD2RQ graph = (GraphD2RQ) result.getGraph();
		graph.connect();
		graph.initInventory(server.baseURI() + "all/");
		this.datasetGraph = new D2RQDatasetGraph(graph);
		
		if (localFile) {
			this.lastModified = new File(this.mappingFile).lastModified();
			this.lastReload = System.currentTimeMillis();
		}
	}

	public Capabilities getCapabilities() {
		//checkMappingFileChanged();
		return this.datasetGraph.getDefaultGraph().getCapabilities();
	}

	public PrefixMapping getPrefixMapping() {
		//checkMappingFileChanged();
		return this.datasetGraph.getDefaultGraph().getPrefixMapping();
	}

	public boolean hasTruncatedResults() {
		//checkMappingFileChanged();
		return hasTruncatedResults;
	}

	public QueryHandler queryHandler() {
		checkMappingFileChanged();
		return this.datasetGraph.getDefaultGraph().queryHandler();
	}
	
	public DatasetGraph asDatasetGraph() {
		// check already done by servlets before getting the graph
		//checkMappingFileChanged();
		return datasetGraph;
	}

	public Model getDefaultModel() {
		// check already done earlier, don't care
		//checkMappingFileChanged();
		return ModelFactory.createModelForGraph(datasetGraph.getDefaultGraph());
	}

	public boolean containsNamedModel(String uri) {
		return false;
	}

	public Lock getLock() {
		return datasetGraph.getLock();
	}

	public Model getNamedModel(String uri) {
		return null;
	}

	public Iterator listNames() {
		return NullIterator.instance();
	}

	public void close() {
		datasetGraph.close();
	}
	
}
