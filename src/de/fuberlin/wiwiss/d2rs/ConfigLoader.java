package de.fuberlin.wiwiss.d2rs;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rs.vocab.D2R;

public class ConfigLoader {
	private boolean isLocalMappingFile;
	private String configURL;
	private String mappingFilename = null;
	private Model model = null;
	private int port = -1;
	private String baseURI = null;
	private String serverName = null;
	private Resource documentMetadata = null;
	
	public ConfigLoader(String configURL) {
		this.configURL = configURL;
		if (configURL.startsWith("file://")) {
			this.isLocalMappingFile = true;
			this.mappingFilename = configURL.substring(7);
		} else if (configURL.startsWith("file:")) {
			this.isLocalMappingFile = true;
			this.mappingFilename = configURL.substring(5);
		} else if (configURL.indexOf(":") == -1) {
			this.isLocalMappingFile = true;
			this.mappingFilename = configURL;
		}
	}

	public void load() throws JenaException {
		this.model = FileManager.get().loadModel(this.configURL);
		Resource server = findServerResource();
		if (server == null) {
			return;
		}
		Statement s = server.getProperty(D2R.baseURI);
		if (s != null) {
			this.baseURI = s.getResource().getURI();
		}
		s = server.getProperty(D2R.port);
		if (s != null) {
			String value = s.getLiteral().getLexicalForm();
			try {
				this.port = Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				throw new JenaException(
						"Illegal integer value '" + value + "' for d2r:port");
			}
		}
		s = server.getProperty(RDFS.label);
		if (s != null) {
			this.serverName = s.getString();
		}
		s = server.getProperty(D2R.documentMetadata);
		if (s != null) {
			this.documentMetadata = s.getResource();
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
	
	public String getMappingURL() {
		return this.configURL;
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
			if (stmt.getPredicate().equals(D2R.documentMetadata)) {
				continue;
			}
			document.add(stmt.getSubject(), stmt.getPredicate(), documentResource);
		}
	}
	
	private Resource findServerResource() {
		ResIterator it = this.model.listSubjectsWithProperty(RDF.type, D2R.Server);
		if (!it.hasNext()) {
			return null;
		}
		return it.nextResource();
	}
}