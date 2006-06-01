package de.fuberlin.wiwiss.d2rs;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joseki.RDFServer;
import org.joseki.Registry;
import org.joseki.Service;
import org.joseki.ServiceRegistry;
import org.joseki.processors.SPARQL;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

/**
 * A D2R Server instance. Sets up a service, loads the D2RQ model, and starts Joseki.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RServer.java,v 1.4 2006/06/01 06:51:02 cyganiak Exp $
 */
public class D2RServer {
	private static Log log = LogFactory.getLog(D2RServer.class);
	
	private int port = 2020;
	private String serviceName = "sparql";
	private Model model = null;
	private NamespacePrefixModel prefixesModel;
	
	public void setPort(int port) {
		log.info("using port " + port);
		this.port = port;
	}
	
	public void initFromMappingFile(String mappingFileURL) {
		log.info("using mapping file: " + mappingFileURL);
		if (mappingFileURL.startsWith("file://")) {
			initAutoReloading(mappingFileURL.substring(7));
			return;
		}
		if (mappingFileURL.startsWith("file:")) {
			initAutoReloading(mappingFileURL.substring(5));
			return;
		}
		if (mappingFileURL.indexOf(":") == -1) {
			initAutoReloading(mappingFileURL);
			return;
		}
		this.model = new ModelD2RQ(mappingFileURL);
		this.prefixesModel = new NamespacePrefixModel();
		this.prefixesModel.update(this.model);
	}

	private void initAutoReloading(String filename) {
		AutoReloader reloader = new AutoReloader(new File(filename));
		this.model = ModelFactory.createModelForGraph(reloader);
		this.prefixesModel = new NamespacePrefixModel();		
		reloader.setPrefixModel(this.prefixesModel);
		reloader.forceReload();
	}
	
	public void start() {
		Registry.add(RDFServer.ServiceRegistryName, createJosekiServiceRegistry());
		new RDFServer(null, this.port).start();
	}
	
	protected ServiceRegistry createJosekiServiceRegistry() {
		ServiceRegistry services = new ServiceRegistry();
		Service service = createJosekiService();
		services.add(this.serviceName, service);
		return services;
	}
	
	protected Service createJosekiService() {
		return new Service(new SPARQL(), this.serviceName, new D2RQDatasetDesc(this.model, this.prefixesModel));
	}
	
	protected void checkIfModelWorks() {
		log.info("verifying mapping file ...");
		this.model.isEmpty();
		log.info("--------------------");
	}
}
