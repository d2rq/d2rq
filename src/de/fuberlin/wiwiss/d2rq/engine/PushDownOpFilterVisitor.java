package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVisitor;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.Op1;
import com.hp.hpl.jena.sparql.algebra.op.Op2;
import com.hp.hpl.jena.sparql.algebra.op.OpAssign;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpConditional;
import com.hp.hpl.jena.sparql.algebra.op.OpDatasetNames;
import com.hp.hpl.jena.sparql.algebra.op.OpDiff;
import com.hp.hpl.jena.sparql.algebra.op.OpDisjunction;
import com.hp.hpl.jena.sparql.algebra.op.OpDistinct;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpExtend;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpGraph;
import com.hp.hpl.jena.sparql.algebra.op.OpGroup;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpLabel;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpList;
import com.hp.hpl.jena.sparql.algebra.op.OpMinus;
import com.hp.hpl.jena.sparql.algebra.op.OpN;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpOrder;
import com.hp.hpl.jena.sparql.algebra.op.OpPath;
import com.hp.hpl.jena.sparql.algebra.op.OpProcedure;
import com.hp.hpl.jena.sparql.algebra.op.OpProject;
import com.hp.hpl.jena.sparql.algebra.op.OpPropFunc;
import com.hp.hpl.jena.sparql.algebra.op.OpQuad;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.algebra.op.OpReduced;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.algebra.op.OpService;
import com.hp.hpl.jena.sparql.algebra.op.OpSlice;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.algebra.op.OpTopN;
import com.hp.hpl.jena.sparql.algebra.op.OpTriple;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;


