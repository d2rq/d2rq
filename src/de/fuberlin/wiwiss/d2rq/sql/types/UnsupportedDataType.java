package de.fuberlin.wiwiss.d2rq.sql.types;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.fuberlin.wiwiss.d2rq.D2RQException;

public class UnsupportedDataType extends DataType {
	private final int jdbcType;
	public UnsupportedDataType(int jdbcType, String name) {
		super(null, name);
		this.jdbcType = jdbcType;
	}
	@Override
	public boolean isUnsupported() {
		return true;
	}
	@Override
	public String rdfType() {
		return null;
	}
	@Override
	public String toSQLLiteral(String value) {
		throw new D2RQException("Attempted toSQLLiteral('" + value + 
				"') on a column of a datatype that cannot be mapped to RDF", 
				D2RQException.DATATYPE_UNMAPPABLE);
	}
	@Override
	public String value(ResultSet resultSet, int column) throws SQLException {
		throw new D2RQException("Attempted to get value of a datatype that cannot be mapped to RDF", 
				D2RQException.DATATYPE_UNMAPPABLE);
	}
	@Override
	public String toString() {
		return super.toString() + "{jdbcType:" + jdbcType + ",typeName:'" + name() + "'}";
	}
}