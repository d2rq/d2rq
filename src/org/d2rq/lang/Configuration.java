package org.d2rq.lang;

import com.hp.hpl.jena.rdf.model.Resource;


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
		return serveVocabulary;
	}
	
	public void setServeVocabulary(boolean serveVocabulary) {
		this.serveVocabulary = serveVocabulary;
	}

	public boolean getUseAllOptimizations() {
		return useAllOptimizations;
	}

	public void setUseAllOptimizations(boolean useAllOptimizations) {
		this.useAllOptimizations = useAllOptimizations;
	}

	public String toString() {
		return "d2rq:Configuration " + super.toString();
	}

	public void accept(D2RQMappingVisitor visitor) {
		visitor.visit(this);
	}
}