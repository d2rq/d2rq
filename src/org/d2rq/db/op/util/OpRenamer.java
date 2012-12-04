package org.d2rq.db.op.util;

import java.util.Collection;

import org.d2rq.db.op.AliasOp;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.InnerJoinOp;
import org.d2rq.db.op.NamedOp;
import org.d2rq.db.op.OrderOp;
import org.d2rq.db.op.ProjectOp;
import org.d2rq.db.op.SelectOp;
import org.d2rq.db.renamer.Renamer;


/**
 * Renames tables and columns in a {@link DatabaseOp} according to a
 * {@link Renamer}. Leaves columns and tables unchanged if they
 * cannot logically be renamed, for example the definitions of
 * base tables.
 *  
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class OpRenamer extends OpMutator {
	private final Renamer renamer;
	
	public OpRenamer(DatabaseOp subject, Renamer renamer) {
		super(subject);
		this.renamer = renamer;
	}
	
	@Override
	public DatabaseOp visitLeave(InnerJoinOp table, Collection<NamedOp> children) {
		return InnerJoinOp.join(children,
				renamer.applyToJoinConditions(table.getJoinConditions()));
	}

	@Override
	public DatabaseOp visitLeave(SelectOp table, DatabaseOp child) {
		return SelectOp.select(child, renamer.applyTo(table.getCondition()));
	}

	@Override
	public DatabaseOp visitLeave(ProjectOp table, DatabaseOp child) {
		return ProjectOp.create(child,
				renamer.applyToProjections(table.getProjections()));
	}

	@Override
	public boolean visitEnter(AliasOp table) {
		return false;
	}

	@Override
	public DatabaseOp visitLeave(AliasOp table, DatabaseOp child) {
		return AliasOp.create(table.getOriginal(), 
				renamer.applyTo(table.getTableName()));
	}

	@Override
	public DatabaseOp visitLeave(OrderOp table, DatabaseOp child) {
		return new OrderOp(renamer.applyTo(table.getOrderBy()), child);
	}
}
