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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.algebra.RelationalOperators;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.find.FindQuery;
import de.fuberlin.wiwiss.d2rq.find.TripleQueryIter;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.nodes.FixedNodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.vocab.SKOS;

/**
 * Provides access to the contents of a d2rq:ClassMap in various
 * ways.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ClassMapLister {
	private static final Log log = LogFactory.getLog(ClassMapLister.class);

	private final Mapping mapping;

	private Map<String,List<TripleRelation>> classMapInventoryBridges = new HashMap<String,List<TripleRelation>>();
	private Map<String,NodeMaker> classMapNodeMakers = new HashMap<String,NodeMaker>();

	public ClassMapLister(Mapping mapping) {
		this.mapping = mapping;
		groupTripleRelationsByClassMap();
	}

	private void groupTripleRelationsByClassMap() {
		if (!classMapInventoryBridges.isEmpty() || !classMapNodeMakers.isEmpty()) return;
		for (Resource classMapResource: mapping.classMapResources()) {
			NodeMaker resourceMaker = this.mapping.classMap(classMapResource).nodeMaker();
			Node classMap = classMapResource.asNode();
			this.classMapNodeMakers.put(toClassMapName(classMap), resourceMaker);
			List<TripleRelation> inventoryBridges = new ArrayList<TripleRelation>();
			for (TripleRelation bridge: mapping.classMap(classMapResource).compiledPropertyBridges()) {
				bridge = bridge.orderBy(TripleRelation.SUBJECT, true);
				if (bridge.selectTriple(new Triple(Node.ANY, RDF.Nodes.type, Node.ANY)) != null) {
					inventoryBridges.add(bridge);
				}
				// TODO The list of label properties is redundantly specified in PageServlet
				if (bridge.selectTriple(new Triple(Node.ANY, RDFS.label.asNode(), Node.ANY)) != null) {
					inventoryBridges.add(bridge);
				} else if (bridge.selectTriple(new Triple(Node.ANY, SKOS.prefLabel.asNode(), Node.ANY)) != null) {
					inventoryBridges.add(bridge);
				} else if (bridge.selectTriple(new Triple(Node.ANY, DC.title.asNode(), Node.ANY)) != null) {
					inventoryBridges.add(bridge);					
				} else if (bridge.selectTriple(new Triple(Node.ANY, DCTerms.title.asNode(), Node.ANY)) != null) {
					inventoryBridges.add(bridge);					
				} else if (bridge.selectTriple(new Triple(Node.ANY, FOAF.name.asNode(), Node.ANY)) != null) {
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
		return classMapInventory(classMapName, Relation.NO_LIMIT);
	}
	
	public Model classMapInventory(String classMapName, int limitPerClassMap) {
		log.info("Listing class map: " + classMapName);
		List<TripleRelation> inventoryBridges = classMapInventoryBridges.get(classMapName);
		if (inventoryBridges == null) {
			return null;
		}
		Model result = ModelFactory.createDefaultModel();
		result.setNsPrefixes(mapping.getPrefixMapping());
		FindQuery query = new FindQuery(Triple.ANY, inventoryBridges, limitPerClassMap, null);
		result.getGraph().getBulkUpdateHandler().add(TripleQueryIter.create(query.iterator()));
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
}
