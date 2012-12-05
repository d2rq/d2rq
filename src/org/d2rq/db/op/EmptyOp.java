package org.d2rq.db.op;

import org.d2rq.db.op.util.OpUtil;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.vendor.Vendor;


/**
 * A {@link DatabaseOp} that is known to be empty (has zero rows).
 * Wraps another {@link DatabaseOp} that provides the column metadata
 * and {@link Vendor}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class EmptyOp extends DatabaseOp.Wrapper {

	public static final EmptyOp NO_COLUMNS = new EmptyOp(DatabaseOp.TRUE);
	
	public static DatabaseOp create(DatabaseOp original) {
		if (OpUtil.isEmpty(original)) return original;
		return new EmptyOp(original);
	}
	
	private EmptyOp(DatabaseOp original) {
		super(original);
	}

	/**
	 * @return <code>false</code> as the table is known to be empty 
	 */
	@Override
	public boolean isNullable(ColumnName column) {
		return false;
	}

	public void accept(OpVisitor visitor) {
		if (visitor.visitEnter(this)) {
			getWrapped().accept(visitor);
		}
		visitor.visitLeave(this);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof EmptyOp)) return false;
		EmptyOp other = (EmptyOp) o;
		return getWrapped().equals(other.getWrapped());
	}

	@Override
	public int hashCode() {
		return getWrapped().hashCode() ^ 235;
	}

	@Override
	public String toString() {
		return "Empty(" + getWrapped() + ")";
	}
	
	
}
