package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Map;

import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType.GenericType;
import de.fuberlin.wiwiss.d2rq.sql.vendor.Vendor;

public class DummyDB extends ConnectedDB {
	private final Vendor vendor;
	private int limit = Database.NO_LIMIT;
	
	public DummyDB() {
		this(Vendor.SQL92);
	}
	
	public DummyDB(final Vendor vendor) {
		super(null, null, null);
		this.vendor = vendor;
	}

	public DummyDB(Map<String,GenericType> overrideColumnTypes) {
		super(null, null, null, false, overrideColumnTypes, Database.NO_LIMIT, Database.NO_FETCH_SIZE, null);
		this.vendor = Vendor.SQL92;
	}
	
	public void setLimit(int newLimit) {
		limit = newLimit;
	}

	@Override
	public Vendor vendor() {
		return vendor;
	}
	
	@Override
	public int limit() {
		return limit;
	}
	
	public boolean equals(Object other) {
		return other instanceof DummyDB;
	}
}
