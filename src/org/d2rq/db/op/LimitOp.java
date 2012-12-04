package org.d2rq.db.op;

import java.util.Collection;

import org.d2rq.db.op.util.OpMutator;


public class LimitOp extends DatabaseOp.Wrapper {
	public static final int NO_LIMIT = -1;
	
	public static DatabaseOp limit(DatabaseOp tabular, final int limit, final int limitInverse) {
		if (limit == NO_LIMIT && limitInverse == NO_LIMIT) {
			return tabular;
		}
		// Rule: recurse over Distinct; merge Limit; wrap all others
		return new OpMutator(tabular) {
			private DatabaseOp wrap(DatabaseOp child) {
				return new LimitOp(limit, limitInverse, child);
			}
			@Override
			public boolean visitEnter(InnerJoinOp original) {
				return false;
			}
			@Override
			public DatabaseOp visitLeave(InnerJoinOp original,
					Collection<NamedOp> newChildren) {
				return wrap(original);
			}
			@Override
			public boolean visitEnter(SelectOp original) {
				return false;
			}
			@Override
			public DatabaseOp visitLeave(SelectOp original, DatabaseOp child) {
				return wrap(original);
			}
			@Override
			public boolean visitEnter(ProjectOp original) {
				return false;
			}
			@Override
			public DatabaseOp visitLeave(ProjectOp original, DatabaseOp child) {
				return wrap(original);
			}
			@Override
			public boolean visitEnter(AliasOp original) {
				return false;
			}
			@Override
			public DatabaseOp visitLeave(AliasOp original, DatabaseOp child) {
				return wrap(original);
			}
			@Override
			public boolean visitEnter(OrderOp original) {
				return false;
			}
			@Override
			public DatabaseOp visitLeave(OrderOp original, DatabaseOp child) {
				return wrap(original);
			}
			@Override
			public boolean visitEnter(LimitOp original) {
				return false;
			}
			@Override
			public DatabaseOp visitLeave(LimitOp original, DatabaseOp child) {
				return new LimitOp(
						combineLimits(limit, original.getLimit()), 
						combineLimits(limitInverse, original.getLimitInverse()), 
						child);
			}
			@Override
			public boolean visitEnter(EmptyOp original) {
				return false;
			}
			/**
			 * Limiting an empty table is a no-op, so just return the original
			 */
			@Override
			public DatabaseOp visitLeave(EmptyOp original, DatabaseOp child) {
				return original;
			}
			@Override
			public DatabaseOp visit(TableOp original) {
				return wrap(original);
			}
			@Override
			public DatabaseOp visit(SQLOp original) {
				return wrap(original);
			}
			@Override
			public DatabaseOp visitOpTrue() {
				return wrap(DatabaseOp.TRUE);
			}
		}.getResult();
	}

	/**
	 * Modifies a Tabular that has an inverse limit to apply the inverse
	 * limit as its limit.
	 */
	public static DatabaseOp swapLimits(DatabaseOp table) {
		return new OpMutator(table) {
			public boolean visitEnter(LimitOp limited) {
				return false;
			}
			public DatabaseOp visitLeave(LimitOp limited, DatabaseOp child) {
				return new LimitOp(limited.getLimitInverse(), limited.getLimit(), limited.getWrapped());
			}
		}.getResult();
	}
	
	public static int combineLimits(int limit1, int limit2) {
	    if (limit1 == NO_LIMIT) {
	        return limit2;
	    } else if (limit2 == NO_LIMIT) {
	        return limit1;
	    }
	    return Math.min(limit1, limit2);
	}

	private final int limit;
	private final int limitInverse;

	private LimitOp(int limit, int limitInverse, DatabaseOp wrapped) {
		super(wrapped);
		this.limit = limit;
		this.limitInverse = limitInverse;
	}

	public int getLimit() { 
		return limit;
	}
	
	public int getLimitInverse() {
		return limitInverse;
	}
	
	public void accept(OpVisitor visitor) {
		if (visitor.visitEnter(this)) {
			getWrapped().accept(visitor);
		}
		visitor.visitLeave(this);
	}
	
	@Override
	public String toString() {
		return "Limit(" + getWrapped() + "," + limit + "," + limitInverse + ")";
	}
	
	@Override
	public int hashCode() {
		return getWrapped().hashCode() ^ limit ^ limitInverse ^ 68;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LimitOp)) return false;
		LimitOp other = (LimitOp) o;
		if (!getWrapped().equals(other.getWrapped())) return false;
		return limit == other.limit && limitInverse == other.limitInverse;
	}
}
