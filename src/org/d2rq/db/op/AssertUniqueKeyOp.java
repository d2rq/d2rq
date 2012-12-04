package org.d2rq.db.op;

import java.util.ArrayList;
import java.util.Collection;

import org.d2rq.db.op.DatabaseOp.Wrapper;
import org.d2rq.db.schema.Key;

/**
 * Asserts that a certain combination of columns is unique in the
 * wrapped {@link DatabaseOp}, even if no unique key is present on it.
 * 
 * TODO: Remove this class and handle this as a modification to the underlying base table(s)'s definition(s) in RelationBuilder 
 */
public class AssertUniqueKeyOp extends Wrapper {
	private final Key key;
	private final Collection<Key> uniqueKeys = new ArrayList<Key>();
	
	public AssertUniqueKeyOp(DatabaseOp wrapped, Key uniqueKey) {
		super(wrapped);
		uniqueKeys.addAll(wrapped.getUniqueKeys());
		uniqueKeys.add(uniqueKey);
		key = uniqueKey;
	}

	public Key getKey() {
		return key;
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
		return "UniqueKey(" + getWrapped() + "," + key + ")";
	}
	
	@Override
	public int hashCode() {
		return getWrapped().hashCode() ^ key.hashCode() ^ 54;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AssertUniqueKeyOp)) return false;
		AssertUniqueKeyOp other = (AssertUniqueKeyOp) o;
		if (!getWrapped().equals(other.getWrapped())) return false;
		if (!getKey().equals(other.getKey())) return false;
		return true;
	}
}
