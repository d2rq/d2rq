package org.d2rq.db.op.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.DistinctOp;
import org.d2rq.db.op.EmptyOp;
import org.d2rq.db.op.ExtendOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.LimitOp;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.OrderOp;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.SQLOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;


/**
 * Applies a projection to a {@link DatabaseOp}, producing a new
 * {@link DatabaseOp}, and possibly a {@link Renamer} that reflects
 * changes in the exposed column names that had to be made. 
 * 
 * Projections are pushed down over {@link LimitOp} and
 * {@link EmptyOp}, merged with other {@link ProjectOp}s,
 * done over a sub-<code>SELECT</code> for some {@link DistinctOp}s,
 * and simply wrapped around other {@link DatabaseOp}s.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class OpProjecter extends OpMutator {
	private final List<ColumnName> projections;
	private Renamer renamer = Renamer.IDENTITY;
	
	public OpProjecter(DatabaseOp original, Set<ColumnName> projections) {
		super(original);
		this.projections = new ArrayList<ColumnName>(projections);
	}
	
	/**
	 * Returns a {@link Renamer} that reflects any changes in column names
	 * that had to be made in order to apply the projection.
	 */
	public Renamer getRenamer() {
		return renamer;
	}
	private DatabaseOp wrap(DatabaseOp original) {
		return ProjectOp.project(original, projections);
	}
	
	@Override
	public boolean visitEnter(InnerJoinOp original) {
		return false;
	}

	/**
	 * TODO: Can we push parts of the projection down into the join?
	 *       E.g., can we eliminate one of the tables in the join,
	 *       or remove a column from a sub-select?
	 */ 
	@Override
	public DatabaseOp visitLeave(InnerJoinOp original,
			Collection<NamedOp> newChildren) {
		return wrap(original);
	}

	@Override
	public boolean visitEnter(SelectOp original) {
		return false;
	}

	/**
	 * TODO: Can we push the projection down over the selection?
	 *       All the columns needed for the selection must be present in the
	 *       projection. This can be beneficial if the projection can be
	 *       pushed even further down, e.g., over a join. 
	 */
	@Override
	public DatabaseOp visitLeave(SelectOp original, DatabaseOp child) {
		return wrap(original);
	}

	@Override
	public boolean visitEnter(ProjectOp original) {
		return false;
	}

	/**
	 * Intersect the projection lists.
	 */
	@Override
	public DatabaseOp visitLeave(ProjectOp original, DatabaseOp child) {
		List<ColumnName> intersection = new ArrayList<ColumnName>();
		for (ColumnName column: projections) {
			if (original.getColumns().contains(column)) {
				intersection.add(column);
			}
		}
		return ProjectOp.project(original.getWrapped(), intersection);
	}

	/**
	 * Always recurse into extensions. If the extension column is being
	 * projected away, then we remove the {@link ExtendOp} on the way up.
	 */
	@Override
	public DatabaseOp visitLeave(ExtendOp original, DatabaseOp child) {
		if (!projections.contains(original.getNewColumn())) {
			return child;
		}
		return super.visitLeave(original, child);
	}

	@Override
	public boolean visitEnter(AliasOp original) {
		// TODO: We probably could push down over aliases, but I don't think the case arises anywhere
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

	/**
	 * TODO: Can we push the projection down over the ordering?
	 *       All the columns needed for the order spec must be present in
	 *       the projection. This can be beneficial if the projection can be
	 *       pushed even further down, e.g., over a join.
	 */
	@Override
	public DatabaseOp visitLeave(OrderOp original, DatabaseOp child) {
		return wrap(original);
	}

	/**
	 * We have a DISTINCT clause. Doing the projection inside the DISTINCT
	 * clause (that is, adding or removing columns inside the DISTNCT) is
	 * preferrable, but only possible if all we do is adding new derived
	 * columns, or removing columns known to be constant throughout the
	 * table. Otherwise, pushing the projection down would change the result
	 * and is invalid.
	 * 
	 * If we can't push down, then we have to wrap the original DISTINCT
	 * into a sub-SELECT, apply the projection on the sub-SELECT, and
	 * rename everything in the node makers to use the new table name.
	 */
	@Override
	public boolean visitEnter(DistinctOp original) {
		Set<ColumnName> columns = new HashSet<ColumnName>(
				original.getColumns().asList());
		for (ColumnName requiredColumn: projections) {
			columns.remove(requiredColumn);
		}
		// columns now contains all columns in the original
		// that we don't need in the projection
		for (ColumnName discarded: columns) {
			if (!OpUtil.isConstantColumn(original, discarded)) {
				// We are dropping a non-constant column. This cannot be
				// pushed down into the DISTINCT, so stop recursion, and
				// wrap the DISTINCT into a PROJECT op on the way out.
				return false;
			}
		}
		// Recurse and apply the projection inside the DISTINCT
		return true;
	}

	@Override
	public DatabaseOp visitLeave(DistinctOp original, DatabaseOp child) {
		if (child.getColumns().asList().equals(projections)) {
			// The child already has exactly the right columns, so evidently
			// we have recursed and applied the projection inside the child.
			// Just create a new DISTINCT wrapping the child.
			return super.visitLeave(original, child);
		}

		// We did not recurse, and need to apply the projection around the
		// DISTINCT, creating an alias (SQL subquery) first.
		AliasOp alias = AliasOp.createWithUniqueName(original, "PROJECT");
		renamer = alias.getRenamer();
		return ProjectOp.project(alias, renamer.applyToColumns(projections));
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
}
