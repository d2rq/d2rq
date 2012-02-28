package de.fuberlin.wiwiss.d2rq.engine;

import java.util.Collection;

import org.openjena.atlas.io.IndentedWriter;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.sse.writers.WriterOp;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.optimizer.iterators.RelationToBindingsD2RQIterator;

public class OpD2RQ extends OpExt
{
	private static final String tagD2RQ = "d2rq"; 	
	protected final OpBGP original;
	protected final Relation relation;
	protected final Collection bindingMakers;
	
	public OpD2RQ(OpBGP original, Relation relation, Collection bindingMakers) 
	{
		super(tagD2RQ);
		this.original = original;
		this.relation = relation;
		this.bindingMakers = bindingMakers;
	}
	
	public QueryIterator eval(QueryIterator input, ExecutionContext execCxt) 
	{
		return RelationToBindingsD2RQIterator.create(relation, bindingMakers, input, execCxt);
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

	/**
	 * used for linking 2 OpD2RQs with an left-join
	 */
	public Relation getRelation() {
		return relation;
	}

	/**
	 * used for linking 2 OpD2RQs with an left-join
	 */
	public Collection getBindingMakers() 
	{
		return bindingMakers;
	}

	public String getSubTag() {
		return "d2rq";
	}

	@Override
	public void outputArgs(IndentedWriter out,
			SerializationContext sCxt) {
		int line = out.getRow() ;
		WriterOp.output(out, this, sCxt) ;
		if ( line != out.getRow() )
			out.ensureStartOfLine() ;
	}	
}
