package de.fuberlin.wiwiss.d2rs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joseki.RDFServer;
import org.joseki.Registry;
import org.joseki.Service;
import org.joseki.ServiceRegistry;
import org.joseki.processors.SPARQL;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

/**
 * A D2R Server instance. Sets up a service, loads the D2RQ model, and starts Joseki.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RServer.java,v 1.2 2006/05/16 22:30:55 cyganiak Exp $
 */
public class D2RServer {
	private static Log log = LogFactory.getLog(D2RServer.class);
	
	private int port = 2020;
	private String serviceName = "sparql";
	private ModelD2RQ model = null;

	public void setPort(int port) {
		log.info("using port " + port);
		this.port = port;
	}
	
	public void initFromMappingFile(String mappingFileURL) {
		log.info("using mapping file: " + mappingFileURL);
		this.model = new ModelD2RQ(mappingFileURL);
		checkIfModelWorks();
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
		return new Service(new SPARQL(), this.serviceName, new D2RQDatasetDesc(this.model));
	}
	
	protected void checkIfModelWorks() {
		log.info("verifying mapping file ...");
		this.model.isEmpty();
		log.info("--------------------");
	}
}
