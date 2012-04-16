package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openjena.atlas.io.IndentedWriter;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterConcat;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.sse.writers.WriterOp;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

import de.fuberlin.wiwiss.d2rq.algebra.CompatibleRelationGroup;
import de.fuberlin.wiwiss.d2rq.algebra.NodeRelation;

/**
 * An {@link Op} that wraps a union of multiple {@link NodeRelation}s.
 * 
 * This is typically, but not necessarily, the result of matching a BGP
 * against a D2RQ-mapped database.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class OpUnionTableSQL extends OpExt {
	
	/**
	 * Creates a new instance from a collection of
	 * {@link NodeRelation}s, or a simpler equivalent Op
	 * if optimizations are possible.
	 */
	public static Op create(Collection<NodeRelation> tables) {
		Collection<OpTableSQL> nonEmpty = new ArrayList<OpTableSQL>();
		for (NodeRelation table: tables) {
			if (table.baseRelation().condition().isFalse()) continue;
			nonEmpty.add(new OpTableSQL(table));
		}
		if (nonEmpty.isEmpty()) {
			return OpNull.create();
		}
		return new OpUnionTableSQL(nonEmpty);
	}
	
	private final List<OpTableSQL> tableOps;
	private final Op effectiveOp;
	
	public OpUnionTableSQL(Collection<OpTableSQL> tableOps) {
		this(tableOps, OpTable.unit());
	}
	
	public OpUnionTableSQL(Collection<OpTableSQL> tableOps, Op effectiveOp) {
		super("sqlunion");
		this.tableOps = new ArrayList<OpTableSQL>(tableOps);
		this.effectiveOp = effectiveOp;
	}
	
	@Override
	public QueryIterator eval(QueryIterator input, final ExecutionContext execCxt) {
		return new QueryIterRepeatApply(input, execCxt) {
			@Override
			protected QueryIterator nextStage(Binding binding) {
				QueryIterConcat resultIt = new QueryIterConcat(execCxt);
				Collection<NodeRelation> tables = new ArrayList<NodeRelation>();
				for (OpTableSQL tableOp: tableOps) {
					tables.add(tableOp.table().extendWith(binding));
				}
				for (CompatibleRelationGroup group: CompatibleRelationGroup.groupNodeRelations(tables)) {
					resultIt.add(QueryIterTableSQL.create(group.baseRelation(), group.bindingMakers(), execCxt));
				}
				return resultIt;
			}
		};
	}

	@Override
	public Op effectiveOp() {
		return effectiveOp;
	}
	
	@Override
	public void outputArgs(IndentedWriter out, SerializationContext sCxt) {
		out.println();
		for (OpTableSQL table: tableOps) {
			WriterOp.output(out, table, sCxt);
		}
	}

	@Override
	public int hashCode() {
		return 72345644 ^ tableOps.hashCode();
	}

	@Override
	public boolean equalTo(Op other, NodeIsomorphismMap labelMap) {
		if (!(other instanceof OpUnionTableSQL)) return false;
		return ((OpUnionTableSQL) other).tableOps.equals(tableOps);
	}
}
