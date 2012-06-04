package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Collections;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType;
import de.fuberlin.wiwiss.d2rq.sql.types.DataType.GenericType;

/**
 * A constant-valued expression.
 * 
 * This class currently doesn't track
 * its type (Is the constant a number or string, for example?).
 * Since we need to know the type when writing the constant to SQL,
 * we keep a reference to an attribute around. The constant is assumed
 * to have the same type as that attribute. This is an ugly hack.
 * 
 * TODO Should have a {@link DataType} instead of the silly column reference
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Constant extends Expression {
	private final String value;
	private final Attribute attributeForTrackingType;
	
	public Constant(String value) {
		this(value, null);
	}
	
	public Constant(String value, Attribute attributeForTrackingType) {
		this.value = value;
		this.attributeForTrackingType = attributeForTrackingType;
	}
	
	public String value() {
		return value;
	}
	
	public Set<Attribute> attributes() {
		return Collections.<Attribute>emptySet();
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression renameAttributes(ColumnRenamer columnRenamer) {
		if (attributeForTrackingType == null) {
			return this;
		}
		return new Constant(value, columnRenamer.applyTo(attributeForTrackingType));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		if (attributeForTrackingType == null) {
			// TODO: This is an unsafe assumption
			return GenericType.CHARACTER.dataTypeFor(database.vendor()).toSQLLiteral(value);
		}
		return database.columnType(
				aliases.originalOf(attributeForTrackingType)).toSQLLiteral(value);
	}

	public String toString() {
		if (attributeForTrackingType == null) {
			return "Constant(" + value + ")";
		}
		return "Constant(" + value + "@" + attributeForTrackingType.qualifiedName() +")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Constant)) return false;
		Constant otherConstant = (Constant) other;
		if (!value.equals(otherConstant.value)) return false;
		if (attributeForTrackingType == null) {
			return otherConstant.attributeForTrackingType == null;
		}
		return attributeForTrackingType.equals(otherConstant.attributeForTrackingType);
	}
	
	public int hashCode() {
		if (attributeForTrackingType == null) {
			return value.hashCode();
		}
		return value.hashCode() ^ attributeForTrackingType.hashCode();
	}
}
