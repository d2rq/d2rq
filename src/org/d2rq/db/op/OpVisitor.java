package org.d2rq.db.op;



/**
 * Visitor for {@link DatabaseOp}s. Implements the Visitor pattern.
 * When walking a hierarchy of {@link DatabaseOp}s, for each node
 * first the respective <code>visitEnter()</code> method is called,
 * then all children are visited, and then <code>visitLeave()</code>
 * is called. If <code>visitEnter()</code> returns <code>false</code>
 * for some node, then its children will not be visited and
 * <code>visitLeave()</code> is skipped for that node.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface OpVisitor {

	boolean visitEnter(InnerJoinOp table);
	void visitLeave(InnerJoinOp table);

	boolean visitEnter(SelectOp table);
	void visitLeave(SelectOp table);

	boolean visitEnter(ProjectOp table);
	void visitLeave(ProjectOp table);
	
	boolean visitEnter(AliasOp table);
	void visitLeave(AliasOp table);
	
	boolean visitEnter(OrderOp table);
	void visitLeave(OrderOp table);
	
	boolean visitEnter(LimitOp table);
	void visitLeave(LimitOp table);
	
	boolean visitEnter(DistinctOp table);
	void visitLeave(DistinctOp table);
	
	boolean visitEnter(AssertUniqueKeyOp table);
	void visitLeave(AssertUniqueKeyOp table);
	
	boolean visitEnter(EmptyOp table);
	void visitLeave(EmptyOp table);
	
	void visit(TableOp table);
	void visit(SQLOp table);
	void visitOpTrue();
	
	public static abstract class Default implements OpVisitor {
		public boolean visitEnter(InnerJoinOp table) { return true; }
		public void visitLeave(InnerJoinOp table) {}
		public boolean visitEnter(SelectOp table) { return true; }
		public void visitLeave(SelectOp table) {}
		public boolean visitEnter(ProjectOp table) { return true; }
		public void visitLeave(ProjectOp table) {}
		public boolean visitEnter(AliasOp table) { return true; }
		public void visitLeave(AliasOp table) {}
		public boolean visitEnter(OrderOp table) { return true; }
		public void visitLeave(OrderOp table) {}
		public boolean visitEnter(LimitOp table) { return true; }
		public void visitLeave(LimitOp table) {}
		public boolean visitEnter(DistinctOp table) { return true; }
		public void visitLeave(DistinctOp table) {}
		public boolean visitEnter(AssertUniqueKeyOp table) { return true; }
		public void visitLeave(AssertUniqueKeyOp table) {}
		public boolean visitEnter(EmptyOp table) { return true; }
		public void visitLeave(EmptyOp table) {}
		public void visit(TableOp table) {}
		public void visit(SQLOp table) {}
		public void visitOpTrue() {}
	}
}
