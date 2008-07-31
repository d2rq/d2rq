package de.fuberlin.wiwiss.d2rs;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joseki.RDFServer;
import org.joseki.Registry;
import org.joseki.Service;
import org.joseki.ServiceRegistry;
import org.joseki.processors.SPARQL;

import com.hp.hpl.jena.query.DataSource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandler;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandlerFactory;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandlerRegistry;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import de.fuberlin.wiwiss.d2rs.vocab.D2R;

/**
 * A D2R Server instance. Sets up a service, loads the D2RQ model, and starts Joseki.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RServer.java,v 1.20 2008/07/31 11:21:33 cyganiak Exp $
 */
public class D2RServer {
	private final static String SPARQL_SERVICE_NAME = "sparql";
	private final static String RESOURCE_SERVICE_NAME = "resource";
	private final static String DEFAULT_BASE_URI = "http://localhost";
	private final static String DEFAULT_SERVER_NAME = "D2R Server";
	private final static String SERVER_INSTANCE = "D2RServer.SERVER_INSTANCE";
	
	public static D2RServer fromServletContext(ServletContext context) {
		return (D2RServer) context.getAttribute(SERVER_INSTANCE);
	}
	
	private ConfigLoader config = null;
	private int overridePort = -1;
	private String overrideBaseURI = null;
	private String configFile;
	private boolean hasTruncatedResults;
	private Model model = null;
	private GraphD2RQ currentGraph = null;
	private DataSource dataset;
	private PrefixMapping prefixes;
	private AutoReloader reloader = null;
	private Log log = LogFactory.getLog(D2RServer.class);
	
	public void putIntoServletContext(ServletContext context) {
		context.setAttribute(SERVER_INSTANCE, this);
	}
	
	public void overridePort(int port) {
		log.info("using port " + port);
		this.overridePort = port;
	}

	public void overrideBaseURI(String baseURI) {
		if (!baseURI.endsWith("/") && !baseURI.endsWith("#")) {
			baseURI += "/";
		}
		log.info("using custom base URI: " + baseURI);
		if (baseURI.contains("#")) {
			log.warn("Base URIs containing '#' may not work correctly!");
		}
		this.overrideBaseURI = baseURI;
	}
	
	public void setConfigFile(String configFileURL) {
		configFile = configFileURL;
	}
	
	public String baseURI() {
		if (this.overrideBaseURI != null) {
			return this.overrideBaseURI;
		}
		if (this.config.baseURI() != null) {
			return this.config.baseURI();
		}
		if (this.port() == 80) {
			return D2RServer.DEFAULT_BASE_URI + "/";
		}
		return D2RServer.DEFAULT_BASE_URI + ":" + this.port() + "/";
	}

	public int port() {
		if (this.overridePort != -1) {
			return this.overridePort;
		}
		if (this.config.port() != -1) {
			return this.config.port();
		}
		return JettyLauncher.DEFAULT_PORT;
	}
	
	public String serverName() {
		if (this.config.serverName() != null) {
			return this.config.serverName();
		}
		return D2RServer.DEFAULT_SERVER_NAME;
	}
	
	public boolean hasTruncatedResults() {
		return hasTruncatedResults;
	}
	
	public String resourceBaseURI() {
		return this.baseURI() + D2RServer.RESOURCE_SERVICE_NAME + "/";
	}
	
	public String graphURLDescribingResource(String resourceURI) {
		if (resourceURI.indexOf(":") == -1) {
			resourceURI = resourceBaseURI() + resourceURI;
		}
		String query = "DESCRIBE <" + resourceURI + ">";
		try {
			return this.baseURI() + D2RServer.SPARQL_SERVICE_NAME + "?query=" + URLEncoder.encode(query, "utf-8");
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public String dataURL(String relativeResourceURI) {
		return this.baseURI() + "data/" + relativeResourceURI;
	}
	
	public String pageURL(String relativeResourceURI) {
		return this.baseURI() + "page/" + relativeResourceURI;
	}
	
	public Model model() {
		return this.model;
	}

	public void addDocumentMetadata(Model document, Resource documentResource) {
		this.config.addDocumentMetadata(document, documentResource);
	}
	
	/**
	 * @return The graph currently in use; will change to a new instance on auto-reload
	 */
	public GraphD2RQ currentGraph() {
		if (this.reloader != null) {
			this.reloader.checkMappingFileChanged();
		}
		if (this.currentGraph == null) {
			throw new RuntimeException("No config-file configured in web.xml");
		}
		return this.currentGraph;
	}
	
	public Dataset dataset() {
		return this.dataset;
	}
	
	public Model reloadModelD2RQ(String mappingFileURL) {
		Model mapModel = ModelFactory.createDefaultModel();
		mapModel.read(mappingFileURL, resourceBaseURI(), "N3");
		updatePrefixes(mapModel);
		this.hasTruncatedResults = mapModel.contains(null, D2RQ.resultSizeLimit, (RDFNode) null);
		ModelD2RQ result = new ModelD2RQ(mapModel, resourceBaseURI());
		this.currentGraph = (GraphD2RQ) result.getGraph();
		this.currentGraph.connect();
		this.currentGraph.initInventory(baseURI() + "all/");
		return result;
	}

	private void updatePrefixes(PrefixMapping newPrefixes) {
		prefixes = new PrefixMappingImpl();
		Iterator it = newPrefixes.getNsPrefixMap().keySet().iterator();
		while (it.hasNext()) {
			String prefix = (String) it.next();
			String uri = newPrefixes.getNsPrefixURI(prefix);
			if (D2R.NS.equals(uri)) continue;
			prefixes.setNsPrefix(prefix, uri);
		}
	}
	
	private void initAutoReloading(String filename) {
		this.reloader = new AutoReloader(new File(filename), this);
		this.model = ModelFactory.createModelForGraph(this.reloader);
		DescribeHandlerRegistry.get().clear();
		DescribeHandlerRegistry.get().add(new FindDescribeHandlerFactory(this.model));
		this.dataset = DatasetFactory.create();
		this.dataset.setDefaultModel(this.model);
		this.reloader.forceReload();
	}

	public void checkMappingFileChanged() {
		if (reloader == null) return;
		reloader.checkMappingFileChanged();
	}
	
	public PrefixMapping getPrefixes() {
		return prefixes;
	}
	
	public void start() {
		log.info("using config file: " + configFile);
		this.config = new ConfigLoader(configFile);
		this.config.load();
		if (this.config.isLocalMappingFile()) {
			initAutoReloading(this.config.getLocalMappingFilename());
		} else {
			this.model = reloadModelD2RQ(this.config.getMappingURL());
		}
		Registry.add(RDFServer.ServiceRegistryName, createJosekiServiceRegistry());
	}
	
	protected ServiceRegistry createJosekiServiceRegistry() {
		ServiceRegistry services = new ServiceRegistry();
		Service service = createJosekiService();
		services.add(D2RServer.SPARQL_SERVICE_NAME, service);
		return services;
	}
	
	protected Service createJosekiService() {
		return new Service(new SPARQL(), D2RServer.SPARQL_SERVICE_NAME,
				new D2RQDatasetDesc(this.dataset));
	}
	
	protected void checkIfModelWorks() {
		log.info("verifying mapping file ...");
		this.model.isEmpty();
		log.info("--------------------");
	}

	private class FindDescribeHandlerFactory implements DescribeHandlerFactory {
		private final Model dataModel;
		FindDescribeHandlerFactory(Model dataModel) {
			this.dataModel = dataModel;
		}
		public DescribeHandler create() {
			return new FindDescribeHandler(dataModel, D2RServer.this);
		}
	}
}
