package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Collection;

import org.d2rq.db.schema.Key;

/**
 * Removes duplicates from the wrapped {@link DatabaseOp}.
 */
public class DistinctOp extends DatabaseOp.Wrapper {
	private final Collection<Key> uniqueKeys = new ArrayList<Key>();
	
	public DistinctOp(DatabaseOp wrapped) {
		super(wrapped);
		uniqueKeys.addAll(wrapped.getUniqueKeys());
		uniqueKeys.add(Key.createFromColumns(getColumns()));
	}

	@Override
	public Collection<Key> getUniqueKeys() {
		return uniqueKeys;
	}

	public void accept(OpVisitor visitor) {
		if (visitor.visitEnter(this)) {
			getWrapped().accept(visitor);
		}
		visitor.visitLeave(this);
	}
	
	@Override
	public String toString() {
		return "Distinct(" + getWrapped() + ")";
	}
	
	@Override
	public int hashCode() {
		return getWrapped().hashCode() ^ 53;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DistinctOp)) return false;
		DistinctOp other = (DistinctOp) o;
		if (!getWrapped().equals(other.getWrapped())) return false;
		return true;
	}
}
