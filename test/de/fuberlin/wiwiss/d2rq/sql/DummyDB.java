package de.fuberlin.wiwiss.d2rq.sql;

import java.util.HashMap;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.map.Database;

public class DummyDB extends ConnectedDB {
	private final String type;
	private final Map<Attribute,SQLDataType> columnTypes = new HashMap<Attribute,SQLDataType>();
	private int limit = Database.NO_LIMIT;
	
	public DummyDB() {
		this(ConnectedDB.Other);
	}
	
	public DummyDB(final String type) {
		super(null, null, null);
		this.type = type;
	}

	public void setColumnType(Attribute attribute, SQLDataType type) {
		columnTypes.put(attribute, type);
	}

	public void setLimit(int newLimit) {
		limit = newLimit;
	}

	protected String getDatabaseProductType() {
		return type;
	}

	public SQLDataType columnType(Attribute attribute) {
		if (columnTypes.containsKey(attribute)) {
			return columnTypes.get(attribute);
		}
		return super.columnType(attribute);
	}
	
	public int limit() {
		return limit;
	}
	
	public boolean equals(Object other) {
		return other instanceof DummyDB;
	}
}
