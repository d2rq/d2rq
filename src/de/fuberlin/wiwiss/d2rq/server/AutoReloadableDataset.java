package de.fuberlin.wiwiss.d2rq.server;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.LabelExistsException;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import com.hp.hpl.jena.util.iterator.NullIterator;

import de.fuberlin.wiwiss.d2rq.SystemLoader;
import de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.map.Database;

public class AutoReloadableDataset implements Dataset {
	private static Log log = LogFactory.getLog(AutoReloadableDataset.class);
	
	/** only reload any this mili seconds */
	private static long RELOAD_FREQUENCY_MS = 1000;

	private final SystemLoader loader;
	private final File watchedFile;
	private final boolean autoReload;
	
	private DatasetGraph datasetGraph = null;
    
	private long lastModified = Long.MAX_VALUE;
	private long lastReload = Long.MIN_VALUE;
	
	/** true if resultSizeLimit is used */
	private boolean hasTruncatedResults;
	
	private Model defaultModel;
	
	public AutoReloadableDataset(SystemLoader loader, String watchedFile, boolean autoReload) {
		this.loader = loader;
		this.watchedFile = watchedFile == null ? null : new File(watchedFile);
		this.autoReload = autoReload;
		reload();
	}

	/** re-init dsg if mapping file has changed */
	public void checkMappingFileChanged() {
		if (!autoReload) return;
		
		// only reload again if lastReload is older than CHECK_FREQUENCY_MS
		long now = System.currentTimeMillis();
		if (now < this.lastReload + RELOAD_FREQUENCY_MS) return;
		
		if (watchedFile.lastModified() == this.lastModified) return;
		
		log.info("Reloading mapping file");
		datasetGraph.close();
		loader.resetMappingFile();
		reload();
	}
	
	private void reload() {
		loader.getMapping().connect();
		GraphD2RQ graph = loader.getGraphD2RQ();
		
		datasetGraph = DatasetGraphFactory.createOneGraph(graph);
		defaultModel = ModelFactory.createModelForGraph(datasetGraph.getDefaultGraph());		

		hasTruncatedResults = false;
		for (Database db: loader.getMapping().databases()) {
			if (db.getResultSizeLimit() != Database.NO_LIMIT) {
				hasTruncatedResults = true;
			}
		}

		if (autoReload) {
			lastModified = watchedFile.lastModified();
			lastReload = System.currentTimeMillis();
		}
	}

	public PrefixMapping getPrefixMapping() {
		//checkMappingFileChanged();
		return this.datasetGraph.getDefaultGraph().getPrefixMapping();
	}

	public boolean hasTruncatedResults() {
		//checkMappingFileChanged();
		return hasTruncatedResults;
	}

	public DatasetGraph asDatasetGraph() {
		// check already done by servlets before getting the graph
		//checkMappingFileChanged();
		return datasetGraph;
	}

	public Model getDefaultModel() {
		// check already done earlier, don't care
		//checkMappingFileChanged();
		return defaultModel;
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

	public Iterator<String> listNames() {
		return NullIterator.instance();
	}

	public void close() {
		datasetGraph.close();
	}

	public void setDefaultModel(Model model) {
		throw new UnsupportedOperationException("Read-only dataset");
	}

	public void addNamedModel(String uri, Model model)
			throws LabelExistsException {
		throw new UnsupportedOperationException("Read-only dataset");
	}

	public void removeNamedModel(String uri) {
		throw new UnsupportedOperationException("Read-only dataset");
	}

	public void replaceNamedModel(String uri, Model model) {
		throw new UnsupportedOperationException("Read-only dataset");
	}

	public boolean supportsTransactions() {
		return false;
	}

	public void begin(ReadWrite readWrite) {
		throw new UnsupportedOperationException("Read-only dataset");
	}

	public void commit() {
		throw new UnsupportedOperationException("Read-only dataset");
	}

	public void abort() {
		throw new UnsupportedOperationException("Read-only dataset");
	}

	public boolean isInTransaction() {
		return false;
	}

	public void end() {
		throw new UnsupportedOperationException("Read-only dataset");
	}
}
