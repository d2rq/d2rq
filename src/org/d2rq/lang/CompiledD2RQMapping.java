package org.d2rq.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.d2rq.CompiledMapping;
import org.d2rq.D2RQException;
import org.d2rq.D2RQOptions;
import org.d2rq.ResourceCollection;
import org.d2rq.algebra.DownloadRelation;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.SQLConnection;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.hp.hpl.jena.sparql.util.Context;


public class CompiledD2RQMapping implements CompiledMapping {
	private final Collection<SQLConnection> sqlConnections = new ArrayList<SQLConnection>();
	private final Collection<TripleRelation> tripleRelations = new ArrayList<TripleRelation>();
	private final Map<String,ResourceCollection> resourceCollections = new HashMap<String,ResourceCollection>();
	private final Collection<DownloadRelation> downloadRelations = new ArrayList<DownloadRelation>();
	private PrefixMapping prefixes = PrefixMapping.Standard;
	private Graph additionalTriples = GraphFactory.createPlainGraph();
	private boolean connected = false;
	private boolean fastMode = false;
	
	public void addSQLConnection(SQLConnection connection) {
		sqlConnections.add(connection);
	}
	
	/**
	 * Connects all databases. This is done automatically if
	 * needed. The method can be used to test the connections
	 * earlier.
	 * 
	 * @throws D2RQException on connection failure
	 */
	public void connect() {
		if (connected) return;
		connected = true;
		for (SQLConnection sqlConnection: sqlConnections) {
			sqlConnection.connection();
		}
	}

	public void close() {
		for (SQLConnection sqlConnection: sqlConnections) {
			sqlConnection.close();
		}
	}

	public void setPrefixes(PrefixMapping prefixes) {
		this.prefixes = prefixes;
	}
	
	public PrefixMapping getPrefixes() {
		return prefixes;
	}

	public void setAdditionalTriples(Graph graph) {
		additionalTriples = graph;
	}
	
	public Graph getAdditionalTriples() {
		return additionalTriples;
	}

	public void setFastMode(boolean fastMode) {
		this.fastMode = fastMode;
	}
	
	public Context getContext() {
		return D2RQOptions.getContext(fastMode);
	}
	
	public void addTripleRelation(TripleRelation tripleRelation) {
		tripleRelations.add(tripleRelation);
	}
	
	public Collection<TripleRelation> getTripleRelations() {
		return tripleRelations;
	}

	public Collection<SQLConnection> getSQLConnections() {
		return sqlConnections;
	}
	
	public void addResourceCollection(String name, ResourceCollection resources) {
		resourceCollections.put(name, resources);
	}
	
	public List<String> getResourceCollectionNames() {
		List<String> result = new ArrayList<String>(resourceCollections.keySet());
		Collections.sort(result);
		return result;
	}

	public List<String> getResourceCollectionNames(Node forNode) {
		List<String> result = new ArrayList<String>();
		for (String name: resourceCollections.keySet()) {
			if (resourceCollections.get(name).mayContain(forNode)) {
				result.add(name);
			}
		}
		Collections.sort(result);
		return result;
	}

	public ResourceCollection getResourceCollection(String name) {
		return resourceCollections.get(name);
	}

	public void addDownloadRelation(DownloadRelation downloadRelation) {
		downloadRelations.add(downloadRelation);
	}
	
	public Collection<? extends DownloadRelation> getDownloadRelations() {
		return downloadRelations;
	}
}
