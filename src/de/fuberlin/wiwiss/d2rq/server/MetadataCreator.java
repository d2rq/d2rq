package de.fuberlin.wiwiss.d2rq.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import de.fuberlin.wiwiss.d2rq.vocab.META;

/**
 * Implements a metadata extension. For more information see <a href=
 * "http://sourceforge.net/apps/mediawiki/trdf/index.php?title=D2R_Server_Metadata_Extension"
 * >http://sourceforge.net/apps/mediawiki/trdf/index.php?title=
 * D2R_Server_Metadata_Extension</a>.
 * 
 * @author Hannes Muehleisen (hannes@muehleisen.org)
 */

public class MetadataCreator {

	private final static String metadataPlaceholderURIPrefix = "about:metadata:";
	// model used to generate nodes
	private Model model;
	// local d2r instance
	private D2RServer server;

	// enabling / disabling flag
	private boolean enable = true;
	// model that holds the rdf template
	private Model tplModel;

	private static final Log log = LogFactory.getLog(MetadataCreator.class);

	public MetadataCreator(D2RServer server) {
		// store D2R server config for template location
		this.server = server;
		model = ModelFactory.createDefaultModel();
		// load template into a model

		try {
			Resource config = server.getConfig().findServerResource();
			String metadataTemplate = config.getProperty(
					D2RConfig.metadataTemplate).getString();

			String templatePath;
			if (metadataTemplate.startsWith(File.separator)) {
				templatePath = metadataTemplate;
			} else {
				File mappingFile = new File(server.getConfig()
						.getLocalMappingFilename());
				String folder = mappingFile.getParent();
				if (folder != null) {
					templatePath = folder + File.separator + metadataTemplate;
				} else {
					templatePath = metadataTemplate;
				}

			}
			File f = new File(templatePath);
			if (f.exists()) {
				log.info("Metadata extension using template file '"
						+ templatePath + "'");
				// add metadata from templates to model
				tplModel = ModelFactory.createDefaultModel();
				try {
					tplModel = FileManager.get().loadModel(templatePath);
					enable = true;
				} catch (JenaException e) {
					log.warn("Unable to load the metadata template file at '"
							+ templatePath + "'");
				}
			} else {
				log.warn("Metadata extension enabled with template path '"
						+ templatePath + "', but file is missing.");
				enable = false;
			}

		} catch (Exception e) {
			// if something goes wrong, disable extension
			enable = false;
		}
	}

	public Model addMetadataFromTemplate(String resourceURI, String documentURL) {
		if (!enable) {
			return ModelFactory.createDefaultModel();
		}

		// iterate over template statements to replace placeholders
		Model metadata = ModelFactory.createDefaultModel();
		metadata.setNsPrefixes(tplModel.getNsPrefixMap());

		StmtIterator it = tplModel.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Resource subj = stmt.getSubject();
			com.hp.hpl.jena.rdf.model.Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();

			try {
				if (subj.toString().contains(metadataPlaceholderURIPrefix)) {
					subj = (Resource) parsePlaceholder(subj, documentURL,
							resourceURI);
					if (subj == null) {
						// create a unique blank node with a fixed id.
						subj = model.createResource(new AnonId(String
								.valueOf(stmt.getSubject().hashCode())));
					}
				}

				if (obj.toString().contains(metadataPlaceholderURIPrefix)) {
					obj = parsePlaceholder(obj, documentURL, resourceURI);
				}

				// only add statements with some objects
				if (obj != null) {
					stmt = metadata.createStatement(subj, pred, obj);
					metadata.add(stmt);
				}
			} catch (Exception e) {
				// something went wrong, oops - lets better remove the offending
				// statement
				metadata.remove(stmt);
				log.info("Failed to parse metadata template statement "
						+ stmt.toString());
				e.printStackTrace();
			}
		}

		// remove blank nodes that don't have any properties
		boolean changes = true;
		while (changes) {
			changes = false;
			StmtIterator stmtIt = metadata.listStatements();
			List<Statement> remList = new ArrayList<Statement>();
			while (stmtIt.hasNext()) {
				Statement s = stmtIt.nextStatement();
				if (s.getObject().isAnon()
						&& !((Resource) s.getObject().as(Resource.class))
								.listProperties().hasNext()) {
					remList.add(s);
					changes = true;
				}
			}
			metadata.remove(remList);
		}
		// log.info(metadata.listStatements().toList());
		return metadata;
	}

	private RDFNode parsePlaceholder(RDFNode phRes, String documentURL,
			String resourceURI) {
		String phURI = phRes.asNode().getURI();
		// get package name and placeholder name from placeholder URI
		phURI = phURI.replace(metadataPlaceholderURIPrefix, "");
		String phPackage = phURI.substring(0, phURI.indexOf(":") + 1);
		String phName = phURI.replace(phPackage, "");
		phPackage = phPackage.replace(":", "");
		Resource serverConfig = server.getConfig().findServerResource();

		if (phPackage.equals("runtime")) {
			// <about:metadata:runtime:time> - the current time
			if (phName.equals("time")) {
				return model.createTypedLiteral(Calendar.getInstance());
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

		// <about:metadata:server:*> - The d2r server configuration parameters
		if (phPackage.equals("config") || phPackage.equals("server")) {
			// look for requested property in the dataset config
			Property p = model.createProperty(D2RConfig.NS + phName);
			if (serverConfig.hasProperty(p)) {
				return serverConfig.getProperty(p).getObject();
			}
		}

		// <about:metadata:database:*> - The d2rq database configuration
		// parameters
		Resource mappingConfig = server.getConfig().findDatabaseResource();
		if (phPackage.equals("database")) {
			Property p = model.createProperty(D2RQ.NS + phName);
			if (mappingConfig.hasProperty(p)) {
				return mappingConfig.getProperty(p).getObject();
			}
		}

		if (phPackage.equals("root")) {
			return model.createResource(META.ROOT);
		}

		// <about:metadata:metadata:*> - The metadata provided by users
		if (phPackage.equals("metadata")) {
			// look for requested property in the dataset config
			Property p = model.createProperty(META.NS + phName);
			if (serverConfig.hasProperty(p))
				return serverConfig.getProperty(p).getObject();
		}

		return model
				.createResource(new AnonId(String.valueOf(phRes.hashCode())));
	}
}
