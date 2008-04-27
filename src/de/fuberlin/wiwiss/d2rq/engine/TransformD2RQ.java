package de.fuberlin.wiwiss.d2rq.engine;

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
			return new OpNull();
		}
		if (nodeRelations.size() == 1) {
			return new OpD2RQ(opBGP, (NodeRelation) nodeRelations.iterator().next());
		}
		Iterator it = nodeRelations.iterator();
		Op tree = null;
		while (it.hasNext()) {
			NodeRelation nodeRelation = (NodeRelation) it.next();
			Op op = new OpD2RQ(null, nodeRelation);
			if (tree == null) {
				tree = op;
			} else {
				tree = new OpUnion(tree, op);
			}
		}
		return tree;
	}
}
