package de.fuberlin.wiwiss.d2rq.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
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

import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import de.fuberlin.wiwiss.d2rq.vocab.META;

/**
 * Implements a metadata extension.
 * 
 * @author Hannes Muehleisen (hannes@muehleisen.org)
 */

public class MetadataCreator {

	private final static String metadataPlaceholderURIPrefix = "about:metadata:";
	// model used to generate nodes
	private Model model = ModelFactory.createDefaultModel();;
	// local d2r instance
	private D2RServer server;

	// enabling / disabling flag
	private boolean enable = true;
	// model that holds the rdf template
	private Model tplModel;

	private static final Log log = LogFactory.getLog(MetadataCreator.class);

	public MetadataCreator(D2RServer server, Model template) {
		// store D2R server config for template location
		this.server = server;

		if (template != null && template.size() > 0) {
			this.enable = true;
			this.tplModel = template;
		}
	}

	public Model addMetadataFromTemplate(String resourceURI,
			String documentURL, String pageUrl) {
		if (!enable || tplModel == null) {
			return ModelFactory.createDefaultModel();
		}

		// iterate over template statements to replace placeholders
		Model metadata = ModelFactory.createDefaultModel();
		metadata.setNsPrefixes(tplModel.getNsPrefixMap());

		StmtIterator it = tplModel.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			Resource subj = stmt.getSubject();
			Property pred = stmt.getPredicate();
			RDFNode obj = stmt.getObject();

			try {
				if (subj.toString().contains(metadataPlaceholderURIPrefix)) {
					subj = (Resource) parsePlaceholder(subj, documentURL,
							resourceURI, pageUrl);
					if (subj == null) {
						// create a unique blank node with a fixed id.
						subj = model.createResource(new AnonId(String
								.valueOf(stmt.getSubject().hashCode())));
					}
				}

				if (obj.toString().contains(metadataPlaceholderURIPrefix)) {
					obj = parsePlaceholder(obj, documentURL, resourceURI,
							pageUrl);
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
			String resourceURI, String pageURL) {
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
			// <about:metadata:runtime:page> - URI of the resource
			if (phName.equals("page")) {
				return model.createResource(pageURL);
			}
			// <about:metadata:runtime:dataset> - URI of the resource
			if (phName.equals("dataset")) {
				return model.createResource(server.getDatasetIri());
			}
			// <about:metadata:runtime:version> - D2R Version
			if (phName.equals("version")) {
				return model.createTypedLiteral(D2RServer.getVersion());
			}
		}

		// <about:metadata:server:*> - The d2r server configuration parameters
		if (phPackage.equals("config") || phPackage.equals("server")) {
			// look for requested property in the dataset config
			Property p = model.createProperty(D2RConfig.NS + phName);
			if (serverConfig != null && serverConfig.hasProperty(p)) {
				return serverConfig.getProperty(p).getObject();
			}
		}

		// <about:metadata:database:*> - The d2rq database configuration
		// parameters
		Resource mappingConfig = server.getConfig().findDatabaseResource();
		if (phPackage.equals("database")) {
			Property p = model.createProperty(D2RQ.NS + phName);
			if (mappingConfig != null && mappingConfig.hasProperty(p)) {
				return mappingConfig.getProperty(p).getObject();
			}
		}

		// <about:metadata:metadata:*> - The metadata provided by users
		if (phPackage.equals("metadata")) {
			// look for requested property in the dataset config
			Property p = model.createProperty(META.NS + phName);
			if (serverConfig != null && serverConfig.hasProperty(p))
				return serverConfig.getProperty(p).getObject();
		}

		return model
				.createResource(new AnonId(String.valueOf(phRes.hashCode())));
	}

	public static File findTemplateFile(D2RServer server,
			Property fileConfigurationProperty) {
		Resource config = server.getConfig().findServerResource();
		if (config == null || !config.hasProperty(fileConfigurationProperty)) {
			return null;
		}
		String metadataTemplate = config.getProperty(fileConfigurationProperty)
				.getString();

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
		return f;
	}

	public static Model loadTemplateFile(File f) {
		try {
			return loadMetadataTemplate(new FileInputStream(f));
		} catch (Exception e) {
			return null;
		}
	}

	public static Model loadMetadataTemplate(InputStream is) {
		try {
			Model tplModel = ModelFactory.createDefaultModel();
			tplModel.read(is, "about:prefix:", "TTL");
			return tplModel;
		} catch (JenaException e) {
			// ignore
		}
		return null;
	}

	public static Comparator<Statement> subjectSorter = new Comparator<Statement>() {
		public int compare(Statement o1, Statement o2) {
			return o1.getPredicate().toString()
					.compareTo(o2.getPredicate().toString());
		}
	};
}
