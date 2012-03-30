package de.fuberlin.wiwiss.d2rq.engine;

import org.openjena.atlas.io.IndentedWriter;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

import de.fuberlin.wiwiss.d2rq.algebra.NodeRelation;

/**
 * An {@link Op} that wraps a {@link NodeRelation}.
 *  
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class OpTableSQL extends OpExt {
	
	/**
	 * Creates a new OpTableSQL, or a simpler Op if optimizations
	 * are possible.
	 */
	public static Op create(NodeRelation table) {
		if (table.baseRelation().condition().isFalse()) {
			return OpNull.create();
		}
		return new OpTableSQL(table);
	}
	
	private final NodeRelation table;
	
	public OpTableSQL(NodeRelation table) {
		super("sql");
		this.table = table;
	}
	
	public NodeRelation table() {
		return table;
	}
	
	@Override
	public QueryIterator eval(QueryIterator input, final ExecutionContext execCxt) {
		return new QueryIterRepeatApply(input, execCxt) {
			@Override
			protected QueryIterator nextStage(Binding binding) {
				return QueryIterTableSQL.create(table.extendWith(binding), execCxt);
			}
		};
	}

	@Override
	public Op effectiveOp() {
		return OpTable.unit();
	}
	
	@Override
	public void outputArgs(IndentedWriter out, SerializationContext sCxt) {
		out.println(table);
	}

	@Override
	public int hashCode() {
		return 72345643 ^ table.hashCode();
	}

	@Override
	public boolean equalTo(Op other, NodeIsomorphismMap labelMap) {
		if (!(other instanceof OpTableSQL)) return false;
		return ((OpTableSQL) other).table.equals(table);
	}
}
