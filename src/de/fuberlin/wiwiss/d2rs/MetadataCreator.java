package de.fuberlin.wiwiss.d2rs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.d2rs.vocab.D2R;
import de.fuberlin.wiwiss.d2rs.vocab.META;

/**
 * Implements a metadata extension.
 * For more information see
 * <a href="http://sourceforge.net/apps/mediawiki/trdf/index.php?title=D2R_Server_Metadata_Extension">http://sourceforge.net/apps/mediawiki/trdf/index.php?title=D2R_Server_Metadata_Extension</a>.
 * 
 * @author Hannes Muehleisen (hannes@muehleisen.org)
 */

public class MetadataCreator {
	
	private final static String metadataPlaceholderURIPrefix = "about:metadata:";	
	private Object currentTime;
	private String metadataTemplate;
	private Model model;
	private Resource config;
	private String templatePath;
	
	// additional template variables
	private String resourceURI;
	private String documentURL;
	private String sparqlQuery;
	
	// enabling / disabling flag
	private boolean enable = true;
	
	private static final Log log = LogFactory.getLog(MetadataCreator.class);

	
	public MetadataCreator(D2RServer newConfig, VelocityWrapper newWrapper) {
		// store D2R server config for template location
		// load template into a model
		model = ModelFactory.createDefaultModel();

		try {
			config = newConfig.getConfig().findServerResource();
			metadataTemplate = config.getProperty(D2R.metadataTemplate).getString();
			templatePath = (String) newWrapper.getEngine().getProperty("file.resource.loader.path");
			
		} catch (Exception e) {
			// if no valid template is found, disable extension
			enable = false;
		}
	}

	public Model addMetadataFromTemplate(Resource documentResource, ServletContext context) {		
		if (!enable) return model;
		currentTime = Calendar.getInstance();
		
		// add metadata from templates
		Model tplModel = ModelFactory.createDefaultModel();
		String tplPath = context.getContextPath() + templatePath + "/" + metadataTemplate;
		
		tplModel = FileManager.get().loadModel(tplPath);
				
		// iterate over template statements to replace placeholders
		Model metadata = ModelFactory.createDefaultModel();
		StmtIterator it = tplModel.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Resource subj = stmt.getSubject();
			com.hp.hpl.jena.rdf.model.Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();
			
			try {
				if (subj.toString().contains(metadataPlaceholderURIPrefix)){
					subj = (Resource) parsePlaceholder(subj, documentResource, context);
					if (subj == null) {
						// create a unique blank node with a fixed id.
						subj = model.createResource(new AnonId(String.valueOf(stmt.getSubject().hashCode())));
					}
				}
				
				if (obj.toString().contains(metadataPlaceholderURIPrefix)){
					obj = parsePlaceholder(obj, documentResource, context);
				}
				
				// only add statements with some objects
				if (obj != null) {
					stmt = metadata.createStatement(subj,pred,obj);
					metadata.add(stmt);
				}
			} catch (Exception e) {
				// something went wrong, oops - lets better remove the offending statement
				metadata.remove(stmt);
				log.info("Failed to parse metadata template statement " + stmt.toString());
				e.printStackTrace();
			}
		}
		
		// remove blank nodes that don't have any properties
		boolean changes = true;
		while ( changes ) {
			changes = false;
			StmtIterator stmtIt = metadata.listStatements();
			List remList = new ArrayList();
			while (stmtIt.hasNext()) {
				Statement s = stmtIt.nextStatement();
				if (    s.getObject().isAnon()
				     && ! ((Resource) s.getObject().as(Resource.class)).listProperties().hasNext() ) {
					remList.add(s);
					changes = true;
				}
			}
			metadata.remove(remList);
		}

		return metadata;
	}
	
	private RDFNode parsePlaceholder(RDFNode phRes, Resource documentResource, ServletContext context) {
		String phURI = phRes.asNode().getURI();
		// get package name and placeholder name from placeholder URI
		phURI = phURI.replace(metadataPlaceholderURIPrefix, "");
		String phPackage = phURI.substring(0, phURI.indexOf(":")+1);
		String phName = phURI.replace(phPackage, "");
		phPackage = phPackage.replace(":", "");
		
		if (phPackage.equals("runtime")) {
			// <about:metadata:runtime:query> - the SPARQL Query used to get the RDF Graph			
			if (phName.equals("query")) {
				return model.createTypedLiteral(sparqlQuery);
			}
			// <about:metadata:runtime:time> - the current time
			if (phName.equals("time")) {
				return model.createTypedLiteral(currentTime);
			}
			// <about:metadata:runtime:graph> - URI of the graph
			if (phName.equals("graph")) {
				return model.createResource(documentURL);
			}
			// <about:metadata:runtime:resource> - URI of the resource
			if (phName.equals("resource")) {
				return model.createResource(resourceURI);
			}
		}
		
		// <about:metadata:config:*> - The configuration parameters
		if (phPackage.equals("config")) {
			// look for requested property in the dataset config
			Property p  = model.createProperty(D2R.NS + phName);
			if (config.hasProperty(p))
				return config.getProperty(p).getObject();
			
		}
		
		// <about:metadata:metadata:*> - The metadata provided by users
		if (phPackage.equals("metadata")) {
			// look for requested property in the dataset config
			Property p  = model.createProperty(META.NS + phName);
			if (config.hasProperty(p))
				return config.getProperty(p).getObject();
		}

		return model.createResource(new AnonId(String.valueOf(phRes.hashCode())));
	}

	public void setResourceURI(String aResourceURI) {
		this.resourceURI = aResourceURI;
	}

	public void setDocumentURL(String aDocumentURL) {
		this.documentURL = aDocumentURL;
	}

	public void setSparqlQuery(String aSparqlQuery) {
		this.sparqlQuery = aSparqlQuery;
	}
	
}
