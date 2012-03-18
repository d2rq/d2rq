package de.fuberlin.wiwiss.d2rq.map;

import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;


/**
 * Representation of a d2rq:Configuration from the mapping file.
 *
 * @author Christian Becker <http://beckr.org#chris>
 */
public class Configuration extends MapObject {
	private boolean serveVocabulary = true;
	private boolean useAllOptimizations = false;
	
	public Configuration() {
		this(null);
	}
	
	public Configuration(Resource resource) {
		super(resource);
	}
	
	public boolean getServeVocabulary() {
		return this.serveVocabulary;
	}
	
	public void setServeVocabulary(boolean serveVocabulary) {
		this.serveVocabulary = serveVocabulary;
	}

	public boolean getUseAllOptimizations() {
		return this.useAllOptimizations;
	}

	public void setUseAllOptimizations(boolean useAllOptimizations) {
		this.useAllOptimizations = useAllOptimizations;
	}

	public String toString() {
		return "d2rq:Configuration " + super.toString();
	}

	public void validate() throws D2RQException {
		/* All settings are optional */
	}
}