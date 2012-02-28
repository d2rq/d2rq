package de.fuberlin.wiwiss.d2rq.server;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joseki.RDFServer;
import org.joseki.Registry;
import org.joseki.Service;
import org.joseki.ServiceRegistry;
import org.joseki.processors.SPARQL;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandler;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandlerFactory;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandlerRegistry;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

/**
 * A D2R Server instance. Sets up a service, loads the D2RQ model, and starts Joseki.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RServer.java,v 1.26 2009/08/02 09:12:06 fatorange Exp $
 */
public class D2RServer {
	private final static String SPARQL_SERVICE_NAME = "sparql";
	
	/* These service names should match the mappings in web.xml */
	private final static String RESOURCE_SERVICE_NAME = "resource";
	private final static String DATA_SERVICE_NAME = "data";
	private final static String PAGE_SERVICE_NAME = "page";
	private final static String VOCABULARY_STEM = "vocab/";
	
	private final static String DEFAULT_BASE_URI = "http://localhost";
	private final static String DEFAULT_SERVER_NAME = "D2R Server";
	private final static String SERVER_INSTANCE = "D2RServer.SERVER_INSTANCE";
	private static final Log log = LogFactory.getLog(D2RServer.class);
	
	/** d2rq mapping file */
	private String configFile;
	
	/** config file parser and Java representation */
	private ConfigLoader config = null;
	
	/** server port from command line, overrides port in config file */
	private int overridePort = -1;
	
	/** base URI from command line */
	private String overrideBaseURI = null;
	
	/** base URI from command line */
	private boolean overrideUseAllOptimizations = false;

	/** the dataset, auto-reloadable in case of local mapping files */
	private AutoReloadableDataset dataset;

	
	public void putIntoServletContext(ServletContext context) {
		context.setAttribute(SERVER_INSTANCE, this);
	}
	
	public static D2RServer fromServletContext(ServletContext context) {
		return (D2RServer) context.getAttribute(SERVER_INSTANCE);
	}
	
	public void overridePort(int port) {
		log.info("using port " + port);
		this.overridePort = port;
	}

	public void overrideBaseURI(String baseURI) {

		// This is a hack to allow hash URIs to be used at least in the
		// SPARQL endpoint. It will not work in the Web interface.
		if (!baseURI.endsWith("/") && !baseURI.endsWith("#")) {
			baseURI += "/";
		}
		if (baseURI.indexOf('#') != -1) {
			log.warn("Base URIs containing '#' may not work correctly!");
		}

		log.info("using custom base URI: " + baseURI);
		this.overrideBaseURI = baseURI;
	}
	
	public void overrideUseAllOptimizations(boolean overrideAllOptimizations) {
		this.overrideUseAllOptimizations = overrideAllOptimizations;
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
		return dataset.hasTruncatedResults();
	}
	
	public String resourceBaseURI(String serviceStem) {
		// This is a hack to allow hash URIs to be used at least in the
		// SPARQL endpoint. It will not work in the Web interface.
		if (this.baseURI().endsWith("#")) {
			return this.baseURI();
		}
		return this.baseURI() + serviceStem + D2RServer.RESOURCE_SERVICE_NAME + "/";
	}
	
	public String resourceBaseURI() {
		return resourceBaseURI("");
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
	
	public static String getResourceServiceName() {
		return RESOURCE_SERVICE_NAME;
	}

	public static String getDataServiceName() {
		return DATA_SERVICE_NAME;
	}
	
	public static String getPageServiceName() {
		return PAGE_SERVICE_NAME;
	}
	
	public String dataURL(String serviceStem, String relativeResourceURI) {
		return this.baseURI() + serviceStem + DATA_SERVICE_NAME + "/" + relativeResourceURI;
	}
	
	public String pageURL(String serviceStem, String relativeResourceURI) {
		return this.baseURI() + serviceStem + PAGE_SERVICE_NAME + "/" + relativeResourceURI;
	}
	
	public boolean isVocabularyResource(Resource r) {
		return r.getURI().startsWith(resourceBaseURI(VOCABULARY_STEM));
	}

	public void addDocumentMetadata(Model document, Resource documentResource) {
		this.config.addDocumentMetadata(document, documentResource);
	}

	/**
	 * @return the auto-reloadable dataset which contains a GraphD2RQ as its default graph, no named graphs
	 */
	public AutoReloadableDataset dataset() {
		return this.dataset;
	}

	/**
	 * @return The graph currently in use; will change to a new instance on auto-reload
	 */
	public GraphD2RQ currentGraph() {
		return (GraphD2RQ) this.dataset.asDatasetGraph().getDefaultGraph();
	}
	
	/**
	 * delegate to auto-reloadable dataset, will reload if necessary
	 */
	public void checkMappingFileChanged() {
		dataset.checkMappingFileChanged();
	}
	
	/** 
	 * delegate to auto-reloadable dataset	 * 
	 * @return prefix mappings for the d2rq base graph
	 */
	public PrefixMapping getPrefixes() {
		return dataset.getPrefixMapping();
	}
	
	public void start() {
		log.info("using config file: " + configFile);
		this.config = new ConfigLoader(configFile);
		this.config.load();
		
		if (config.isLocalMappingFile())
			this.dataset = new AutoReloadableDataset(config.getLocalMappingFilename(), true, this);
		else
			this.dataset = new AutoReloadableDataset(config.getMappingURL(), false, this);
		this.dataset.forceReload();
		
		if (this.overrideUseAllOptimizations)
			currentGraph().getConfiguration().setUseAllOptimizations(true);
		
		if (currentGraph().getConfiguration().getUseAllOptimizations()) {
			log.info("Fast mode (all optimizations)");
		} else {
			log.info("Safe mode (launch using --fast to use all optimizations)");
		}
		
		DescribeHandlerRegistry.get().clear();
		DescribeHandlerRegistry.get().add(new FindDescribeHandlerFactory());

		Registry.add(RDFServer.ServiceRegistryName, createJosekiServiceRegistry());
	}
	
	public void shutdown()
	{
		log.info("shutting down");
		
		currentGraph().close();
	}
	
	protected ServiceRegistry createJosekiServiceRegistry() {
		ServiceRegistry services = new ServiceRegistry();
		Service service = new Service(new SPARQL(),
				D2RServer.SPARQL_SERVICE_NAME,
				new D2RQDatasetDesc(this.dataset));
		services.add(D2RServer.SPARQL_SERVICE_NAME, service);
		return services;
	}
	
	private class FindDescribeHandlerFactory implements DescribeHandlerFactory {
		
		public DescribeHandler create() {
			return new FindDescribeHandler(D2RServer.this);
		}
	}
	
	public ConfigLoader getConfig() {
		return config;
	}
}
