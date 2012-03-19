package de.fuberlin.wiwiss.d2rq.dbschema;

/**
 * Represents a column type obtained from {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} 
 * in both integer and string forms.
 * 
 * TODO: Merge with ConnectedDB.ColumnType
 *
 * @author Christian Becker <http://beckr.org#chris>
 */
public class ColumnType {
	
	/**
	 * Constant indicating an unmappable column type. A column type is
	 * unmappable if no reasonable RDF literal representation of its
	 * value is known.
	 */
	public final static String UNMAPPABLE = "UNMAPPABLE";
	
	private final int typeId;
	private final String typeName;
	private final int size;
	
	ColumnType(int typeId, String typeName, int size) {
		this.typeId = typeId;
		this.typeName = typeName;
		this.size = size;
	}
	
	ColumnType(int typeId, String typeName) {
		this(typeId, typeName, 0);
	}
	
	public int typeId() {
		return this.typeId;
	}

	public String typeName() {
		return this.typeName;
	}
	
	/**
	 * @return Size of the datatype (in characters, bits, etc.), or 0 if not applicable
	 */
	public int size() {
		return this.size;
	}
	
	@Override
	public String toString() {
		return "ColumnType(" + typeId + "=" + typeName + (size == 0 ? "" : "(" + size + ")") + ")";
	}
}	