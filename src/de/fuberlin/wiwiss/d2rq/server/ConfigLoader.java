package de.fuberlin.wiwiss.d2rq.server;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;
import de.fuberlin.wiwiss.d2rq.vocab.VocabularySummarizer;

public class ConfigLoader {

	public static final int DEFAULT_LIMIT_PER_CLASS_MAP = 50;
	public static final int DEFAULT_LIMIT_PER_PROPERTY_BRIDGE = 50;
	private static final Log log = LogFactory.getLog(ConfigLoader.class);

	/**
	 * Accepts an absolute URI, relative file: URI, or plain file name
	 * (including names with spaces, Windows backslashes etc) and returns an
	 * equivalent full absolute URI.
	 */
	public static String toAbsoluteURI(String fileName) {
		// Permit backslashes when using the file: URI scheme under Windows
		// This is not required for the latter File.toURL() call
		if (System.getProperty("os.name").toLowerCase().indexOf("win") != -1) {
			fileName = fileName.replaceAll("\\\\", "/");
		}
		try {
			// Check if it's an absolute URI already - but don't confuse Windows
			// drive letters with URI schemes
			if (fileName.matches("[a-zA-Z0-9]{2,}:.*")
					&& new URI(fileName).isAbsolute()) {
				return fileName;
			}
			return new File(fileName).getAbsoluteFile().toURI().normalize()
					.toString();
		} catch (URISyntaxException ex) {
			throw new D2RQException(ex);
		}
	}

	private boolean isLocalMappingFile;
	private String configURL;
	private String mappingFilename = null;
	private Model model = null;
	private int port = -1;
	private String baseURI = null;
	private String serverName = null;
	private Resource documentMetadata = null;
	private boolean vocabularyIncludeInstances = true;
	private boolean autoReloadMapping = true;
	private int limitPerClassMap = DEFAULT_LIMIT_PER_CLASS_MAP;
	private int limitPerPropertyBridge = DEFAULT_LIMIT_PER_PROPERTY_BRIDGE;
	private boolean enableMetadata = true;
	private double sparqlTimeout = 60;
	private double pageTimeout = 12;
	
	/**
	 * @param configURL
	 *            Config file URL, or <code>null</code> for an empty config
	 */
	public ConfigLoader(String configURL) {
		this.configURL = configURL;
		if (configURL == null) {
			isLocalMappingFile = false;
		} else {
			if (configURL.startsWith("file://")) {
				isLocalMappingFile = true;
				mappingFilename = configURL.substring(7);
			} else if (configURL.startsWith("file:")) {
				isLocalMappingFile = true;
				mappingFilename = configURL.substring(5);
			} else if (configURL.indexOf(":") == -1) {
				isLocalMappingFile = true;
				mappingFilename = configURL;
			}
		}
	}

	public void load() {
		if (configURL == null) {
			model = ModelFactory.createDefaultModel();
			return;
		}
		this.model = FileManager.get().loadModel(this.configURL);
		Resource server = findServerResource();
		if (server == null) {
			return;
		}
		new VocabularySummarizer(D2RConfig.class).assertNoUndefinedTerms(model, 
				D2RQException.CONFIG_UNKNOWN_PROPERTY, 
				D2RQException.CONFIG_UNKNOWN_CLASS);
		Statement s = server.getProperty(D2RConfig.baseURI);
		if (s != null) {
			this.baseURI = s.getResource().getURI();
		}
		s = server.getProperty(D2RConfig.port);
		if (s != null) {
			String value = s.getLiteral().getLexicalForm();
			try {
				this.port = Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				throw new D2RQException("Illegal integer value '" + value
						+ "' for d2r:port", D2RQException.MUST_BE_NUMERIC);
			}
		}
		s = server.getProperty(RDFS.label);
		if (s != null) {
			this.serverName = s.getString();
		}
		s = server.getProperty(D2RConfig.documentMetadata);
		if (s != null) {
			this.documentMetadata = s.getResource();
		}
		s = server.getProperty(D2RConfig.vocabularyIncludeInstances);
		if (s != null) {
			this.vocabularyIncludeInstances = s.getBoolean();
		}
		s = server.getProperty(D2RConfig.autoReloadMapping);
		if (s != null) {
			this.autoReloadMapping = s.getBoolean();
		}
		s = server.getProperty(D2RConfig.limitPerClassMap);
		if (s != null) {
			try {
				limitPerClassMap = s.getInt();
			} catch (JenaException ex) {
				if (!s.getBoolean()) {
					limitPerClassMap = Relation.NO_LIMIT;
				}
			}
		}
		s = server.getProperty(D2RConfig.limitPerPropertyBridge);
		if (s != null) {
			try {
				limitPerPropertyBridge = s.getInt();
			} catch (JenaException ex) {
				if (!s.getBoolean()) {
					limitPerPropertyBridge = Relation.NO_LIMIT;
				}
			}
		}
		s = server.getProperty(D2RConfig.enableMetadata);
		if (s != null) {
			this.enableMetadata = s.getBoolean();
		}
		s = server.getProperty(D2RConfig.pageTimeout);
		if (s != null) {
			try {
				String value = s.getLiteral().getLexicalForm();
				pageTimeout = Double.parseDouble(value);
			} catch (Exception ex) {
				throw new D2RQException("Value for d2r:pageTimeout must be a numeric literal: '" + 
						s.getObject() + "'", D2RQException.MUST_BE_NUMERIC);
			}
		}
		s = server.getProperty(D2RConfig.sparqlTimeout);
		if (s != null) {
			try {
				String value = s.getLiteral().getLexicalForm();
				sparqlTimeout = Double.parseDouble(value);
			} catch (Exception ex) {
				throw new D2RQException("Value for d2r:sparqlTimeout must be a numeric literal: '" + 
						s.getObject() + "'", D2RQException.MUST_BE_NUMERIC);
			}
		}
	}

