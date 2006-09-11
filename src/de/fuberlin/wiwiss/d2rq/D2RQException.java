package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.shared.JenaException;

/**
 * Exception used to signal most D2RQ errors.
 *
 * @author Chris Bizer chris@bizer.de
 * @version $Id: D2RQException.java,v 1.3 2006/09/11 22:29:20 cyganiak Exp $
 */

public class D2RQException extends JenaException {
	public static final int UNSPECIFIED = 0;
	public static final int MAPPING_NO_DATABASE = 1;
	public static final int CLASSMAP_DUPLICATE_DATABASE = 2;
	public static final int CLASSMAP_NO_DATABASE = 3;
	public static final int CLASSMAP_INVALID_DATABASE = 4;
	public static final int CLASSMAP_NO_PROPERTYBRIDGES = 5;
	public static final int RESOURCEMAP_DUPLICATE_BNODEIDCOLUMNS = 6;
	public static final int RESOURCEMAP_DUPLICATE_URICOLUMN = 7;
	public static final int RESOURCEMAP_DUPLICATE_URIPATTERN = 8;
	public static final int RESOURCEMAP_ILLEGAL_CONTAINSDUPLICATE = 9;
	public static final int RESOURCEMAP_MISSING_PRIMARYSPEC = 10;
	public static final int RESOURCEMAP_DUPLICATE_PRIMARYSPEC = 11;
	public static final int RESOURCEMAP_DUPLICATE_TRANSLATEWITH = 12;
	public static final int RESOURCEMAP_INVALID_TRANSLATEWITH = 13;
	public static final int PROPERTYBRIDGE_DUPLICATE_BELONGSTOCLASSMAP = 14;
	public static final int PROPERTYBRIDGE_INVALID_BELONGSTOCLASSMAP = 15;
	public static final int PROPERTYBRIDGE_DUPLICATE_COLUMN = 16;
	public static final int PROPERTYBRIDGE_DUPLICATE_PATTERN = 17;
	public static final int PROPERTYBRIDGE_DUPLICATE_DATATYPE = 18;
	public static final int PROPERTYBRIDGE_DUPLICATE_LANG = 19;
	public static final int PROPERTYBRIDGE_DUPLICATE_REFERSTOCLASSMAP = 20;
	public static final int PROPERTYBRIDGE_INVALID_REFERSTOCLASSMAP = 21;
	public static final int PROPERTYBRIDGE_DUPLICATE_VALUEMAXLENGTH = 22;
	public static final int PROPERTYBRIDGE_DUPLICATE_VALUE = 23;
	public static final int PROPERTYBRIDGE_CONFLICTING_DATABASES = 24;
	public static final int PROPERTYBRIDGE_LANG_AND_DATATYPE = 25;
	public static final int PROPERTYBRIDGE_NONLITERAL_WITH_DATATYPE = 26;
	public static final int PROPERTYBRIDGE_NONLITERAL_WITH_LANG = 27;
	
	private int code;
	
	public D2RQException(String message) {
		this(message, UNSPECIFIED);
	}

	public D2RQException(Throwable cause) {
		this(cause, UNSPECIFIED);
	}
	
	public D2RQException(String message, Throwable cause) {
		this(message, cause, UNSPECIFIED); 
	}

	public D2RQException(String message, int code) {
		super(message);
		this.code = code;
	}

	public D2RQException(Throwable cause, int code) {
		super(cause);
		this.code = code;
	}
	
	public D2RQException(String message, Throwable cause, int code) {
		super(message, cause); 
		this.code = code;
	}
	
	public int errorCode() {
		return this.code;
	}
}

