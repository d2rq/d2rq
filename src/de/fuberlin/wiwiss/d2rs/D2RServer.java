package de.fuberlin.wiwiss.d2rs;

import org.joseki.RDFServer;
import org.joseki.Registry;
import org.joseki.Service;
import org.joseki.ServiceRegistry;
import org.joseki.processors.SPARQL;

import de.fuberlin.wiwiss.d2rq.ModelD2RQ;

public class D2RServer {
	private int port = 2020;
	private String serviceName = "sparql";
	private ModelD2RQ model = null;

	public void setPort(int port) {
		this.port = port;
	}
	
	public void initFromMappingFile(String mappingFileURL) {
		this.model = new ModelD2RQ(mappingFileURL);
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
}
