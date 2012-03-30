package de.fuberlin.wiwiss.d2rq.jena;

import com.hp.hpl.jena.enhanced.BuiltinPersonalities;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;

/**
 * <p>A D2RQ read-only Jena model backed by a D2RQ-mapped non-RDF database.</p>
 *
 * <p>D2RQ is a declarative mapping language for describing mappings between ontologies and relational data models.
 * More information about D2RQ is found at: http://www4.wiwiss.fu-berlin.de/bizer/d2rq/</p>
 *
 * <p>This class is a thin wrapper around a {@link GraphD2RQ} and provides only
 * convenience constructors.</p>
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 *
 * @see de.fuberlin.wiwiss.d2rq.jena.GraphD2RQ
 */
public class ModelD2RQ extends ModelCom implements Model {

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ map that will be loaded from the given URL.
	 * Its serialization format will be guessed from the
	 * file extension and defaults to RDF/XML.
	 * @param mapURL URL of the D2RQ map to be used for this model
	 */
	public ModelD2RQ(String mapURL) {
		this(FileManager.get().loadModel(mapURL), mapURL + "#");
	}

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ map that may be in "RDF/XML", "N-TRIPLES" or "TURTLE"
	 * format.
	 * @param mapURL URL of the D2RQ map to be used for this model
	 * @param serializationFormat the format of the map, or <tt>null</tt>
	 * 		for guessing based on the file extension
	 * @param baseURIForData Base URI for turning relative URI patterns into
	 * 		absolute URIs; if <tt>null</tt>, then D2RQ will pick a base URI
	 */
	public ModelD2RQ(String mapURL, String serializationFormat, String baseURIForData) {
		this(FileManager.get().loadModel(mapURL, serializationFormat),
				(baseURIForData == null) ? mapURL + "#" : baseURIForData);
	}

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ map that is provided as a Jena model.
	 * @param mapModel a Jena model containing the D2RQ map
	 * @param baseURIForData Base URI for turning relative URI patterns into
	 * 		absolute URIs; if <tt>null</tt>, then D2RQ will pick a base URI
	 */
	public ModelD2RQ(Model mapModel, String baseURIForData) {
		super(new GraphD2RQ(new MapParser(mapModel, 
				(baseURIForData == null) ? "http://localhost/resource/" : baseURIForData).parse()), BuiltinPersonalities.model); // BuiltinPersonalities.model really required?
	}
	
	/**
	 * Create a non-RDF database-based model. The model is created
	 * from the given D2RQ map.
	 */
	public ModelD2RQ(Mapping mapping) {
		super(new GraphD2RQ(mapping), BuiltinPersonalities.model);
	}
	
	/**
	 * @return The underlying {@link GraphD2RQ}
	 */
	@Override
	public GraphD2RQ getGraph() {
		return (GraphD2RQ) super.getGraph();
	}
}