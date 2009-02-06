package de.fuberlin.wiwiss.d2rq.engine;

import java.util.Collection;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.main.OpExtMain;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;

public class OpD2RQ extends OpExtMain {
	private final OpBGP original;
	private final Relation relation;
	private final Collection bindingMakers;
	
	public OpD2RQ(OpBGP original, Relation relation, Collection bindingMakers) {
		this.original = original;
		this.relation = relation;
		this.bindingMakers = bindingMakers;
	}
	
	public QueryIterator eval(QueryIterator input, ExecutionContext execCxt) 
	{
		// I'm not sure what the semantics of input is; so I just close
		// and ignore it
//		input.close();
		
//		return RelationToBindingsIterator.create(relation, bindingMakers, execCxt);
		return RelationToBindingsIterator2.create(relation, bindingMakers, input, execCxt);
	}

	public boolean equalTo(Op other, NodeIsomorphismMap labelMap) {
		if (!(other instanceof OpD2RQ)) return false;
		OpD2RQ other2 = (OpD2RQ) other;
        return original.getPattern().equiv(other2.original.getPattern(), labelMap);
	}

	public int hashCode() {
		return original.hashCode() ^ relation.hashCode() ^ bindingMakers.hashCode();
	}

	public OpExt copy() {
		return this;	// We are immutable
	}

	public Op effectiveOp() {
		return original;
	}

	public String getName() {
		return "d2rq";
	}
	
}
