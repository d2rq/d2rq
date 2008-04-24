package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.values.BlankNodeID;
import de.fuberlin.wiwiss.d2rq.values.Pattern;

/**
 * <p>The URI maker rule states that any URI that matches a URI pattern is not contained
 * in a URI column. The reasoning is that lookup of a node in a URI pattern is relatively
 * quick -- often it requires just an integer lookup in a primary key table -- but
 * lookup in URI columns may require multiple full table scans. Since URI lookup is
 * such a common operation, this rule can help a lot by reducing full table scans.</p>
 * 
 * <p>Checking a number of NodeMakers against the rule works like this:</p>
 * 
 * <ol>
 * <li>An URIMakerRuleChecker is created using {@link #createRuleChecker(Node)},</li>
 * <li>node makers are added one by one to the rule checker,</li>
 * <li>as soon as a NodeMaker backed by an URI pattern has matched, subsequent
 *   calls to {@link URIMakerRuleChecker#canMatch(NodeMaker)} will return false
 *   if the argument is backed by a URI column.</li>
 * </ol>
 * 
 * <p>Performance is best when all candidate NodeMakers backed by URI patterns are
 * sent to the rule checker before any NodeMaker backed by a URI column. For this
 * purpose, {@link #sortRDFRelations(Collection)} sorts a collection of
 * RDFRelations in an order that puts URI patterns first.</p>
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: URIMakerRule.java,v 1.2 2008/04/24 17:48:54 cyganiak Exp $
 */
public class URIMakerRule implements Comparator {
	private Map identifierCache = new HashMap();

	public List sortRDFRelations(Collection rdfRelations) {
		ArrayList results = new ArrayList(rdfRelations);
		Collections.sort(results, this);
		return results;
	}

	public URIMakerRuleChecker createRuleChecker(Node node) {
		return new URIMakerRuleChecker(node);
	}
	
	public int compare(Object o1, Object o2) {
		if (!(o1 instanceof RDFRelation) || !(o2 instanceof RDFRelation)) {
			return 0;
		}
		int priority1 = priority((RDFRelation) o1);
		int priority2 = priority((RDFRelation) o2);
		if (priority1 > priority2) {
			return -1;
		}
		if (priority1 < priority2) {
			return 1;
		}
		return 0;
	}
	
	private int priority(RDFRelation relation) {
		int result = 0;
		for (int i = 0; i < 3; i++) {
			URIMakerIdentifier id = uriMakerIdentifier(relation.nodeMaker(i));
			if (id.isURIPattern()) {
				result += 3;
			}
			if (id.isURIColumn()) {
				result -= 1;
			}
		}
		return result;
	}

	private URIMakerIdentifier uriMakerIdentifier(NodeMaker nodeMaker) {
		URIMakerIdentifier cachedIdentifier = (URIMakerIdentifier) this.identifierCache.get(nodeMaker);
		if (cachedIdentifier == null) {
			cachedIdentifier = new URIMakerIdentifier(nodeMaker);
			this.identifierCache.put(nodeMaker, cachedIdentifier);
		}
		return cachedIdentifier;
	}
	
	private class URIMakerIdentifier implements NodeSetFilter {
		private boolean isURIMaker = false;
		private boolean isColumn = false;
		private boolean isPattern = false;
		URIMakerIdentifier(NodeMaker nodeMaker) {
			nodeMaker.describeSelf(this);
		}
		boolean isURIColumn() { return this.isURIMaker && this.isColumn; }
		boolean isURIPattern() { return this.isURIMaker && this.isPattern; }
		public void limitTo(Node node) { }
		public void limitToBlankNodes() { }
		public void limitToEmptySet() { }
		public void limitToLiterals(String language, RDFDatatype datatype) { }
		public void limitToURIs() { this.isURIMaker = true; }
		public void limitValuesToAttribute(Attribute attribute) { this.isColumn = true; }
		public void limitValuesToBlankNodeID(BlankNodeID id) { }
		public void limitValuesToPattern(Pattern pattern) { this.isPattern = true; }
	}
	
	public class URIMakerRuleChecker {
		private Node node;
		private boolean canMatchURIColumn = true;
		public URIMakerRuleChecker(Node node) {
			this.node = node;
		}
		public void addPotentialMatch(NodeMaker nodeMaker) {
			if (node.isURI() 
					&& uriMakerIdentifier(nodeMaker).isURIPattern() 
					&& !nodeMaker.selectNode(
							node, new MutableRelation(Relation.TRUE)).equals(
									NodeMaker.EMPTY)) {
				this.canMatchURIColumn = false;
			}
		}
		public boolean canMatch(NodeMaker nodeMaker) {
			return this.canMatchURIColumn || !uriMakerIdentifier(nodeMaker).isURIColumn();
		}
	}
}
