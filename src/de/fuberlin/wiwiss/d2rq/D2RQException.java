package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.shared.JenaException;

/**
 * Exception used to signal most D2RQ errors.
 *
 * @author Chris Bizer chris@bizer.de
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
//	public static final int PROPERTYBRIDGE_DUPLICATE_VALUE = 23;
	public static final int PROPERTYBRIDGE_CONFLICTING_DATABASES = 24;
	public static final int PROPERTYBRIDGE_LANG_AND_DATATYPE = 25;
	public static final int PROPERTYBRIDGE_NONLITERAL_WITH_DATATYPE = 26;
	public static final int PROPERTYBRIDGE_NONLITERAL_WITH_LANG = 27;
	public static final int TRANSLATIONTABLE_TRANSLATION_AND_JAVACLASS = 28;
	public static final int TRANSLATIONTABLE_TRANSLATION_AND_HREF = 29;
	public static final int TRANSLATIONTABLE_HREF_AND_JAVACLASS = 30;
	public static final int TRANSLATIONTABLE_DUPLICATE_JAVACLASS = 31;
	public static final int TRANSLATIONTABLE_DUPLICATE_HREF = 32;
	public static final int TRANSLATION_MISSING_DBVALUE = 33;
	public static final int TRANSLATION_MISSING_RDFVALUE = 34;
//	public static final int DATABASE_DUPLICATE_ODBCDSN = 35;
	public static final int DATABASE_DUPLICATE_JDBCDSN = 35;
	public static final int DATABASE_DUPLICATE_JDBCDRIVER = 36;
	public static final int DATABASE_MISSING_JDBCDRIVER = 37;
	public static final int DATABASE_DUPLICATE_USERNAME = 38;
	public static final int DATABASE_DUPLICATE_PASSWORD = 39;
//	public static final int DATABASE_ODBC_WITH_JDBC = 40;
//	public static final int DATABASE_ODBC_WITH_JDBCDRIVER = 41;
	public static final int DATABASE_JDBCDRIVER_CLASS_NOT_FOUND = 42;
	public static final int D2RQ_SQLEXCEPTION = 43;
	public static final int SQL_INVALID_RELATIONNAME = 44;
	public static final int SQL_INVALID_ATTRIBUTENAME = 45;
	public static final int SQL_INVALID_ALIAS = 46;
	public static final int SQL_INVALID_JOIN = 47;
	public static final int MAPPING_RESOURCE_INSTEADOF_LITERAL = 48;
	public static final int MAPPING_LITERAL_INSTEADOF_RESOURCE = 49;
	public static final int RESOURCEMAP_ILLEGAL_URIPATTERN = 50;
	public static final int DATABASE_MISSING_DSN = 51;
	public static final int MUST_BE_NUMERIC = 52;
	public static final int RESOURCEMAP_DUPLICATE_CONSTANTVALUE = 53;
	public static final int CLASSMAP_INVALID_CONSTANTVALUE = 53;
	public static final int D2RQ_DB_CONNECTION_FAILED = 54;
	public static final int MAPPING_UNKNOWN_D2RQ_PROPERTY = 55;
	public static final int MAPPING_UNKNOWN_D2RQ_CLASS = 56;
	public static final int PROPERTYBRIDGE_DUPLICATE_SQL_EXPRESSION = 57;
	public static final int MAPPING_TYPECONFLICT = 58;
	public static final int PROPERTYBRIDGE_DUPLICATE_URI_SQL_EXPRESSION = 59;
	public static final int PROPERTYBRIDGE_DUPLICATE_LIMIT = 60;
	public static final int PROPERTYBRIDGE_DUPLICATE_LIMITINVERSE = 61;
	public static final int PROPERTYBRIDGE_DUPLICATE_ORDER = 62;
	public static final int PROPERTYBRIDGE_DUPLICATE_ORDERDESC = 63;
	public static final int DATABASE_ALREADY_CONNECTED = 64;
	public static final int DOWNLOADMAP_DUPLICATE_BELONGSTOCLASSMAP = 65;
	public static final int DOWNLOADMAP_INVALID_BELONGSTOCLASSMAP = 66;
	public static final int DOWNLOADMAP_DUPLICATE_MEDIATYPE = 67;
	public static final int DOWNLOADMAP_DUPLICATE_CONTENTCOLUMN = 68;
	public static final int DOWNLOADMAP_INVALID_CONSTANTVALUE = 69;
	public static final int DOWNLOADMAP_NO_CONTENTCOLUMN = 70;
	public static final int DOWNLOADMAP_DUPLICATE_DATABASE = 71;
	public static final int DOWNLOADMAP_INVALID_DATABASE = 72;
	public static final int DOWNLOADMAP_NO_DATASTORAGE = 73;
	public static final int DATATYPE_UNMAPPABLE = 74;
	public static final int DATATYPE_UNKNOWN = 75;
	public static final int DATABASE_DUPLICATE_STARTUPSCRIPT = 76;
	public static final int MAPPING_TURTLE_SYNTAX = 77;
	public static final int STARTUP_SQL_SCRIPT_ACCESS = 78;
	public static final int STARTUP_SQL_SCRIPT_SYNTAX = 79;
	public static final int DATATYPE_DOES_NOT_SUPPORT_DISTINCT = 80;
	public static final int CONFIG_UNKNOWN_PROPERTY = 81;
	public static final int CONFIG_UNKNOWN_CLASS = 82;
	public static final int STARTUP_BASE_URI_NOT_ABSOLUTE = 83;
	public static final int QUERY_TIMEOUT = 84;
	public static final int PROPERTYBRIDGE_MISSING_PREDICATESPEC = 85;
	public static final int SQL_COLUMN_NOT_FOUND = 86;
	public static final int STARTUP_UNKNOWN_FORMAT = 87;
	
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
		super(message + " (E" + code + ")");
		this.code = code;
	}

	public D2RQException(Throwable cause, int code) {
		super(cause);
		this.code = code;
	}
	
	public D2RQException(String message, Throwable cause, int code) {
		super(message + " (E" + code + ")", cause); 
		this.code = code;
	}
	
	public int errorCode() {
		return this.code;
	}
}

