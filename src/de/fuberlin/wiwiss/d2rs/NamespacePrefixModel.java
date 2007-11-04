package de.fuberlin.wiwiss.d2rs;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;
import com.hp.hpl.jena.shared.PrefixMapping;

/**
 * A model that presents a simple RDF description of a namespace mapping.
 * Initially, the model will be empty. A call to {@link #update(PrefixMapping)}
 * will change the model's state to represent the argument {@link PrefixMapping}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: NamespacePrefixModel.java,v 1.4 2007/11/04 00:41:14 cyganiak Exp $
 */
public class NamespacePrefixModel extends ModelCom {
	public final static String namespaceModelURI = 
		"http://www.wiwiss.fu-berlin.de/suhl/bizer/d2r-server/vocab#ns_model";
	public final static String prefixPropertyURI = 
		"http://www.wiwiss.fu-berlin.de/suhl/bizer/d2r-server/vocab#prefix";
	public final static Property prefix = ResourceFactory.createProperty(prefixPropertyURI);

	private Set ignorableNamespaceURIs = new HashSet(1);
	
	public NamespacePrefixModel() {
		super(com.hp.hpl.jena.graph.Factory.createGraphMem());
	}
	
	public void ignoreNamespaceURI(String uri) {
		ignorableNamespaceURIs.add(uri);
	}
	
	public void update(PrefixMapping prefixes) {
		removeAll();
		Iterator it = prefixes.getNsPrefixMap().keySet().iterator();
		while (it.hasNext()) {
			String prefix = (String) it.next();
			String uri = prefixes.getNsPrefixURI(prefix);
			if (ignorableNamespaceURIs.contains(uri)) continue;
			getResource(uri).addProperty(NamespacePrefixModel.prefix, prefix);
		}
	}
}
