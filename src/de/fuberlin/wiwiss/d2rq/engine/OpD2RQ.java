package de.fuberlin.wiwiss.d2rq.engine;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.main.OpExtMain;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

public class OpD2RQ extends OpExtMain {
	private final OpBGP original;
	private final NodeRelation replacement;
	
	public OpD2RQ(OpBGP original, NodeRelation replacement) {
		this.original = original;
		this.replacement = replacement;
	}
	
	public QueryIterator eval(QueryIterator input, ExecutionContext execCxt) {
		// TODO @@@ Auto-generated method stub
		return null;
	}

	public boolean equalTo(Op other, NodeIsomorphismMap labelMap) {
		if (!(other instanceof OpD2RQ)) return false;
		OpD2RQ other2 = (OpD2RQ) other;
        return original.getPattern().equiv(other2.original.getPattern(), labelMap);
	}

	public int hashCode() {
		return original.hashCode() ^ replacement.hashCode();
	}

	public OpExt copy() {
		return this;	// We are immutable
	}

	public Op effectiveOp() {
		return original;
	}

	public String getName() {
		return "D2RQ";
	}
}
