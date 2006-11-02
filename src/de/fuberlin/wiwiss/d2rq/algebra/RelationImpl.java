package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

public class RelationImpl implements Relation {
	private ConnectedDB database;
	private AliasMap aliases;
	private Map attributeConditions;
	private Expression condition;
	private Set joinConditions;
	
	public RelationImpl(ConnectedDB database, AliasMap aliases, Map attributeConditions, 
			Expression condition, Set joinConditions) {
		this.database = database;
		this.aliases = aliases;
		this.attributeConditions = attributeConditions;
		this.condition = condition;
		this.joinConditions = joinConditions;
	}

	public ConnectedDB database() {
		return this.database;
	}
	
	public AliasMap aliases() {
		return this.aliases;
	}

	public Map attributeConditions() {
		return this.attributeConditions;
	}

	public Expression condition() {
		return this.condition;
	}

	public Set joinConditions() {
		return this.joinConditions;
	}

	public Relation select(Map newConditions) {
		if (newConditions.isEmpty()) {
			return this;
		}
		Map unified = new HashMap(this.attributeConditions);
		Iterator it = newConditions.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			Attribute attribute = (Attribute) entry.getKey();
			String value = (String) entry.getValue();
			if (this.attributeConditions.containsKey(attribute)) {
				if (value.equals(this.attributeConditions.get(attribute))) {
					continue;
				}
				return Relation.EMPTY;
			}
			unified.put(attribute, value);
		}
		return new RelationImpl(this.database, this.aliases, unified, 
				this.condition, this.joinConditions);
	}
	
	public Relation renameColumns(ColumnRenamer renames) {
		return new RelationImpl(this.database, renames.applyTo(this.aliases),
				renames.applyToMapKeys(this.attributeConditions), 
				renames.applyTo(this.condition), renames.applyToJoinSet(this.joinConditions));
	}
}
