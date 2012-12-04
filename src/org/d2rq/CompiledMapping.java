package org.d2rq;

import java.util.Collection;
import java.util.List;

import org.d2rq.algebra.DownloadRelation;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.SQLConnection;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.util.Context;


/**
 * TODO: Rename to Mapping?
 * TODO: Write documentation
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface CompiledMapping {

	// TODO: Probably unnecessary as we already had to connect in order to compile
	void connect();
	
	void close();

	PrefixMapping getPrefixes();

	Collection<TripleRelation> getTripleRelations();
	
	Collection<? extends DownloadRelation> getDownloadRelations();

	Collection<SQLConnection> getSQLConnections();
	
	List<String> getResourceCollectionNames();
	
	List<String> getResourceCollectionNames(Node resource);
	
	ResourceCollection getResourceCollection(String name);
	
	Graph getAdditionalTriples();
	
	Context getContext();
}
