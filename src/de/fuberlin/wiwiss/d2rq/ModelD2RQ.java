/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/

package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.enhanced.*;

/**
 * A D2RQ read-only Jena model backed by a non-RDF database.
 *
 * D2RQ is a declarative mapping language for describing mappings between ontologies and relational data models.
 * More information about D2RQ is found at: http://www.wiwiss.fu-berlin.de/suhl/bizer/d2rq/
 *
 * <p>History:<br>
 * 06-14-2004: Initial version of this class.<br>
 * 08-03-2004: enableDebug(), new constructors
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 *
 * @see de.fuberlin.wiwiss.d2rq.GraphD2RQ
 */
public class ModelD2RQ extends ModelCom implements Model {

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ map that must be provided in N3 notation.
	 * @param mapURL URL of the D2RQ map to be used for this model
	 */
	public ModelD2RQ(String mapURL) {
		super(new GraphD2RQ(mapURL), BuiltinPersonalities.model);
	}

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ map that may be in "RDF/XML", "N-TRIPLES" or "N3"
	 * format.
	 * @param mapURL URL of the D2RQ map to be used for this model
	 * @param serializationFormat the format of the map
	 */
	public ModelD2RQ(String mapURL, String serializationFormat) {
		super(new GraphD2RQ(mapURL, serializationFormat), BuiltinPersonalities.model);
	}

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ map that is provided as a Jena model.
	 * @param mapModel a Jena model containing the D2RQ map
	 */
	public ModelD2RQ(Model mapModel) {
		super(new GraphD2RQ(mapModel), BuiltinPersonalities.model);
	}

	/**
	 * Enables D2RQ debug messages.
	 */
	public void enableDebug() {
		((GraphD2RQ) getGraph()).enableDebug();
	}
}


