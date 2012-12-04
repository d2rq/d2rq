package org.d2rq.jena;

import org.d2rq.CompiledMapping;
import org.d2rq.SystemLoader;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;


/**
 * <p>A D2RQ read-only Jena model backed by a D2RQ-mapped non-RDF database.</p>
 *
 * <p>This class is a thin wrapper around a {@link GraphD2RQ} and provides only
 * convenience constructors. {@link SystemLoader} provides a more flexible
 * and powerful facade for generating models.</p>
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 *
 * @see GraphD2RQ
 * @see SystemLoader
 */
public class ModelD2RQ extends ModelCom implements Model {

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ or R2RML mapping that will be loaded from the given URL.
	 * Its serialization format will be guessed from the
	 * file extension.
	 *
	 * @param mapURL URL of the D2RQ map to be used for this model
	 */
	public ModelD2RQ(String mapURL) {
		this(mapURL, null);
	}

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ or R2RML mapping that will be loaded from the given URL.
	 * Its serialization format will be guessed from the
	 * file extension.
	 *
	 * @param mapURL URL of the D2RQ map to be used for this model
	 * @param baseIRI Base IRI for turning relative IRI patterns into
	 * 		absolute URIs; if <tt>null</tt>, then D2RQ will pick a base URI
	 */
	public ModelD2RQ(String mapURL, String baseIRI) {
		this(SystemLoader.loadMapping(mapURL, baseIRI));
	}

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ or R2RML mapping that is provided as a Jena model.
	 * @param mappingModel a Jena model containing a D2RQ or R2RML mapping
	 * @param baseIRI Base IRI for turning relative IRI patterns into
	 * 		absolute URIs; if <tt>null</tt>, then D2RQ will pick a base URI
	 */
	public ModelD2RQ(Model mappingModel, String baseIRI) {
		this(SystemLoader.createMapping(mappingModel, baseIRI));
	}
	
	/**
	 * Create a non-RDF database-based model. The model is created
	 * from the given mapping.
	 */
	public ModelD2RQ(CompiledMapping mapping) {
		super(new GraphD2RQ(mapping));
	}
	
	/**
	 * @return The underlying {@link GraphD2RQ} of this model
	 */
	@Override
	public GraphD2RQ getGraph() {
		return (GraphD2RQ) super.getGraph();
	}
}