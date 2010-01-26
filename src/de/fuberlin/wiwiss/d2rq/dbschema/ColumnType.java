package de.fuberlin.wiwiss.d2rq.dbschema;

/**
 * Represents a column type obtained from {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} 
 * in both integer and string forms.
 *
 * @author Christian Becker <http://beckr.org#chris>
 * @version $Id: ColumnType.java,v 1.2 2010/01/26 16:26:14 fatorange Exp $
 */
public class ColumnType {
	private int typeId;
	private String typeName;

	ColumnType(int typeId, String typeName) {
		this.typeId = typeId;
		this.typeName = typeName;
	}
	
	public int typeId() {
		return this.typeId;
	}

	public String typeName() {
		return this.typeName;
	}
}	