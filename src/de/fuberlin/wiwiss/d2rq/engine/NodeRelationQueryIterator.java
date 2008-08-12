package de.fuberlin.wiwiss.d2rq.engine;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.Binding0;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterNullIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterSingleton;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIteratorBase;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.util.IndentedWriter;
import com.hp.hpl.jena.sparql.util.Utils;

import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

public class NodeRelationQueryIterator extends QueryIteratorBase {
	
	public static QueryIterator createQueryIterator(NodeRelation relation, ExecutionContext context) {
		if (relation.baseRelation().condition().isFalse()) {
			return new QueryIterNullIterator(context);
		}
		if (relation.baseRelation().isTrivial()) {
			return new QueryIterSingleton(new Binding0(), context);
		}
		return new NodeRelationQueryIterator(relation);
	}
	
	private final NodeRelation relation;
	private final QueryExecutionIterator wrapped;
	private Binding nextBinding = null;
	
	private NodeRelationQueryIterator(NodeRelation relation) {
		this.relation = relation;
		SelectStatementBuilder sql = new SelectStatementBuilder(relation.baseRelation());
		wrapped = new QueryExecutionIterator(sql.getSQLStatement(), sql.getColumnSpecs(), relation.baseRelation().database());
	}
	
	public boolean hasNextBinding() {
		while (nextBinding == null && wrapped.hasNext()) {
			Binding binding = toBinding(wrapped.nextRow());
			if (binding == null) continue;
			nextBinding = binding;
		}
		return nextBinding != null;
	}
	
	public Binding moveToNextBinding() {
		Binding result = nextBinding;
		nextBinding = null;
		return result;
	}

	public void closeIterator() {
		wrapped.close();
	}
	
	private Binding toBinding(ResultRow row) {
		BindingMap result = new BindingMap();
		Iterator it = relation.variableNames().iterator();
		while (it.hasNext()) {
			String variableName = (String) it.next();
			Node node = relation.nodeMaker(variableName).makeNode(row);
			if (node == null) {
				return null;
			}
			result.add(Var.alloc(variableName), node);
		}
		return result;
	}
	
	public void output(IndentedWriter out, SerializationContext cxt) {
		out.print(Utils.className(this));
	}
}
