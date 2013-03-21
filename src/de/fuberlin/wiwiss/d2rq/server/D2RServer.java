package de.fuberlin.wiwiss.d2rq.server;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joseki.RDFServer;
import org.joseki.Registry;
import org.joseki.Service;
import org.joseki.ServiceRegistry;
import org.joseki.processors.SPARQL;

import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandler;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandlerFactory;
import com.hp.hpl.jena.sparql.core.describe.DescribeHandlerRegistry;
import com.hp.hpl.jena.sparql.util.Context;

import de.fuberlin.wiwiss.d2rq.ResourceDescriber;
import de.fuberlin.wiwiss.d2rq.SystemLoader;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.map.Mapping;

/**
 * A D2R Server instance. Sets up a service, loads the D2RQ model, and starts
 * Joseki.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class D2RServer {
	private final static String SPARQL_SERVICE_NAME = "sparql";

	/* These service names should match the mappings in web.xml */
	private final static String RESOURCE_SERVICE_NAME = "resource";
	private final static String DATASET_SERVICE_NAME = "dataset";
	private final static String DATA_SERVICE_NAME = "data";
	private final static String PAGE_SERVICE_NAME = "page";
	private final static String VOCABULARY_STEM = "vocab/";

	private final static String DEFAULT_SERVER_NAME = "D2R Server";
	private final static String SYSTEM_LOADER = "D2RServer.SYSTEM_LOADER";

	private static final Log log = LogFactory.getLog(D2RServer.class);

	/** System loader for access to the GraphD2RQ and configuration */
	private final SystemLoader loader;

	/** config file parser and Java representation */
	private final ConfigLoader config;

	/** base URI from command line */
	private String overrideBaseURI = null;

	/** the dataset, auto-reloadable in case of local mapping files */
	private AutoReloadableDataset dataset;

	private boolean startupError = false;
	
	public D2RServer(SystemLoader loader) {
		this.loader = loader;
		this.config = loader.getServerConfig();
	}

	public static D2RServer fromServletContext(ServletContext context) {
		return retrieveSystemLoader(context).getD2RServer();
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
		this.overrideBaseURI = baseURI;
	}

	public String baseURI() {
		if (this.overrideBaseURI != null) {
			return this.overrideBaseURI;
		}
		return this.config.baseURI();
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
		return this.baseURI() + serviceStem + D2RServer.RESOURCE_SERVICE_NAME
				+ "/";
	}

	public String resourceBaseURI() {
		return resourceBaseURI("");
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

	public static String getDatasetServiceName() {
		return DATASET_SERVICE_NAME;
	}

	public static String getSparqlServiceName() {
		return SPARQL_SERVICE_NAME;
	}

	public String dataURL(String serviceStem, String relativeResourceURI) {
		return this.baseURI() + serviceStem + DATA_SERVICE_NAME + "/"
				+ relativeResourceURI;
	}

	public String pageURL(String serviceStem, String relativeResourceURI) {
		return this.baseURI() + serviceStem + PAGE_SERVICE_NAME + "/"
				+ relativeResourceURI;
	}

	public boolean isVocabularyResource(Resource r) {
		return r.getURI().startsWith(resourceBaseURI(VOCABULARY_STEM));
	}

	public void addDocumentMetadata(Model document, Resource documentResource) {
		this.config.addDocumentMetadata(document, documentResource);
	}

	/**
	 * @return the auto-reloadable dataset which contains a GraphD2RQ as its
	 *         default graph, no named graphs
	 */
	public AutoReloadableDataset dataset() {
		return this.dataset;
	}

	public Mapping getMapping() {
		return loader.getMapping();
	}

	/**
	 * delegate to auto-reloadable dataset, will reload if necessary
	 */
	public void checkMappingFileChanged() {
		dataset.checkMappingFileChanged();
	}

	/**
	 * delegate to auto-reloadable dataset *
	 * 
	 * @return prefix mappings for the d2rq base graph
	 */
	public PrefixMapping getPrefixes() {
		return dataset.getPrefixMapping();
	}

	public void start() {
		startupError = true;
		if (config.isLocalMappingFile()) {
			this.dataset = new AutoReloadableDataset(loader,
					config.getLocalMappingFilename(),
					config.getAutoReloadMapping());
		} else {
			this.dataset = new AutoReloadableDataset(loader, null, false);
		}

		if (loader.getMapping().configuration().getUseAllOptimizations()) {
			log.info("Fast mode (all optimizations)");
		} else {
			log.info("Safe mode (launch using --fast to use all optimizations)");
		}

		// Set up a custom DescribeHandler that calls out to
		// {@link ResourceDescriber}
		DescribeHandlerRegistry.get().clear();
		DescribeHandlerRegistry.get().add(new DescribeHandlerFactory() {
			public DescribeHandler create() {
				return new DescribeHandler() {
					private BulkUpdateHandler adder;

					public void start(Model accumulateResultModel,
							Context qContext) {
						adder = accumulateResultModel.getGraph()
								.getBulkUpdateHandler();
					}

					public void describe(Resource resource) {
						log.info("DESCRIBE <" + resource + ">");
						boolean outgoingTriplesOnly = isVocabularyResource(resource)
								&& !getConfig().getVocabularyIncludeInstances();
						adder.add(new ResourceDescriber(getMapping(), resource
								.asNode(), outgoingTriplesOnly,
								Relation.NO_LIMIT, 
								Math.round(config.getSPARQLTimeout() * 1000)).description());
					}

					public void finish() {
					}
				};
			}
		});

		Registry.add(RDFServer.ServiceRegistryName,
				createJosekiServiceRegistry());
		
		if (config.getSPARQLTimeout() > 0) {
			ARQ.getContext().set(ARQ.queryTimeout, config.getSPARQLTimeout() * 1000);
		}
		
		startupError = false;
	}

	public boolean errorOnStartup() {
		return startupError;
	}
	
	public void shutdown() {
		log.info("shutting down");
		loader.getMapping().close();
	}

	protected ServiceRegistry createJosekiServiceRegistry() {
		ServiceRegistry services = new ServiceRegistry();
		Service service = new Service(new SPARQL(),
				D2RServer.SPARQL_SERVICE_NAME,
				new D2RQDatasetDesc(this.dataset));
		services.add(D2RServer.SPARQL_SERVICE_NAME, service);
		return services;
	}

	public ConfigLoader getConfig() {
		return config;
	}

	public static void storeSystemLoader(SystemLoader loader,
			ServletContext context) {
		context.setAttribute(SYSTEM_LOADER, loader);
	}

	public static SystemLoader retrieveSystemLoader(ServletContext context) {
		return (SystemLoader) context.getAttribute(SYSTEM_LOADER);
	}

	private static String getUri(String base, String service) {
		if (base == null) {
			base = "";
		}
		return base.endsWith("/") ? base + service : base + "/" + service;
	}

	public String getDatasetIri() {
		// construct the dataset IRI from the configured base URI and "/dataset"
		// - as in web.xml
		return getUri(baseURI(), D2RServer.getDatasetServiceName());
	}

	public String getSparqlUrl() {
		return getUri(baseURI(), D2RServer.getSparqlServiceName());
	}
	
	public static String getVersion() {
		String version = D2RServer.class.getPackage().getImplementationVersion();
		
		if (version == null) {
			version = "devbuild";
		}
		return version;
	}
}
