package de.fuberlin.wiwiss.d2rq.server;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
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

public class ConfigLoader {

	public static final int DEFAULT_LIMIT_PER_CLASS_MAP = 50;
	public static final int DEFAULT_LIMIT_PER_PROPERTY_BRIDGE = 50;
	
	/**
	 * Accepts an absolute URI, relative file: URI, or plain
	 * file name (including names with spaces, Windows backslashes
	 * etc) and returns an equivalent full absolute URI.
	 */
	public static String toAbsoluteURI(String fileName) {
		// Permit backslashes when using the file: URI scheme under Windows
		// This is not required for the latter File.toURL() call
		if (System.getProperty("os.name").toLowerCase().indexOf("win") != -1) {
			fileName = fileName.replaceAll("\\\\", "/");
		}
		try {
			// Check if it's an absolute URI already - but don't confuse Windows drive letters with URI schemes
			if (fileName.matches("[a-zA-Z0-9]{2,}:.*") && new URI(fileName).isAbsolute()) {
				return fileName;
			}
			return new File(fileName).getAbsoluteFile().toURI().normalize().toString();
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
	
	/**
	 * @param configURL Config file URL, or <code>null</code> for an empty config
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
				throw new D2RQException(
						"Illegal integer value '" + value + "' for d2r:port");
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
			document.add(documentResource, stmt.getPredicate(), stmt.getObject());
		}
		it = this.model.listStatements(null, null, this.documentMetadata);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			if (stmt.getPredicate().equals(D2RConfig.documentMetadata)) {
				continue;
			}
			document.add(stmt.getSubject(), stmt.getPredicate(), documentResource);
		}
	}
	
	protected Resource findServerResource() {
		ResIterator it = this.model.listSubjectsWithProperty(RDF.type, D2RConfig.Server);
		if (!it.hasNext()) {
			return null;
		}
		return it.nextResource();
	}
}