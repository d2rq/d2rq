/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/

package de.fuberlin.wiwiss.d2rq;

import java.util.Iterator;
import java.util.Map.Entry;

import com.hp.hpl.jena.enhanced.BuiltinPersonalities;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.FileManager;

import de.fuberlin.wiwiss.d2rq.map.D2RQ;

/**
 * A D2RQ read-only Jena model backed by a non-RDF database.
 *
 * D2RQ is a declarative mapping language for describing mappings between ontologies and relational data models.
 * More information about D2RQ is found at: http://www.wiwiss.fu-berlin.de/suhl/bizer/d2rq/
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ModelD2RQ.java,v 1.8 2006/09/02 22:41:43 cyganiak Exp $
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
		this(FileManager.get().loadModel(mapURL), mapURL + "#");
	}

	/** 
	 * Create a non-RDF database-based model. The model is created
	 * from a D2RQ map that may be in "RDF/XML", "N-TRIPLES" or "N3"
	 * format.
	 * @param mapURL URL of the D2RQ map to be used for this model
	 * @param serializationFormat the format of the map
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
		super(new GraphD2RQ(mapModel, baseURIForData), BuiltinPersonalities.model);
		copyPrefixes(mapModel);
	}

	/**
	 * Copies all prefixes from the mapping file to the D2RQ model.
	 * This makes the output of Model.write(...) nicer. The D2RQ
	 * prefix is dropped on the assumption that it is not wanted
	 * in the actual data.
	 */ 
	private void copyPrefixes(PrefixMapping prefixes) {
		setNsPrefixes(prefixes);
		Iterator it = getNsPrefixMap().entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			String namespace = (String) entry.getValue();
			if (D2RQ.uri.equals(namespace) || namespace.startsWith("file:")) {
				removeNsPrefix((String) entry.getKey());
			}
		}
	}
}