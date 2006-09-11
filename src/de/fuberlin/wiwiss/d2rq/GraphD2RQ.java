package de.fuberlin.wiwiss.d2rq;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelationImpl;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.find.FindQuery;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.pp.PrettyPrinter;
import de.fuberlin.wiwiss.d2rq.rdql.D2RQQueryHandler;
import de.fuberlin.wiwiss.d2rq.rdql.GraphUtils;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * A D2RQ virtual read-only graph backed by a non-RDF database.
 * 
 * D2RQ is a declarative mapping language for describing mappings between
 * ontologies and relational data models. More information about D2RQ is found
 * at: http://www.wiwiss.fu-berlin.de/suhl/bizer/d2rq/
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: GraphD2RQ.java,v 1.31 2006/09/11 22:29:20 cyganiak Exp $
 */
public class GraphD2RQ extends GraphBase implements Graph {
	private Log log = LogFactory.getLog(GraphD2RQ.class);
	
	static private boolean usingD2RQQueryHandler=false;
	
//	private final ReificationStyle style;
//	private boolean closed = false;
	private Capabilities capabilities = null;

	private Mapping mapping;

	public static boolean isUsingD2RQQueryHandler() {
		return usingD2RQQueryHandler;
	}
	public static void setUsingD2RQQueryHandler(boolean usingD2RQQueryHandler) {
		GraphD2RQ.usingD2RQQueryHandler = usingD2RQQueryHandler;
	}
	
	public Collection getPropertyBridges() {
		return this.mapping.compiledPropertyBridges();
	}
	public Collection getPropertyBridges(Database db) {
		return (Collection) this.mapping.compiledPropertyBridgesByDatabase().get(db);
	}

	/**
	 * Creates a new D2RQ graph from a Jena model containing a D2RQ
	 * mapping.
	 * @param mapModel the model containing a D2RQ mapping file
	 * @param baseURIForData Base URI for turning relative URI patterns into
	 * 		absolute URIs; if <tt>null</tt>, then D2RQ will pick a base URI
	 * @throws D2RQException on error in the mapping model
	 */
	public GraphD2RQ(Model mapModel, String baseURIForData) throws D2RQException {
		copyPrefixes(mapModel);
		if (baseURIForData == null) {
			baseURIForData = "http://localhost/resource/";
		}
		MapParser parser = new MapParser(mapModel, baseURIForData);
		this.mapping = parser.parse();
	}

