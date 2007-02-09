package de.fuberlin.wiwiss.d2rs;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
import com.hp.hpl.jena.query.describe.DescribeHandlerRegistry;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

/**
 * A D2R Server instance. Sets up a service, loads the D2RQ model, and starts Joseki.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RServer.java,v 1.12 2007/02/09 11:22:53 cyganiak Exp $
 */
public class D2RServer {
	private static D2RServer instance = null;
	private static String SPARQLServiceName = "sparql";
	private static String resourceServiceName = "resource";
	private static String defaultBaseURI = "http://localhost";
	private static int defaultPort = 2020;
	
	public static D2RServer instance() {
		if (D2RServer.instance == null) {
			D2RServer.instance = new D2RServer();
		}
		return D2RServer.instance;
	}

	private ConfigLoader config = null;
	private int port = -1;
	private String baseURI = null;
	private Model model = null;
	private GraphD2RQ currentGraph = null;
	private DataSource dataset;
	private NamespacePrefixModel prefixesModel;
	private AutoReloader reloader = null;
	private Log log = LogFactory.getLog(D2RServer.class);
	
	private D2RServer() {
		// private to enforce singleton
	}
	
	public void setPort(int port) {
		log.info("using port " + port);
		this.port = port;
	}

	public void setBaseURI(String baseURI) {
		if (!baseURI.endsWith("/")) {
			baseURI += "/";
		}
		log.info("using custom base URI: " + baseURI);
		this.baseURI = baseURI;
	}
	
	public void setConfigFile(String configFileURL) {
		log.info("using config file: " + configFileURL);
		this.config = new ConfigLoader(configFileURL);
		this.config.load();
		if (this.config.isLocalMappingFile()) {
			initAutoReloading(this.config.getLocalMappingFilename());
		} else {
			this.model = reloadModelD2RQ(this.config.getMappingURL());
			this.prefixesModel = new NamespacePrefixModel();
			this.prefixesModel.update(this.model);
		}
	}

	public String baseURI() {
		if (this.baseURI != null) {
			return this.baseURI;
		}
		if (this.config.baseURI() != null) {
			return this.config.baseURI();
		}
		if (this.port() == 80) {
			return D2RServer.defaultBaseURI + "/";
		}
		return D2RServer.defaultBaseURI + ":" + this.port() + "/";
	}

	public int port() {
		if (this.port != -1) {
			return this.port;
		}
		if (this.config.port() != -1) {
			return this.config.port();
		}
		return D2RServer.defaultPort;
	}
	
	public String resourceBaseURI() {
		return this.baseURI() + D2RServer.resourceServiceName + "/";
	}
	
	public String graphURLDescribingResource(String resourceURI) {
		if (resourceURI.indexOf(":") == -1) {
			resourceURI = resourceBaseURI() + resourceURI;
		}
		String query = "DESCRIBE <" + resourceURI + ">";
		try {
			return this.baseURI() + D2RServer.SPARQLServiceName + "?query=" + URLEncoder.encode(query, "utf-8");
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

	/**
	 * @return The graph currently in use; will change to a new instance on auto-reload
	 */
	public GraphD2RQ currentGraph() {
		if (this.reloader != null) {
			this.reloader.checkMappingFileChanged();
		}
		return this.currentGraph;
	}
	
	public Dataset dataset() {
		return this.dataset;
	}
	
	public ModelD2RQ reloadModelD2RQ(String mappingFileURL) {
		Model mapModel = ModelFactory.createDefaultModel();
		mapModel.read(mappingFileURL, resourceBaseURI(), "N3");
		ModelD2RQ d2rqModel = new ModelD2RQ(mapModel, resourceBaseURI());
		this.currentGraph = (GraphD2RQ) d2rqModel.getGraph();
		this.currentGraph.connect();
		this.currentGraph.initInventory(baseURI() + "all/");
		return d2rqModel;
	}
	
	private void initAutoReloading(String filename) {
		this.reloader = new AutoReloader(new File(filename));
		this.model = ModelFactory.createModelForGraph(this.reloader);
		DescribeHandlerRegistry.get().clear();
		DescribeHandlerRegistry.get().add(new FindDescribeHandler(this.model));
		this.prefixesModel = new NamespacePrefixModel();		
		this.reloader.setPrefixModel(this.prefixesModel);
		this.dataset = DatasetFactory.create();
		this.dataset.setDefaultModel(this.model);
		this.dataset.addNamedModel(NamespacePrefixModel.namespaceModelURI, this.prefixesModel);
		this.reloader.forceReload();
	}
	
	public void start() {
		Registry.add(RDFServer.ServiceRegistryName, createJosekiServiceRegistry());
		new RDFServer(null, port()).start();
		log.info("[[[ Server started at " + baseURI() + " ]]]");
	}
	
	protected ServiceRegistry createJosekiServiceRegistry() {
		ServiceRegistry services = new ServiceRegistry();
		Service service = createJosekiService();
		services.add(D2RServer.SPARQLServiceName, service);
		return services;
	}
	
	protected Service createJosekiService() {
		return new Service(new SPARQL(), D2RServer.SPARQLServiceName,
				new D2RQDatasetDesc(this.dataset));
	}
	
	protected void checkIfModelWorks() {
		log.info("verifying mapping file ...");
		this.model.isEmpty();
		log.info("--------------------");
	}
}
