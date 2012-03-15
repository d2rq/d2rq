package de.fuberlin.wiwiss.d2rq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationalOperators;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.engine.D2RQDatasetGraph;
import de.fuberlin.wiwiss.d2rq.engine.QueryEngineD2RQ;
import de.fuberlin.wiwiss.d2rq.find.FindQuery;
import de.fuberlin.wiwiss.d2rq.map.Configuration;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;

/**
 * A D2RQ virtual read-only graph backed by a non-RDF database.
 * 
 * D2RQ is a declarative mapping language for describing mappings between
 * ontologies and relational data models. More information about D2RQ is found
 * at: http://www4.wiwiss.fu-berlin.de/bizer/d2rq/
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class GraphD2RQ extends GraphBase implements Graph {
	
	static {
		QueryEngineD2RQ.register();
	}
	
	private Log log = LogFactory.getLog(GraphD2RQ.class);
	private final Capabilities capabilities = new D2RQCapabilities();
	private final Mapping mapping;
	private final D2RQDatasetGraph dataset = new D2RQDatasetGraph(this);

	/**
	 * Creates a new D2RQ graph from a Jena model containing a D2RQ
	 * mapping.
	 * @param mapModel the model containing a D2RQ mapping file
	 * @param baseURIForData Base URI for turning relative URI patterns into
	 * 		absolute URIs; if <tt>null</tt>, then D2RQ will pick a base URI
	 * @throws D2RQException on error in the mapping model
	 */
	public GraphD2RQ(Model mapModel, String baseURIForData) throws D2RQException {
		this(new MapParser(mapModel, 
				(baseURIForData == null) ? "http://localhost/resource/" : baseURIForData).parse());
	}

	/**
	 * Creates a new D2RQ graph from a previously prepared {@link Mapping} instance.
	 * @param mapping A D2RQ mapping
	 * @throws D2RQException If the mapping is invalid
	 */
	public GraphD2RQ(Mapping mapping) throws D2RQException {
		this.mapping = mapping;
		this.mapping.validate();
		getPrefixMapping().setNsPrefixes(mapping.getPrefixMapping());
	}

	public Mapping getMapping() {
		return mapping;
	}
	
	/**
	 * Returns a QueryHandler for this graph.
	 * The query handler class can be set by the mapping.
	 * It then must have exact constructor signature QueryHandler(Graph)
	 * For some reasons, Java does not allow to call getConstructor(GraphD2RQ.class)
	 * on SimpleQueryHandler class.
	 * @see com.hp.hpl.jena.graph.Graph#queryHandler
	 */
	public QueryHandler queryHandler() {
		// jg: it would be more efficient to have just one instance per graph
		// on the other hand: new instance guaranties that all information with handler
		// is up to date.
		checkOpen();
		return new D2RQQueryHandler(this, dataset);
	}

	/**
	 * @see com.hp.hpl.jena.graph.Graph#close()
	 */
	public void close() {
		for (Database db: mapping.databases()) {
			db.connectedDB().close();
		}
		this.closed = true;
	}

	public Capabilities getCapabilities() { 
		return this.capabilities;
	}

	public ExtendedIterator<Triple> graphBaseFind( TripleMatch m ) {
		checkOpen();
		Triple t = m.asTriple();
		if (this.log.isDebugEnabled()) {
			this.log.debug("Find: " + PrettyPrinter.toString(t, getPrefixMapping()));
		}
		return new FindQuery(t, this.mapping.compiledPropertyBridges(), this.mapping.configuration().getServeVocabulary(), this.mapping.getHasDynamicProperties(), this.mapping.getVocabularyModel()).iterator();
    }

	/**
	 * Connects all databases. This is done automatically if
	 * needed. The method can be used to test the connections
	 * earlier.
	 * @throws D2RQException on connection failure
	 */
	public void connect() {
		for (Database db: mapping.databases()) {
			db.connectedDB().connection();
		}
	}
	
	static TripleRelation[] emptyPropertyBridgeArray=new TripleRelation[0];
	
    /**
     * TODO This section was done as a quick hack for D2R Server 0.3 and really shouldn't be here
     */
    private Map<String,List<TripleRelation>> classMapInventoryBridges = new HashMap<String,List<TripleRelation>>();
    private Map<String,NodeMaker> classMapNodeMakers = new HashMap<String,NodeMaker>();
    
    public void initInventory(String inventoryBaseURI) {
		for (Resource classMapResource: mapping.classMapResources()) {
			NodeMaker resourceMaker = this.mapping.classMap(classMapResource).nodeMaker();
			Node classMap = classMapResource.asNode();
			this.classMapNodeMakers.put(toClassMapName(classMap), resourceMaker);
			List<TripleRelation> inventoryBridges = new ArrayList<TripleRelation>();
			for (TripleRelation bridge: mapping.classMap(classMapResource).compiledPropertyBridges()) {
				if (bridge.selectTriple(new Triple(Node.ANY, RDF.Nodes.type, Node.ANY)) != null) {
					inventoryBridges.add(bridge);
				}
				if (bridge.selectTriple(new Triple(Node.ANY, RDFS.label.asNode(), Node.ANY)) != null) {
					inventoryBridges.add(bridge);
				}
			}
			if (inventoryBridges.isEmpty()) {
				Relation relation = (Relation) this.mapping.classMap(classMapResource).relation();
				NodeMaker typeNodeMaker = new FixedNodeMaker(
						RDF.type.asNode(), false);
				NodeMaker resourceNodeMaker = new FixedNodeMaker(RDFS.Resource.asNode(), false);
				inventoryBridges.add(new TripleRelation(relation, 
						resourceMaker, typeNodeMaker, resourceNodeMaker));
			}
			this.classMapInventoryBridges.put(toClassMapName(classMap), inventoryBridges);
		}
    }

    private String toClassMapName(Node classMap) {
    	return classMap.getLocalName();
    }
    
    public Collection<String> classMapNames() {
    	return this.classMapInventoryBridges.keySet();
    }
    
    public Model classMapInventory(String classMapName) {
    	List<TripleRelation> inventoryBridges = classMapInventoryBridges.get(classMapName);
    	if (inventoryBridges == null) {
    		return null;
    	}
    	Model result = ModelFactory.createDefaultModel();
    	result.setNsPrefixes(this.getPrefixMapping());
    	result.getGraph().getBulkUpdateHandler().add(
    			new FindQuery(Triple.ANY, inventoryBridges).iterator());
    	return result;
    }

    public Collection<String> classMapNamesForResource(Node resource) {
    	if (!resource.isURI()) {
    		return Collections.<String>emptyList();
    	}
    	List<String> results = new ArrayList<String>();
		for (Entry<String,NodeMaker> entry: classMapNodeMakers.entrySet()) {
			String classMapName = entry.getKey();
			NodeMaker nodeMaker = entry.getValue();
			if (!nodeMaker.selectNode(resource, RelationalOperators.DUMMY).equals(NodeMaker.EMPTY)) {
				results.add(classMapName);
			}
		}
    	return results;
    }
    
    public Collection<TripleRelation> tripleRelations() {
    	return mapping.compiledPropertyBridges();
    }
    
    public Configuration getConfiguration() {
    	return this.mapping.configuration();
    }
}