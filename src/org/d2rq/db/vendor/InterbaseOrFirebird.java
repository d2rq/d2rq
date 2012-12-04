package org.d2rq.db.vendor;


public class InterbaseOrFirebird extends SQL92 {

	public InterbaseOrFirebird() {
		super(false);
	}
	
	public String getTrueTable() {
		return "RDB$DATABASE";
	}
}
