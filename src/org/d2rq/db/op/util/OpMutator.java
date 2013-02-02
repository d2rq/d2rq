package org.d2rq.db.op.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.AssertUniqueKeyOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.DistinctOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.ExtendOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.OpVisitor;
import org.d2rq.db.op.OrderOp;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.TableOp;


public abstract class OpMutator {
	private final DatabaseOp original;
	
	public OpMutator(DatabaseOp original) {
		this.original = original;
	}
	
	public DatabaseOp getResult() {
		final Stack<DatabaseOp> resultStack = new Stack<DatabaseOp>();
		original.accept(new OpVisitor() {
			public boolean visitEnter(InnerJoinOp table) { 
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					for (NamedOp child: table.getTables()) {
						resultStack.push(child);
					}
					return false;
				}
			}
			public void visitLeave(InnerJoinOp table) {
				List<NamedOp> newChildren = new ArrayList<NamedOp>();
				for (DatabaseOp newChild: resultStack) {
					if (newChild instanceof NamedOp) {
						newChildren.add((NamedOp) newChild);
					} else {
						throw new IllegalArgumentException("Only NamedTable allowed in InnerJoin; offending child: " + newChild);
					}
				}
				resultStack.clear();
				resultStack.push(OpMutator.this.visitLeave(
						table, newChildren));
			}
			public boolean visitEnter(SelectOp table) {
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					resultStack.push(table.getWrapped());
					return false;
				}
			}
			public void visitLeave(SelectOp table) {
				resultStack.push(OpMutator.this.visitLeave(
						table, resultStack.pop()));
			}
			public boolean visitEnter(ProjectOp table) {
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					resultStack.push(table.getWrapped());
					return false;
				}
			}
			public void visitLeave(ProjectOp table) {
				resultStack.push(OpMutator.this.visitLeave(
						table, resultStack.pop()));
			}
			public boolean visitEnter(ExtendOp table) {
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					resultStack.push(table.getWrapped());
					return false;
				}
			}
			public void visitLeave(ExtendOp table) {
				resultStack.push(OpMutator.this.visitLeave(
						table, resultStack.pop()));
			}
			public boolean visitEnter(AliasOp table) {
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					resultStack.push(table.getOriginal());
					return false;
				}
			}
			public void visitLeave(AliasOp table) {
				resultStack.push(OpMutator.this.visitLeave(
						table, resultStack.pop()));
			}
			public boolean visitEnter(OrderOp table) {
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					resultStack.push(table.getWrapped());
					return false;
				}
			}
			public void visitLeave(OrderOp table) {
				resultStack.push(OpMutator.this.visitLeave(
						table, resultStack.pop()));
			}
			public boolean visitEnter(LimitOp table) {
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					resultStack.push(table.getWrapped());
					return false;
				}
			}
			public void visitLeave(LimitOp table) {
				resultStack.push(OpMutator.this.visitLeave(
						table, resultStack.pop()));
			}
			public boolean visitEnter(DistinctOp table) {
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					resultStack.push(table.getWrapped());
					return false;
				}
			}
			public void visitLeave(DistinctOp table) {
				resultStack.push(OpMutator.this.visitLeave(
						table, resultStack.pop()));
			}
			public boolean visitEnter(AssertUniqueKeyOp table) {
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					resultStack.push(table.getWrapped());
					return false;
				}
			}
			public void visitLeave(AssertUniqueKeyOp table) {
				resultStack.push(OpMutator.this.visitLeave(
						table, resultStack.pop()));
			}
			public boolean visitEnter(EmptyOp table) {
				if (OpMutator.this.visitEnter(table)) {
					return true;
				} else {
					resultStack.push(table.getWrapped());
					return false;
				}
			}
			public void visitLeave(EmptyOp table) {
				resultStack.push(OpMutator.this.visitLeave(
						table, resultStack.pop()));
			}
			public void visit(TableOp table) {
				resultStack.push(OpMutator.this.visit(table));
			}
			public void visit(SQLOp table) {
				resultStack.push(OpMutator.this.visit(table));
			}
			public void visitOpTrue() {
				resultStack.push(OpMutator.this.visitOpTrue());
			}
		});
		return resultStack.pop();
	}
	
	public boolean visitEnter(InnerJoinOp original) {
		return true;
	}
	
	public DatabaseOp visitLeave(InnerJoinOp original, Collection<NamedOp> newChildren) {
		return InnerJoinOp.join(newChildren, 
				original.getJoinConditions());
	}

	public boolean visitEnter(SelectOp original) {
		return true;
	}
	
	public DatabaseOp visitLeave(SelectOp original, DatabaseOp child) {
		return SelectOp.select(child, original.getCondition());
	}

	public boolean visitEnter(ProjectOp original) {
		return true;
	}

	public DatabaseOp visitLeave(ProjectOp original, DatabaseOp child) {
		return ProjectOp.project(child, original.getColumns());
	}
	
	public boolean visitEnter(ExtendOp original) {
		return true;
	}

	public DatabaseOp visitLeave(ExtendOp original, DatabaseOp child) {
		return ExtendOp.extend(child, original.getNewColumn(), 
				original.getExpression(), original.getVendor());
	}
	
	public boolean visitEnter(AliasOp original) {
		return true;
	}

	public DatabaseOp visitLeave(AliasOp original, DatabaseOp child) {
		return AliasOp.create(child, original.getTableName());
	}

	public boolean visitEnter(OrderOp original) {
		return true;
	}

	public DatabaseOp visitLeave(OrderOp original, DatabaseOp child) {
		return new OrderOp(original.getOrderBy(), child);
	}
	
	public boolean visitEnter(LimitOp original) {
		return true;
	}

	public DatabaseOp visitLeave(LimitOp original, DatabaseOp child) {
		return LimitOp.limit(child, 
				original.getLimit(), original.getLimitInverse());
	}
	
	public boolean visitEnter(DistinctOp original) {
		return true;
	}

	public DatabaseOp visitLeave(DistinctOp original, DatabaseOp child) {
		return new DistinctOp(child);
	}
	
	public boolean visitEnter(AssertUniqueKeyOp original) {
		return true;
	}

	public DatabaseOp visitLeave(AssertUniqueKeyOp original, DatabaseOp child) {
		return new AssertUniqueKeyOp(child, original.getKey());
	}
	
	public boolean visitEnter(EmptyOp original) {
		return true;
	}

	public DatabaseOp visitLeave(EmptyOp original, DatabaseOp child) {
		return EmptyOp.create(child);
	}
	
	public DatabaseOp visit(TableOp original) {
		return original;
	}
	
	public DatabaseOp visit(SQLOp original) {
		return original;
	}

	public DatabaseOp visitOpTrue() {
		return DatabaseOp.TRUE;
	}
}
