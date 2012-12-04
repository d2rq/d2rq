package org.d2rq.algebra;

import java.util.Map;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.nodes.BindingMaker;
import org.d2rq.nodes.NodeMaker;

import com.hp.hpl.jena.sparql.core.Var;


/**
 * A {@link DatabaseOp} associated with a number of named {@link NodeMaker}s.
 * 
 * TODO: Rename to NodeTabular?
 * FIXME: Looks like the condition on a provided BindingMaker is sometimes ignored
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NodeRelation {
	
	public static NodeRelation createEmpty(NodeRelation r) {
		return new NodeRelation(r.getSQLConnection(), 
				EmptyOp.create(r.getBaseTabular()), 
				r.getBindingMaker());
	}
	
	private final SQLConnection sqlConnection;
	private final DatabaseOp base;
	private final BindingMaker bindingMaker;
	
	public NodeRelation(SQLConnection connection, DatabaseOp base, Map<Var,NodeMaker> nodeMakers) {
		this(connection, base, new BindingMaker(nodeMakers));
	}
	
	public NodeRelation(SQLConnection connection, DatabaseOp base, BindingMaker bindingMaker) {
		this.sqlConnection = connection;
		this.base = base;
		this.bindingMaker = bindingMaker;
	}
	
	public SQLConnection getSQLConnection() {
		return sqlConnection;
		
	}
	public DatabaseOp getBaseTabular() {
		return base;
	}
	
	public BindingMaker getBindingMaker() {
		return bindingMaker;
	}
	
	public NodeMaker nodeMaker(Var variable) {
		return bindingMaker.get(variable);
	}

	public String toString() {
		StringBuffer result = new StringBuffer("NodeRelation(");
		result.append(base);
		result.append(", ");
		result.append(bindingMaker);
		result.append(")");
		return result.toString();
	}
}
