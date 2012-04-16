package de.fuberlin.wiwiss.d2rq.sql;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType.GenericType;
import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class DummyDB extends ConnectedDB {
	private final Vendor vendor;
	private int limit = Database.NO_LIMIT;
	private Map<Attribute,Boolean> nullability = new HashMap<Attribute,Boolean>();
	
	public DummyDB() {
		this(Vendor.SQL92);
	}
	
	public DummyDB(final Vendor vendor) {
		super(Collections.<String,GenericType>emptyMap(), Database.NO_LIMIT, Database.NO_FETCH_SIZE);
		this.vendor = vendor;
	}
	
	public DummyDB(Map<String,GenericType> overrideColumnTypes) {
		super(overrideColumnTypes, Database.NO_LIMIT, Database.NO_FETCH_SIZE);
		this.vendor = Vendor.SQL92;
	}
	

	@Override
	public void init() {
		// TODO Auto-generated method stub
	}

	@Override
	public Connection connection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close(Connection c) {
		// TODO Auto-generated method stub
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}
	
	public void setLimit(int newLimit) {
		limit = newLimit;
	}

	public void setNullable(Attribute column, boolean flag) {
		nullability.put(column, flag);
	}
	
	@Override
	public Vendor vendor() {
		return vendor;
	}
	
	@Override
	public int limit() {
		return limit;
	}
	
	@Override
	public boolean isNullable(Attribute column) {
		if (!nullability.containsKey(column)) return true;
		return nullability.get(column);
	}
	
	public boolean equals(Object other) {
		return other instanceof DummyDB;
	}
}