	/**
	 * Copies all prefixes from the mapping file to the D2RQ model.
	 * This makes the output of Model.write(...) nicer. The D2RQ
	 * prefix is dropped on the assumption that it is not wanted
	 * in the actual data.
	 */ 
	private void copyPrefixes(PrefixMapping prefixes) {
		getPrefixMapping().setNsPrefixes(prefixes);
		Iterator it = getPrefixMapping().getNsPrefixMap().entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			String namespace = (String) entry.getValue();
			if (D2RQ.NS.equals(namespace)) {
				getPrefixMapping().removeNsPrefix((String) entry.getKey());
			}
		}
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
		String queryHandler = this.mapping.processingInstruction(D2RQ.queryHandler);
		if (queryHandler!=null) {
		    try {
		        Class c=Class.forName(queryHandler);
		        Constructor con=c.getConstructor(new Class[]{Graph.class}); 
		        QueryHandler qh=(QueryHandler)con.newInstance(new Object[]{this});
		        return qh;
		    } catch (Exception e) {
		        throw new RuntimeException(e);
		    }
		} else if (usingD2RQQueryHandler) {
			return new D2RQQueryHandler(this);
		} else {
		    return new SimpleQueryHandler(this);
		}
	}

	/**
	 * @see com.hp.hpl.jena.graph.Graph#close()
	 */
	public void close() {
		this.closed = true;
	}

	public Capabilities getCapabilities() { 
		if (this.capabilities == null) this.capabilities = new D2RQCapabilities();
		return this.capabilities;
	}

	public ExtendedIterator graphBaseFind( TripleMatch m ) {
		checkOpen();
		Triple t = m.asTriple();
		if (this.log.isDebugEnabled()) {
			this.log.debug("Find: " + PrettyPrinter.toString(t, getPrefixMapping()));
		}
		return new FindQuery(t, this.mapping.compiledPropertyBridges()).iterator();
    }
	
	static RDFRelationImpl[] emptyPropertyBridgeArray=new RDFRelationImpl[0];
	
	// used by D2RQPatternStage
	/**
	 * Finds all property bridges from this graph mapping that match a triple.
	 */
	public ArrayList propertyBridgesForTriple(Triple t) { // PropertyBridge[]
		return GraphUtils.propertyBridgesForTriple(t, this.mapping.compiledPropertyBridges());
	}
	
	public ArrayList propertyBridgesForTriple(Triple t, Database db) {
		return GraphUtils.propertyBridgesForTriple(t, getPropertyBridges(db));
	}	
	
    /**
     * @return Returns the propertyBridgesByDatabase.
     */
    public Map getPropertyBridgesByDatabase() {
        return this.mapping.compiledPropertyBridgesByDatabase();
    }
    
    /**
     * TODO This section was done as a quick hack for D2R Server 0.3 and really shouldn't be here
     */
    private String inventoryBaseURI = null;
    private Map classMapInventoryBridges = new HashMap();
    private Map classMapNodeMakers = new HashMap();
    
    public void initInventory(String inventoryBaseURI) {
    	this.inventoryBaseURI = inventoryBaseURI;
		Iterator it = this.mapping.classMapResources().iterator();
		while (it.hasNext()) {
			Resource classMapResource = (Resource) it.next();
			NodeMaker resourceMaker = (NodeMaker) this.mapping.classMap(classMapResource);
			Node classMap = classMapResource.asNode();
			this.classMapNodeMakers.put(toClassMapName(classMap), resourceMaker);
			List inventoryBridges = new ArrayList();
			Iterator bridgeIt = this.mapping.classMap(classMapResource).compiledPropertyBridges().iterator();
			while (bridgeIt.hasNext()) {
				RDFRelationImpl bridge = (RDFRelationImpl) bridgeIt.next();
				if (!bridge.selectTriple(new Triple(Node.ANY, RDF.Nodes.type, Node.ANY)).equals(Relation.EMPTY)) {
					inventoryBridges.add(bridge);
				}
				if (!bridge.selectTriple(new Triple(Node.ANY, RDFS.label.asNode(), Node.ANY)).equals(Relation.EMPTY)) {
					inventoryBridges.add(bridge);
				}
			}
			if (!this.mapping.classMap(classMapResource).compiledPropertyBridges().isEmpty()) {
				RDFRelationImpl aBridge = (RDFRelationImpl) this.mapping.classMap(classMapResource).compiledPropertyBridges().iterator().next();
				NodeMaker classMapNodeMaker = new FixedNodeMaker(
						Node.createURI(this.inventoryBaseURI + toClassMapName(classMap)), false);
				NodeMaker seeAlsoNodeMaker = new FixedNodeMaker(
						RDFS.seeAlso.asNode(), false);
				inventoryBridges.add(new RDFRelationImpl(aBridge.baseRelation(), 
						classMapNodeMaker, seeAlsoNodeMaker, resourceMaker));
			}
			this.classMapInventoryBridges.put(toClassMapName(classMap), inventoryBridges);
		}
    }

    private String toClassMapName(Node classMap) {
    	return classMap.getLocalName();
    }
    
    public Collection classMapNames() {
    	return this.classMapInventoryBridges.keySet();
    }
    
    public Model classMapInventory(String classMapName) {
    	List inventoryBridges = (List) this.classMapInventoryBridges.get(classMapName);
    	if (inventoryBridges == null) {
    		return null;
    	}
    	Model result = ModelFactory.createDefaultModel();
    	result.setNsPrefixes(this.getPrefixMapping());
    	result.getGraph().getBulkUpdateHandler().add(
    			new FindQuery(Triple.ANY, inventoryBridges).iterator());
    	return result;
    }

    public Collection classMapNamesForResource(Node resource) {
    	if (!resource.isURI()) {
    		return Collections.EMPTY_LIST;
    	}
    	List results = new ArrayList();
    	Iterator it = this.classMapNodeMakers.entrySet().iterator();
    	while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			String classMapName = (String) entry.getKey();
			NodeMaker nodeMaker = (NodeMaker) entry.getValue();
			if (!nodeMaker.selectNode(resource, MutableRelation.DUMMY).equals(NodeMaker.EMPTY)) {
				results.add(classMapName);
			}
		}
    	return results;
    }
}