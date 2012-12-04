package org.d2rq.tmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.d2rq.algebra.NodeRelation;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.nodes.BindingMaker;

public class CompatibleRelationGroup {

	public static Collection<CompatibleRelationGroup> groupNodeRelations(List<? extends NodeRelation> relations) {
		Collection<CompatibleRelationGroup> result = new ArrayList<CompatibleRelationGroup>();
		for (NodeRelation relation: relations) {
			result.add(new CompatibleRelationGroup(relation));
		}
		return result;
	}
	
	private final NodeRelation relation;
	
	public CompatibleRelationGroup(NodeRelation relation) {
		this.relation = relation;
	}
	
	public SQLConnection getSQLConnection() {
		return relation.getSQLConnection();
	}
	
	public DatabaseOp baseRelation() {
		return relation.getBaseTabular();
	}
	
	public Collection<BindingMaker> bindingMakers() {
		return Collections.singleton(relation.getBindingMaker());
	}
}
