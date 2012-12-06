package org.d2rq.lang;

import com.hp.hpl.jena.rdf.model.Resource;


/**
 * Representation of a d2rq:Configuration from the mapping file.
 *
 * @author Christian Becker <http://beckr.org#chris>
 */
public class Configuration extends MapObject {
	public final static boolean DEFAULT_SERVE_VOCABULARY = true;
	public final static boolean DEFAULT_USE_ALL_OPTIMIZATIONS = false;
	
	private boolean serveVocabulary = DEFAULT_SERVE_VOCABULARY;
	private boolean useAllOptimizations = DEFAULT_USE_ALL_OPTIMIZATIONS;
	
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