/**
 * Visitor for traversing the operator-tree, moving down any
 * filter conditions as far as possible.
 * 
 * @author Herwig Leimer
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class PushDownOpFilterVisitor implements OpVisitor {
//	private static final Log log = LogFactory.getLog(PushDownOpFilterVisitor.class);

	public static Op transform(Op op) {
		PushDownOpFilterVisitor visitor = new PushDownOpFilterVisitor();
		op.visit(visitor);
		return visitor.result();
	}
	
	private final TransformCopy copy = new TransformCopy(false);
	private final Stack<Op> stack = new Stack<Op>();
	private List<Expr> filterExpr = new ArrayList<Expr>();

	/**
	 * Returns the changed operator-tree
	 * 
	 * @return Op - root-node of the operator-tree
	 */
	public Op result() {
		if (stack.size() != 1) {
			throw new IllegalStateException("Stack is not aligned");
		}
		return stack.pop();
	}

	/**
	 * When visiting an OpFilter, all its filterconditions are collected during
	 * the top-down-stepping. During the bottom-up-stepping all filterconditions
	 * which were moved down, are removed
	 */
	public void visit(final OpFilter opFilter) {
		filterExpr.addAll(opFilter.getExprs().getList());
		Op subOp = null;
		if (opFilter.getSubOp() != null) {
			opFilter.getSubOp().visit(this);
			subOp = stack.pop();
		}
		opFilter.getExprs().getList().removeAll(filterExpr);
		// remove the filter if it has no expressions
		if (opFilter.getExprs().isEmpty()) {
			stack.push(subOp);
		} else {
			stack.push(opFilter);
		}
	}

	/**
	 * When visiting an OpUnion also 3 conditions for moving down the
	 * filterconditions are checked. Only when a condition is satisfied, the
	 * filtercondition can be moved down to the next operator in the tree.
	 * Otherwise the condition will stay here and during the bottum-up-stepping
	 * an OpFilter containing these remained filterconditions will be inserted
	 * in the operator-tree. Conditions for moving down a filtercondition: (M1,
	 * M2 are graphpatterns, F is a filtercondition) 1) Filter(Union(M1, M2),
	 * F)) will become Union(Filter(M1, F), M2) when the filterexpression is
	 * only referenced to M1 2) Filter(Union(M1, M2), F)) will become Union(M1,
	 * Filter(M2, F)) when the filterexpression is only referenced to M2
	 * 
	 * TODO: Dubious! Shouldn't this be Union(Filter(M1,F), Filter(M2,F))? What
	 * does the code actually do?
	 * 
	 * 3) Filter(Union(M1, M2), F)) will become Join(Union(M1, F), Union(M2, F))
	 * when the filterexpression is referenced to M1 and M2
	 */
	public void visit(OpUnion opUnion) {
		checkMoveDownFilterExprAndVisitOpUnion(opUnion);
	}

	/**
	 * When visiting an OpJoin 3 conditions for moving down the filterconditions
	 * are checked. Only when a condition is satisfied, the filtercondition can
	 * be moved down to the next operator in the tree. Otherwise the condition
	 * will stay here and during the bottum-up-stepping an OpFilter containing
	 * these remained filterconditions will be inserted in the operator-tree.
	 * Conditions for moving down a filtercondition: (M1, M2 are graphpatterns,
	 * F is a filtercondition) 1) Filter(Join(M1, M2), F)) will become
	 * Join(Filter(M1, F), M2) when the filterexpression is only referenced to
	 * M1 2) Filter(Join(M1, M2), F)) will become Join(M1, Filter(M2, F)) when
	 * the filterexpression is only referenced to M2 3) Filter(Join(M1, M2), F))
	 * will become Join(Filter(M1, F), Filter(M2, F)) when the filterexpression
	 * is referenced to M1 and M2
	 */
	public void visit(OpJoin opJoin) {
		checkMoveDownFilterExprAndVisitOpJoin(opJoin);
	}

	/**
	 * When there are some filterexpressions which belong to an OpBGP, the OpBGP
	 * will be converted to an OpFilteredBGP. A OpFilteredBGP is nearly the same
	 * like an OpBGP but it has a link to its parent, which is an OpFilter with
	 * the coresponding filter-conditions, because in the transforming-process
	 * of the OpBGPs to OpD2RQs a link to the above OpFilter is needed.
	 */
	public void visit(OpBGP op) {
		wrapInCurrentFilter(op);
	}

	/**
	 * When visiting an OpDiff 3 conditions for moving down the filterconditions
	 * are checked. Only when a condition is satisfied, the filtercondition can
	 * be moved down to the next operator in the tree. Otherwise the condition
	 * will stay here and during the bottom-up-stepping an OpFilter containing
	 * these remained filterconditions will be inserted in the operator-tree.
	 * Conditions for moving down a filtercondition: (M1, M2 are graphpatterns,
	 * F is a filtercondition) 1) Filter(Diff(M1, M2), F)) will become
	 * Diff(Filter(M1, F), M2) when the filterexpression is only referenced to
	 * M1 2) Filter(Diff(M1, M2), F)) will become Diff(M1, Filter(M2, F)) when
	 * the filterexpression is only referenced to M2 3) Filter(Diff(M1, M2), F))
	 * will become Diff(Filter(M1, F), Filter(M2, F)) when the filterexpression
	 * is referenced to M1 and M2
	 */
	public void visit(OpDiff opDiff) {
		// TODO: Regel nochmal ueberdenken !!!
		checkMoveDownFilterExprAndVisitOpDiff(opDiff);
	}

	public void visit(OpConditional opCondition) {
		// TODO moving down / improvements possible!! Check this
		wrapInCurrentFilterAndRecurse(opCondition);
	}

	public void visit(OpProcedure opProc) {
		// TODO What is this?
		wrapInCurrentFilterAndRecurse(opProc);
	}

	public void visit(OpPropFunc opPropFunc) {
		// TODO What is this?
		wrapInCurrentFilterAndRecurse(opPropFunc);
	}

	public void visit(OpTable opTable) {
		wrapInCurrentFilter(opTable);
	}

	public void visit(OpQuadPattern quadPattern) {
		wrapInCurrentFilter(quadPattern);
	}

	public void visit(OpPath opPath) {
		wrapInCurrentFilter(opPath);
	}

	public void visit(OpTriple opTriple) {
		wrapInCurrentFilter(opTriple);
	}

	public void visit(OpDatasetNames dsNames) {
		wrapInCurrentFilter(dsNames);
	}

	public void visit(OpSequence opSequence) {
		// TODO What is this?
		wrapInCurrentFilterAndRecurse(opSequence);
	}

	/**
	 * When visiting an OpJoin 2 conditions for moving down the filterconditions
	 * are checked. Only when a condition is satisfied, the filtercondition can
	 * be moved down to the next operator in the tree. Otherwise the condition
	 * will stay here and during the bottum-up-stepping an OpFilter containing
	 * these remained filterconditions will be inserted in the operator-tree.
	 * Conditions for moving down a filtercondition: (M1, M2 are graphpatterns,
	 * F is a filtercondition) 1) Filter(LeftJoin(M1, M2), F)) will become
	 * LeftJoin(Filter(M1, F), M2) when the filterexpression is only referenced
	 * to M1 2) Filter(LeftJoin(M1, M2), F)) will become LeftJoin(Filter(M1, F),
	 * Filter(M2, F)) when the filterexpression is referenced to M1 and M2
	 */
	public void visit(OpLeftJoin opLeftJoin) {
		checkMoveDownFilterExprAndVisitOpLeftJoin(opLeftJoin);
	}

	public void visit(OpGraph opGraph) {
		// TODO Pushing down might be possible
		wrapInCurrentFilterAndRecurse(opGraph);
	}

	public void visit(OpService opService) {
		// Don't recurse into the OpService, just return it
		// (with any filters that were pushed down to us applied)
		wrapInCurrentFilter(opService);
	}

	public void visit(OpExt opExt) {
		wrapInCurrentFilter(opExt);
	}

	public void visit(OpNull opNull) {
		wrapInCurrentFilter(opNull);
	}

	public void visit(OpLabel opLabel) {
		moveFilterPast(opLabel);
	}

	public void visit(OpList opList) {
		// TODO Might be able to move down filter past the op
		wrapInCurrentFilterAndRecurse(opList);
	}

	public void visit(OpOrder opOrder) {
		// TODO Might be able to move down filter past the op
		wrapInCurrentFilterAndRecurse(opOrder);
	}

	public void visit(OpProject opProject) {
		// TODO Might be able to move down filter past the op
		wrapInCurrentFilterAndRecurse(opProject);
	}

	public void visit(OpDistinct opDistinct) {
		wrapInCurrentFilterAndRecurse(opDistinct);
	}

	public void visit(OpReduced opReduced) {
		wrapInCurrentFilterAndRecurse(opReduced);
	}

	public void visit(OpAssign opAssign) {
		// TODO Might be able to move down filter past the op
		wrapInCurrentFilterAndRecurse(opAssign);
	}

	public void visit(OpSlice opSlice) {
		wrapInCurrentFilterAndRecurse(opSlice);
	}

	public void visit(OpGroup opGroup) {
		wrapInCurrentFilterAndRecurse(opGroup);
	}

	public void visit(OpExtend opExtend) {
		// TODO Might be able to move down filter past the OpExtend
		wrapInCurrentFilterAndRecurse(opExtend);
	}

	/**
	 * TODO I have no clue if this actually works
	 * 
	 * Filter(A-B,e) = Filter(A,e)-B
	 */
	public void visit(OpMinus opMinus) {
		// Walk into A subtree
		opMinus.getLeft().visit(this);
		Op leftWithFilter = stack.pop();

		// Walk into B subtree with empty expression list
		List<Expr> tmp = filterExpr;
		filterExpr = new ArrayList<Expr>();
		opMinus.getRight().visit(this);
		Op right = stack.pop();

		// Remove the entire filter expression on the way up
		filterExpr = tmp;

		stack.push(OpMinus.create(leftWithFilter, right));
	}

	public void visit(OpDisjunction opDisjunction) {
		// TODO What the heck is an OpDisjunction anyway?
		wrapInCurrentFilterAndRecurse(opDisjunction);
	}

	public void visit(OpTopN opTop) {
		// TODO Might be able to move down filter past the OpTopN
		wrapInCurrentFilterAndRecurse(opTop);
	}

	public void visit(OpQuad opQuad) {
		wrapInCurrentFilter(opQuad);
	}

	private void wrapInCurrentFilterAndRecurse(Op1 op1) {
		List<Expr> retainedFilterExpr = new ArrayList<Expr>(filterExpr);
		filterExpr.clear();
		Op subOp = null;
		if (op1.getSubOp() != null) {
			op1.getSubOp().visit(this);
			subOp = stack.pop();
		}
		wrapInFilter(op1.apply(copy, subOp), retainedFilterExpr);
		filterExpr = retainedFilterExpr;
	}

	private void wrapInCurrentFilterAndRecurse(Op2 op2) {
		List<Expr> retainedFilterExpr = new ArrayList<Expr>(filterExpr);
		filterExpr.clear();
		Op left = null;
		if (op2.getLeft() != null) {
			op2.getLeft().visit(this);
			left = stack.pop();
		}
		Op right = null;
		if (op2.getRight() != null) {
			op2.getRight().visit(this);
			right = stack.pop();
		}
		wrapInFilter(op2.apply(copy, left, right), retainedFilterExpr);
		filterExpr = retainedFilterExpr;
	}

	private void wrapInCurrentFilterAndRecurse(OpN opN) {
		List<Expr> retainedFilterExpr = new ArrayList<Expr>(filterExpr);
		filterExpr.clear();
		List<Op> children = new ArrayList<Op>();
		for (Op child : opN.getElements()) {
			child.visit(this);
			children.add(stack.pop());
		}
		wrapInFilter(opN.apply(copy, children), retainedFilterExpr);
		filterExpr = retainedFilterExpr;
	}

	private void wrapInCurrentFilter(Op op) {
		wrapInFilter(op, filterExpr);
	}

	private void wrapInFilter(Op op, List<Expr> filters) {
		if (filterExpr.isEmpty()) {
			stack.push(op);
		} else {
			stack.push(OpFilter.filter(new ExprList(filters), op));
		}
	}

	private void moveFilterPast(Op1 op1) {
		op1.getSubOp().visit(this);
	}

	/**
	 * Calculates the set of valid filterexpressions as a subset from the whole
	 * set of possibilities.
	 * 
	 * @param candidates
	 *            - the whole set
	 * @param op
	 * @return List - the subset from the set of possiblities
	 */
	private List<Expr> calcValidFilterExpr(List<Expr> candidates, Op op) {
		Set<Var> mentionedVars = VarCollector.mentionedVars(op);
		List<Expr> result = new ArrayList<Expr>();
		for (Expr expr : candidates) {
			if (mentionedVars.containsAll(expr.getVarsMentioned())) {
				result.add(expr);
			}
		}
		return result;
	}

	/**
	 * Checks first if a filterexpression can be moved down. And after visits
	 * the operator
	 */
	private void checkMoveDownFilterExprAndVisitOpUnion(OpUnion opUnion) {
		Op left = null;
		Op right = null;
		Op newOp;
		List<Expr> filterExprBeforeOpUnion, filterExprAfterOpUnion, notMoveableFilterExpr;

		// contains the filterexpressions that are valid before this op2
		filterExprBeforeOpUnion = new ArrayList<Expr>(this.filterExpr);
		// contains the filterexpressions that are valid after this op2
		// this is needed because during the bottom-up-stepping all
		// filterexpressions
		// which could not be transformed down, must be inserted by means of an
		// OpFilter
		// above this op2
		filterExprAfterOpUnion = new ArrayList<Expr>();

		// check left subtree
		if ((left = opUnion.getLeft()) != null) {
			// calculate the set of filterexpressions that are also valid for
			// the
			// left subtree
			this.filterExpr = calcValidFilterExpr(filterExprBeforeOpUnion, left);
			filterExprAfterOpUnion.addAll(this.filterExpr);
			// step down
			opUnion.getLeft().visit(this);
			left = stack.pop();
		}

		// check the right subtree
		if ((right = opUnion.getRight()) != null) {
			// calculate the set of filterexpressions that are also valid for
			// the
			// right subtree
			this.filterExpr = calcValidFilterExpr(filterExprBeforeOpUnion,
					right);
			filterExprAfterOpUnion.addAll(this.filterExpr);
			// step down
			opUnion.getRight().visit(this);
			right = stack.pop();
		}

		// note: filterExprAfterOpUnion contains now all filterexpressions which
		// could
		// be moved down
		// now calculate all filterexpressions which were not moveable
		notMoveableFilterExpr = new ArrayList<Expr>(filterExprBeforeOpUnion);
		notMoveableFilterExpr.removeAll(filterExprAfterOpUnion);

		// if there are some filterexpressions which could not be moved down,
		// an opFilter must be inserted that contains this filterexpressions
		if (!notMoveableFilterExpr.isEmpty()) {
			// create the filter
			newOp = OpFilter.filter(OpUnion.create(left, right));
			// add the conditions
			((OpFilter) newOp).getExprs().getList()
					.addAll(notMoveableFilterExpr);
		} else {
			newOp = opUnion;
		}

		// restore filterexpressions
		this.filterExpr = filterExprBeforeOpUnion;

		this.stack.push(newOp);
	}

	/**
	 * Checks first if a filterexpression can be moved down. And after visits
	 * the operator
	 */
	private void checkMoveDownFilterExprAndVisitOpJoin(OpJoin opJoin) {
		Op left = null;
		Op right = null;
		Op newOp;
		List<Expr> filterExprBeforeOpJoin, filterExprAfterOpJoin, notMoveableFilterExpr;

		// contains the filterexpressions that are valid before this op2
		filterExprBeforeOpJoin = new ArrayList<Expr>(this.filterExpr);
		// contains the filterexpressions that are valid after this op2
		// this is needed because during the bottom-up-stepping all
		// filterexpressions
		// which could not be transformed down, must be inserted by means of an
		// OpFilter
		// above this op2
		filterExprAfterOpJoin = new ArrayList<Expr>();

		// check left subtree
		if ((left = opJoin.getLeft()) != null) {
			// calculate the set of filterexpressions that are also valid for
			// the
			// left subtree
			this.filterExpr = calcValidFilterExpr(filterExprBeforeOpJoin, left);
			filterExprAfterOpJoin.addAll(this.filterExpr);
			// step down
			opJoin.getLeft().visit(this);
			left = stack.pop();
		}

		// check the right subtree
		if ((right = opJoin.getRight()) != null) {
			// calculate the set of filterexpressions that are also valid for
			// the
			// right subtree
			this.filterExpr = calcValidFilterExpr(filterExprBeforeOpJoin, right);
			filterExprAfterOpJoin.addAll(this.filterExpr);
			// step down
			opJoin.getRight().visit(this);
			right = stack.pop();
		}

		// note: filterExprAfterOpUnion contains now all filterexpressions which
		// could
		// be moved down
		// now calculate all filterexpressions which were not moveable
		notMoveableFilterExpr = new ArrayList<Expr>(filterExprBeforeOpJoin);
		notMoveableFilterExpr.removeAll(filterExprAfterOpJoin);

		// if there are some filterexpressions which could not be moved down,
		// an opFilter must be inserted that contains this filterexpressions
		if (!notMoveableFilterExpr.isEmpty()) {
			// create the filter
			newOp = OpFilter.filter(OpJoin.create(left, right));
			// add the conditions
			((OpFilter) newOp).getExprs().getList()
					.addAll(notMoveableFilterExpr);
		} else {
			// nothing must be done
			newOp = opJoin;
		}

		// restore filterexpressions
		this.filterExpr = filterExprBeforeOpJoin;

		this.stack.push(newOp);
	}

	/**
	 * Checks first if a filterexpression can be moved down. And after visits
	 * the operator
	 */
	private void checkMoveDownFilterExprAndVisitOpLeftJoin(OpLeftJoin opLeftJoin) {
		Op left = null;
		Op right = null;
		Op newOp;
		List<Expr> filterExprBeforeOpLeftJoin, filterExprAfterOpLeftJoin, notMoveableFilterExpr, filterExprRightSide, validFilterExprRightSide;

		// contains the filterexpressions that are valid before this op2
		filterExprBeforeOpLeftJoin = new ArrayList<Expr>(this.filterExpr);
		// contains the filterexpressions that are valid after this op2
		// this is needed because during the bottom-up-stepping all
		// filterexpressions
		// which could not be transformed down, must be inserted by means of an
		// OpFilter
		// above this op2
		filterExprAfterOpLeftJoin = new ArrayList<Expr>();

		// check left subtree
		if ((left = opLeftJoin.getLeft()) != null) {
			// calculate the set of filterexpressions that are also valid for
			// the
			// left subtree
			this.filterExpr = calcValidFilterExpr(filterExprBeforeOpLeftJoin,
					left);
			filterExprAfterOpLeftJoin.addAll(this.filterExpr);
			// step down
			opLeftJoin.getLeft().visit(this);
			left = stack.pop();
		}

		// check the right subtree
		if ((right = opLeftJoin.getRight()) != null) {
			// calculate the set of filterexpressions that are also valid for
			// the
			// right subtree

			filterExprRightSide = calcValidFilterExpr(
					filterExprBeforeOpLeftJoin, right);
			validFilterExprRightSide = new ArrayList<Expr>();

			// now check for expr that are vaild for left-side and right-side
			// only when an expr is vaid for left-side, it can be vaild for
			// right-side
			for (Expr expr : filterExprRightSide) {
				if (this.filterExpr.contains(expr)) {
					// valid
					validFilterExprRightSide.add(expr);
				}
			}

			this.filterExpr = validFilterExprRightSide;
			filterExprAfterOpLeftJoin.addAll(this.filterExpr);

			// step down
			opLeftJoin.getRight().visit(this);
			right = stack.pop();
		}

		// note: filterExprAfterOpLeftJoin contains now all filterexpressions
		// which could
		// be moved down
		// now calculate all filterexpressions which were not moveable
		notMoveableFilterExpr = new ArrayList<Expr>(filterExprBeforeOpLeftJoin);
		notMoveableFilterExpr.removeAll(filterExprAfterOpLeftJoin);

		// if there are some filterexpressions which could not be moved down,
		// an opFilter must be inserted that contains this filterexpressions
		if (!notMoveableFilterExpr.isEmpty()) {
			// create the filter for an opleftjoin
			newOp = OpFilter.filter(OpLeftJoin.create(left, right,
					opLeftJoin.getExprs()));
			// add the conditions
			((OpFilter) newOp).getExprs().getList()
					.addAll(notMoveableFilterExpr);
		} else {
			// nothing must be done
			newOp = opLeftJoin;
		}

		// restore filterexpressions
		this.filterExpr = filterExprBeforeOpLeftJoin;

		this.stack.push(newOp);
	}

	/**
	 * Checks first if a filterexpression can be moved down. And after visits
	 * the operator
	 */
	private void checkMoveDownFilterExprAndVisitOpDiff(Op2 opDiff) {
		Op left = null;
		Op right = null;
		Op newOp;
		List<Expr> filterExprBeforeOpUnionOpJoin, filterExprAfterOpUnionOpJoin, notMoveableFilterExpr;

		// contains the filterexpressions that are valid before this op2
		filterExprBeforeOpUnionOpJoin = new ArrayList<Expr>(this.filterExpr);
		// contains the filterexpressions that are valid after this op2
		// this is needed because during the bottom-up-stepping all
		// filterexpressions
		// which could not be transformed down, must be inserted by means of an
		// OpFilter
		// above this op2
		filterExprAfterOpUnionOpJoin = new ArrayList<Expr>();

		// check left subtree
		if ((left = opDiff.getLeft()) != null) {
			// calculate the set of filterexpressions that are also valid for
			// the
			// left subtree
			this.filterExpr = calcValidFilterExpr(
					filterExprBeforeOpUnionOpJoin, left);
			filterExprAfterOpUnionOpJoin.addAll(this.filterExpr);
			// step down
			opDiff.getLeft().visit(this);
			left = stack.pop();
		}

		// check the right subtree
		if ((right = opDiff.getRight()) != null) {
			// calculate the set of filterexpressions that are also valid for
			// the
			// right subtree
			this.filterExpr = calcValidFilterExpr(
					filterExprBeforeOpUnionOpJoin, right);
			filterExprAfterOpUnionOpJoin.addAll(this.filterExpr);
			// step down
			opDiff.getRight().visit(this);
			right = stack.pop();
		}

		// note: filterExprAfterOpUnion contains now all filterexpressions which
		// could
		// be moved down
		// now calculate all filterexpressions which were not moveable
		notMoveableFilterExpr = new ArrayList<Expr>(
				filterExprBeforeOpUnionOpJoin);
		notMoveableFilterExpr.removeAll(filterExprAfterOpUnionOpJoin);

		// if there are some filterexpressions which could not be moved down,
		// an opFilter must be inserted that contains this filterexpressions
		if (!notMoveableFilterExpr.isEmpty()) {
			// create the filter
			newOp = OpFilter.filter(OpDiff.create(left, right));
			// add the conditions
			((OpFilter) newOp).getExprs().getList()
					.addAll(notMoveableFilterExpr);
		} else {
			// nothing must be done
			newOp = opDiff;
		}

		// restore filterexpressions
		this.filterExpr = filterExprBeforeOpUnionOpJoin;

		this.stack.push(newOp);
	}
}