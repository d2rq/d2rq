package de.fuberlin.wiwiss.d2rq.sql;

import java.util.HashMap;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;

public class DummyDB extends ConnectedDB {
	private final String type;
	private final Map columnTypes = new HashMap();
	
	public DummyDB() {
		this(ConnectedDB.Other);
	}
	
	public DummyDB(final String type) {
		super(null, null, null);
		this.type = type;
	}

	public void setColumnType(Attribute attribute, int type) {
		columnTypes.put(attribute, new Integer(type));
	}
	
	public String dbType() {
		return type;
	}

	public int columnType(Attribute attribute) {
		if (columnTypes.containsKey(attribute)) {
			return ((Integer) columnTypes.get(attribute)).intValue();
		}
		return super.columnType(attribute);
	}
	
	public boolean equals(Object other) {
		return other instanceof DummyDB;
	}
}
