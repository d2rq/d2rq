package de.fuberlin.wiwiss.d2rq.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.algebra.CompatibleRelationGroup;

public class TransformD2RQ extends TransformCopy {
	private final Log log = LogFactory.getLog(TransformD2RQ.class);
	
	private GraphD2RQ graph;
	
	public TransformD2RQ(GraphD2RQ graph) {
		this.graph = graph;
	}
	
	public Op transform(OpBGP opBGP) {
		log.trace(opBGP);
		List nodeRelations = new GraphPatternTranslator(
				opBGP.getPattern().getList(),
				graph.tripleRelations()).translate();
		if (nodeRelations.isEmpty()) {
			return OpNull.create();
		}
		if (nodeRelations.size() == 1) {
			NodeRelation nodeRelation = (NodeRelation) nodeRelations.iterator().next();
			return new OpD2RQ(opBGP, 
					nodeRelation.baseRelation(),
					Collections.singleton(new BindingMaker(nodeRelation)));
		}
		Collection compatibleGroups = CompatibleRelationGroup.groupNodeRelations(nodeRelations);
		Iterator it = compatibleGroups.iterator();
		Op tree = null;
		while (it.hasNext()) {
			CompatibleRelationGroup group = (CompatibleRelationGroup) it.next();
			Op op = new OpD2RQ(null, group.baseRelation(), group.bindingMakers());
			if (tree == null) {
				tree = op;
			} else {
				tree = new OpUnion(tree, op);
			}
		}
		return tree;
	}
}
