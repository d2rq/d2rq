package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Collections;
import java.util.Set;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * A constant-valued expression.
 * 
 * This class currently doesn't track
 * its type (Is the constant a number or string, for example?).
 * Since we need to know the type when writing the constant to SQL,
 * we keep a reference to an attribute around. The constant is assumed
 * to have the same type as that attribute. This is an ugly hack.
 * 
 * TODO Should have a SQL type code instead of the stupid column reference
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Constant.java,v 1.1 2008/04/24 17:48:53 cyganiak Exp $
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
	
	public Set columns() {
		return Collections.EMPTY_SET;
	}

	public boolean isFalse() {
		return false;
	}

	public boolean isTrue() {
		return false;
	}

	public Expression renameColumns(ColumnRenamer columnRenamer) {
		if (attributeForTrackingType == null) {
			return this;
		}
		return new Constant(value, columnRenamer.applyTo(attributeForTrackingType));
	}

	public String toSQL(ConnectedDB database, AliasMap aliases) {
		if (attributeForTrackingType == null) {
			// TODO: This is an unsafe assumption
			return database.quoteValue(value, ConnectedDB.TEXT_COLUMN);
		}
		return database.quoteValue(value, aliases.originalOf(attributeForTrackingType));
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
		return value.equals(otherConstant.value) && 
				((attributeForTrackingType == null && otherConstant.attributeForTrackingType == null)
				|| attributeForTrackingType.equals(otherConstant.attributeForTrackingType));
	}
	
	public int hashCode() {
		if (attributeForTrackingType == null) {
			return value.hashCode();
		}
		return value.hashCode() ^ attributeForTrackingType.hashCode();
	}
}
