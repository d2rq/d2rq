/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/

package de.fuberlin.wiwiss.d2rq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * A D2RQ virtual read-only graph backed by a non-RDF database.
 * 
 * D2RQ is a declarative mapping language for describing mappings between
 * ontologies and relational data models. More information about D2RQ is found
 * at: http://www.wiwiss.fu-berlin.de/suhl/bizer/d2rq/
 * 
 * <p>History:<br>
 * 06-06-2004: Initial version of this class.<br>
 * 08-03-2004: New query algorithm, moved map building to MapParser
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 * 
 * @see de.fuberlin.wiwiss.d2rq.D2RQCapabilities
 */
public class GraphD2RQ extends GraphBase implements Graph {
	static private boolean usingD2RQQueryHandler=false;
	
//	private final ReificationStyle style;
//	private boolean closed = false;
	private Capabilities capabilities = null;

    /** Collection of all PropertyBridges definded in the mapping file */
	private List propertyBridges;

	public static boolean isUsingD2RQQueryHandler() {
		return usingD2RQQueryHandler;
	}
	public static void setUsingD2RQQueryHandler(boolean usingD2RQQueryHandler) {
		GraphD2RQ.usingD2RQQueryHandler = usingD2RQQueryHandler;
	}
	
	public List getPropertyBridges() {
		return propertyBridges;
	}

	
	/**
	 * Creates a new D2RQ graph from a D2RQ mapping file in Notation 3 syntax.
	 * @param mapURL the URL where the mapping file is located
	 * @throws D2RQException on error in the mapping file
	 */
	public GraphD2RQ(String mapURL) throws D2RQException {
		this(mapURL, "N3");
	}

	/**
	 * Creates a new D2RQ graph from a D2RQ mapping file. The second parameter
	 * specifies the RDF syntax of the mapping file. Supported values
	 * are "RDF/XML", "N-TRIPLE" and "N3". 
	 * @param mapURL the URL where the mapping file is located
	 * @param serializationFormat the serialization syntax format
	 * @throws D2RQException on error in the mapping file
	 */
	public GraphD2RQ(String mapURL, String serializationFormat) throws D2RQException {
//		this.style = ReificationStyle.Minimal;
		Model mapModel = getModelFromURL(mapURL, serializationFormat);
		this.initMap(mapModel);
	}

	/**
	 * Creates a new D2RQ graph from a Jena model containing a D2RQ
	 * mapping.
	 * @param mapModel the model containing a D2RQ mapping file
	 * @throws D2RQException on error in the mapping model
	 */
	public GraphD2RQ(Model mapModel) throws D2RQException {
//		this.style = ReificationStyle.Minimal;
		this.initMap(mapModel);
	}

	/**
	 * Enables D2RQ debug messages.
	 */
	public void enableDebug() {
		Logger.instance().setDebug(true);
	}

	private Model getModelFromURL(String mapURL, String serializationFormat) {
		Model result = ModelFactory.createDefaultModel();
		result.read(mapURL, serializationFormat);
		return result;
	}

	private void initMap(Model mapModel) throws D2RQException {
		MapParser parser = new MapParser(mapModel);
		parser.parse();
		this.propertyBridges = sortPropertyBridges(parser.getPropertyBridges());
	}
    
	private List sortPropertyBridges(Collection unsortedBridges) {
		Comparator uriEvaluationOrderComparator = new Comparator() {
			public int compare(Object policy1, Object policy2) {
				return ((PropertyBridge) policy2).getEvaluationPriority() -
					   ((PropertyBridge) policy1).getEvaluationPriority();
			}
		};
		List result = new ArrayList(unsortedBridges);
		Collections.sort(result, uriEvaluationOrderComparator);
		return result;
	}

	/**
	 * Todo: Implement D2RQ query handler
	 * @see com.hp.hpl.jena.graph.Graph#queryHandler
	 */
	public QueryHandler queryHandler() {
		// jg: it would be more efficient to have just one instance per graph
		// on the other hand: new instance guaraties that all information with handler
		// is up to date.
		checkOpen();
		if (usingD2RQQueryHandler) 
			return new D2RQQueryHandler(this);
		return new SimpleQueryHandler(this);
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

	public ExtendedIterator find( TripleMatch m ) {
		checkOpen();
		Triple t = m.asTriple();

		if (Logger.instance().debugEnabled()) {
			Logger.instance().debug("--------------------------------------------");
			Logger.instance().debug("        Find(SPO) Query Pattern");
			Logger.instance().debug("--------------------------------------------");
			Logger.instance().debug("Subject: " + t.getSubject());
			Logger.instance().debug("Predicate: " + t.getPredicate());
			Logger.instance().debug("Object: " + t.getObject());
			Logger.instance().debug("");
		}

		QueryCombiner combiner = new QueryCombiner();
		QueryContext context = new QueryContext();
		Iterator it = this.propertyBridges.iterator();
		while (it.hasNext()) {
			PropertyBridge bridge = (PropertyBridge) it.next();
			if (!bridge.couldFit(t, context)) {
				continue;
			}
			if (Logger.instance().debugEnabled()) {
				Logger.instance().debug("--------------------------------------------");
				Logger.instance().debug("Using property bridge: " + bridge);
			}
			combiner.add(new TripleQuery(bridge,
					t.getSubject(), t.getPredicate(), t.getObject()));
		}
		return combiner.getResultIterator();
    }
	
	static PropertyBridge[] emptyPropertyBridgeArray=new PropertyBridge[0];
	
	// used by D2RQPatternStage
	public ArrayList propertyBridgesForTriple(Triple t) { // PropertyBridge[]
		QueryContext context = new QueryContext();
		Iterator it = this.propertyBridges.iterator();
		ArrayList list=new ArrayList(2);
		while (it.hasNext()) {
			PropertyBridge bridge = (PropertyBridge) it.next();
			if (!bridge.couldFit(t, context)) {
				continue;
			}
			if (Logger.instance().debugEnabled()) {
				Logger.instance().debug("--------------------------------------------");
				Logger.instance().debug("Using property bridge: " + bridge + " for triple " + t );
			}
			list.add(bridge);
		}
		return list;
		// return (PropertyBridge[]) list.toArray(emptyPropertyBridgeArray);
	}	
	
}