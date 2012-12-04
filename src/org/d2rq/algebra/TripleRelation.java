package org.d2rq.algebra;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.nodes.BindingMaker;
import org.d2rq.nodes.NodeMaker;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Var;


/**
 * A collection of virtual triples obtained by applying a {@link DatabaseOp} to a
 * database, and applying {@link NodeMaker}s for subject, predicate and object
 * to each result row. This is a simple extension (or rather restriction) of
 * {@link NodeRelation}.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TripleRelation extends NodeRelation {
	public static final Var SUBJECT = Var.alloc("subject");
	public static final Var PREDICATE = Var.alloc("predicate");
	public static final Var OBJECT = Var.alloc("object");

	public static final Set<Var> SPO = 
		new HashSet<Var>(Arrays.asList(new Var[]{SUBJECT, PREDICATE, OBJECT}));
	
	public static TripleRelation fromNodeRelation(NodeRelation relation) {
		if (relation instanceof TripleRelation) return (TripleRelation) relation;
		if (!relation.getBindingMaker().variableNames().equals(SPO)) {
			throw new IllegalArgumentException("Not a TripleRelation header: " + 
					relation.getBindingMaker().variableNames());
		}
		return new TripleRelation(relation.getSQLConnection(), relation.getBaseTabular(), relation.getBindingMaker()); 
	}
	
	public TripleRelation(SQLConnection connection, DatabaseOp baseRelation, 
			final NodeMaker subjectMaker, final NodeMaker predicateMaker, final NodeMaker objectMaker) {
		super(connection, baseRelation, new HashMap<Var,NodeMaker>() {{
			put(SUBJECT, subjectMaker);
			put(PREDICATE, predicateMaker);
			put(OBJECT, objectMaker);
		}});
	}

	public TripleRelation(SQLConnection connection, DatabaseOp baseRelation, BindingMaker bindingMaker) {
		super(connection, baseRelation, bindingMaker);
	}
	
	public TripleRelation orderBy(Var variable, boolean ascending) {
		return fromNodeRelation(NodeRelationUtil.order(this, variable, ascending));
	}

	public TripleRelation limit(int limit) {
		return fromNodeRelation(NodeRelationUtil.limit(this, limit));
	}

	public TripleRelation selectTriple(Triple t) {
		NodeRelation r = this;
		r = NodeRelationUtil.select(r, SUBJECT, t.getSubject());
		r = NodeRelationUtil.select(r, PREDICATE, t.getPredicate());
		r = NodeRelationUtil.select(r, OBJECT, t.getObject());
		r = NodeRelationUtil.project(r, SPO);
		if (t.getObject().isConcrete() && !t.getSubject().isConcrete()) {
		    r = new NodeRelation(getSQLConnection(), 
		    		LimitOp.swapLimits(r.getBaseTabular()), r.getBindingMaker());
		}
		return fromNodeRelation(r);
	}
}