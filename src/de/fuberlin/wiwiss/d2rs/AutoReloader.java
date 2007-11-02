package de.fuberlin.wiwiss.d2rs;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.fuberlin.wiwiss.d2rq.D2RQException;

public class AutoReloader extends GraphBase {
	private static Log log = LogFactory.getLog(AutoReloader.class);
	private static long CHECK_FREQUENCY_MS = 1000;
	
	private final D2RServer server;
	private Graph base = null;
	private PrefixMappingImpl prefixes = new PrefixMappingImpl();
	private File mappingFile;
	private long lastModified = Long.MAX_VALUE;
	private long previousCheck = Long.MIN_VALUE;
	private NamespacePrefixModel prefixModel = null;
	
	public AutoReloader(File mappingFile, D2RServer server) {
		this.mappingFile = mappingFile;
		this.server = server;
	}
	
	public void setPrefixModel(NamespacePrefixModel m) {
		this.prefixModel = m;
	}
	
	public void forceReload() {
		reloadMappingFile();
	}
	
	protected ExtendedIterator graphBaseFind(TripleMatch m) {
		checkMappingFileChanged();
		return this.base.find(m);
	}
	
	public Capabilities getCapabilities() {
		return this.base.getCapabilities();
	}

	public PrefixMapping getPrefixMapping() {
		return this.prefixes;
	}

	public QueryHandler queryHandler() {
		checkMappingFileChanged();
		return this.base.queryHandler();
	}
	
	public void checkMappingFileChanged() {
		if (this.mappingFile == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (this.previousCheck + CHECK_FREQUENCY_MS > now) {
			return;
		}
		long lastmod = this.mappingFile.lastModified();
		if (lastmod == this.lastModified) {
			return;
		}
		this.lastModified = lastmod;
		reloadMappingFile();
	}
	
	private void reloadMappingFile() {
		if (this.base != null) {
			log.info("Reloading mapping file");
		}
		try {
			Model model = server.reloadModelD2RQ(this.mappingFile.toURL().toString());
			setNewBase(model.getGraph());
			this.prefixModel.update(model);
		} catch (MalformedURLException ex) {
			throw new D2RQException("No URL: " + this.mappingFile, ex);
		}
	}
	
	private void setNewBase(Graph newBase) {
		this.base = newBase;
		this.prefixes.setNsPrefixes(newBase.getPrefixMapping());
	}
}