	public boolean isLocalMappingFile() {
		return this.isLocalMappingFile;
	}

	public String getLocalMappingFilename() {
		if (!this.isLocalMappingFile) {
			return null;
		}
		return this.mappingFilename;
	}

	public int port() {
		if (this.model == null) {
			throw new IllegalStateException("Must load() first");
		}
		return this.port;
	}

	public String baseURI() {
		if (this.model == null) {
			throw new IllegalStateException("Must load() first");
		}
		return this.baseURI;
	}

	public String serverName() {
		if (this.model == null) {
			throw new IllegalStateException("Must load() first");
		}
		return this.serverName;
	}

	public boolean getVocabularyIncludeInstances() {
		return this.vocabularyIncludeInstances;
	}

	public int getLimitPerClassMap() {
		return limitPerClassMap;
	}

	public int getLimitPerPropertyBridge() {
		return limitPerPropertyBridge;
	}

	public boolean getAutoReloadMapping() {
		return this.autoReloadMapping;
	}

	public double getPageTimeout() {
		return pageTimeout;
	}
	
	public double getSPARQLTimeout() {
		return sparqlTimeout;
	}
	
	public void addDocumentMetadata(Model document, Resource documentResource) {
		if (this.documentMetadata == null) {
			return;
		}
		if (this.model == null) {
			throw new IllegalStateException("Must load() first");
		}
		StmtIterator it = this.documentMetadata.listProperties();
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();

			document.add(documentResource, stmt.getPredicate(),
					stmt.getObject());
		}
		it = this.model.listStatements(null, null, this.documentMetadata);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (stmt.getPredicate().equals(D2RConfig.documentMetadata)) {
				continue;
			}
			document.add(stmt.getSubject(), stmt.getPredicate(),
					documentResource);
		}
	}

	protected Resource findServerResource() {
		ResIterator it = this.model.listSubjectsWithProperty(RDF.type,
				D2RConfig.Server);
		if (!it.hasNext()) {
			return null;
		}
		return it.nextResource();
	}

	protected Resource findDatabaseResource() {
		ResIterator it = this.model.listSubjectsWithProperty(RDF.type,
				D2RQ.Database);
		if (!it.hasNext()) {
			return null;
		}
		return it.nextResource();
	}

	// cache resource metadata model, so we dont need to load the file on every
	// request (!)
	private Model resourceMetadataTemplate = null;

	protected Model getResourceMetadataTemplate(D2RServer server,
			ServletContext context) {
		if (resourceMetadataTemplate == null) {
			resourceMetadataTemplate = loadMetadataTemplate(server, context,
					D2RConfig.metadataTemplate, "resource-metadata.ttl");
		}
		return resourceMetadataTemplate;
	}

	// cache dataset metadata model, so we dont need to load the file on every
	// request (!)
	private Model datasetMetadataTemplate = null;

	protected Model getDatasetMetadataTemplate(D2RServer server,
			ServletContext context) {
		if (datasetMetadataTemplate == null) {
			datasetMetadataTemplate = loadMetadataTemplate(server, context,
					D2RConfig.datasetMetadataTemplate, "dataset-metadata.ttl");

		}
		return datasetMetadataTemplate;
	}

	private Model loadMetadataTemplate(D2RServer server,
			ServletContext context, Property configurationFlag,
			String defaultTemplateName) {

		Model metadataTemplate;

		File userTemplateFile = MetadataCreator.findTemplateFile(server,
				configurationFlag);
		Model userResourceTemplate = MetadataCreator
				.loadTemplateFile(userTemplateFile);
		if (userResourceTemplate != null && userResourceTemplate.size() > 0) {
			metadataTemplate = userResourceTemplate;
			log.info("Using user-specified metadata template at '"
					+ userTemplateFile + "'");

		} else {
			// load default template
			InputStream drtStream = context.getResourceAsStream("/WEB-INF/"
					+ defaultTemplateName);
			log.info("Using default metadata template.");
			metadataTemplate = MetadataCreator.loadMetadataTemplate(drtStream);
		}

		return metadataTemplate;
	}

	protected boolean serveMetadata() {
		return enableMetadata;
	}
}