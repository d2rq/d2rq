package de.fuberlin.wiwiss.d2rq.algebra;

import de.fuberlin.wiwiss.d2rq.expr.Expression;


/**
 * Wraps a relation and allows it to be modified by relational
 * operators. Normally, applying an operator to a relation
 * results in a new object. This is impractical in some places.
 * The MutableRelation solves this problem.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MutableRelation.java,v 1.4 2008/04/24 17:48:52 cyganiak Exp $
 */
public class MutableRelation implements RelationalOperators {

	public final static MutableRelation DUMMY = new MutableRelation(null) {
		public Relation renameColumns(ColumnRenamer renamer) { return null; }
		public Relation select(Expression condition) { return null; }
	};
	
	private Relation relation;
	
	public MutableRelation(Relation initialState) {
		this.relation = initialState;
	}
	
	public Relation immutableSnapshot() {
		return this.relation;
	}
	
	public Relation renameColumns(ColumnRenamer renamer) {
		return this.relation = this.relation.renameColumns(renamer);
	}

	public Relation empty() {
		return this.relation = Relation.EMPTY;
	}
	
	public Relation select(Expression condition) {
		if (condition.isFalse()) {
			return empty();
		}
		return this.relation = this.relation.select(condition);
	}
}
