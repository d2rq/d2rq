package org.d2rq.db.expr;

import java.util.Collections;
import java.util.Set;

import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;


/**
 * A constant-valued expression. The datatype of the constant must be
 * specified. For situations where the datatype is not yet known at
 * constant creation time, a column name, expression, or generic type
 * can be specified instead.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Constant extends Expression {
	private final String value;
	private final Datatyper datatyper;
	
	public static Constant create(String value, final GenericType genericType) {
		return new Constant(value, new Datatyper(genericType) {
			public DataType getDataType(DatabaseOp table, Vendor vendor) {
				return genericType.dataTypeFor(vendor);
			}
		});
	}
	
	public static Constant create(String value, final DataType dataType) {
		return new Constant(value, new Datatyper(dataType) {
			public DataType getDataType(DatabaseOp table, Vendor vendor) {
				return dataType;
			}
		});
	}

	// TODO Apparently not used; delete?
	public static Constant create(String value, ColumnName columnForType) {
		return create(value, new ColumnExpr(columnForType));
	}
	
	// TODO Apparently not used; delete?
	public static Constant create(String value, final Expression expressionForType) {
		return new Constant(value, new Datatyper(expressionForType) {
			public DataType getDataType(DatabaseOp table, Vendor vendor) {
				return expressionForType.getDataType(table, vendor);
			}
		});
	}

	private abstract static class Datatyper {
		private final Object identity;
		Datatyper(Object identity) {
			this.identity = identity;
		}
		abstract DataType getDataType(DatabaseOp table, Vendor vendor);
		public String toString() {
			return identity.toString();
		}
		public boolean equals(Object o) {
			if (!(o instanceof Datatyper)) return false;
			return identity.equals(((Datatyper) o).identity);
		}
		public int hashCode() {
			return identity.hashCode() ^ 4234;
		}
	}
	
	/**
	 * One of the two types must be <code>null</code>
	 */
	private Constant(String value, Datatyper datatyper) {
		this.value = value;
		this.datatyper = datatyper;
	}
	
	public String value() {
		return value;
	}
	
	public Set<ColumnName> getColumns() {
		return Collections.<ColumnName>emptySet();
	}

	public boolean isFalse() {
		// TODO: Check if this is a boolean constant
		return false;
	}

	public boolean isTrue() {
		// TODO: Check if this is a boolean constant
		return false;
	}

	public boolean isConstant() {
		return true;
	}
	
	public boolean isConstantColumn(ColumnName column, boolean constIfTrue, 
			boolean constIfFalse, boolean constIfConstantValue) {
		return false;
	}

	public Expression rename(Renamer columnRenamer) {
		return this;
	}

	public String toSQL(DatabaseOp table, Vendor vendor) {
		return getDataType(table, vendor).toSQLLiteral(value, vendor);
	}

	public DataType getDataType(DatabaseOp table, Vendor vendor) {
		return datatyper.getDataType(table, vendor);
	}
	
	public String toString() {
		return "Constant(" + value + "@" + datatyper + ")";
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Constant)) return false;
		Constant otherConstant = (Constant) other;
		if (!value.equals(otherConstant.value)) return false;
		return datatyper.equals(otherConstant.datatyper);
	}
	
	public int hashCode() {
		return value.hashCode() ^ datatyper.hashCode() ^ 996;
	}
}
