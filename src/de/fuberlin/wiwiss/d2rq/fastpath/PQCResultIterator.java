package de.fuberlin.wiwiss.d2rq.fastpath;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/** 
 * Iterator for PatternQueryCombiner results.
 * 
 * @author jgarbers
 * @version $Id: PQCResultIterator.java,v 1.6 2008/04/25 15:25:26 cyganiak Exp $
 */
public class PQCResultIterator extends NiceIterator implements ClosableIterator {
	private final VariableBindings variableBindings;
	private final Collection constraints;
	private final TripleRelation[] conjunction;
	private final Iterator conjunctionsIterator;
	private ApplyTripleMakerRowIterator resultSet; 
	private Triple[] nextRow = null;
	
	public PQCResultIterator(TripleRelation[][] tripleQueries, VariableBindings variableBindings, Collection constraints) {
		this.variableBindings=variableBindings;
		this.constraints=constraints;
		this.conjunction = new TripleRelation[tripleQueries.length];
		this.conjunctionsIterator = new CombinationIterator(tripleQueries);
	}
	
	public boolean hasNext() {
		if (this.nextRow == null) {
			this.nextRow = tryFetchNextRow();
		}
		return (this.nextRow != null);
	}

	public Object next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		Triple[] result = this.nextRow;
		this.nextRow = null;
		return result;
	}
	
	/**
	 * Tries to prefetch a <code>prefetchedResult</code>.
	 * There are two resources to draw from:
	 * 1. another row from the current SQL query (resultSet)
	 * 2. a new SQL query can be started
	 * Only those TripleQuery conjunctions are considered that may have
	 * solutions in terms of NodeConstraints on shared variables.
	 */
	private Triple[] tryFetchNextRow() {
		while (true) {
			if ((this.resultSet != null) && (this.resultSet.hasNext())) {
				return this.resultSet.nextRow();
			}
			if (!this.conjunctionsIterator.hasNext()) {
				return null;
			}
			Object[] combination = (Object[]) this.conjunctionsIterator.next();
			for (int i = 0; i < combination.length; i++) {
				this.conjunction[i] = (TripleRelation) combination[i];
			}
			ConstraintHandler ch = new ConstraintHandler();
			ch.setVariableBindings(this.variableBindings);
			ch.setTripleQueryConjunction(this.conjunction);
			ch.setRDQLConstraints(this.constraints);
			ch.makeConstraints();
			if (!ch.possible) {
				continue;
			}
			this.resultSet = new ApplyTripleMakerRowIterator(
					getSQL(ch).execute(), this.conjunction);
		} // endless while loop
	}

	private SelectStatementBuilder getSQL(ConstraintHandler ch) {
		ConnectedDB db = conjunction[0].baseRelation().database();
		SelectStatementBuilder sql = new SelectStatementBuilder(db);
		sql.setEliminateDuplicates(db.allowDistinct());	
		for (int i = 0; i < this.conjunction.length; i++) {
			TripleRelation t = this.conjunction[i];
			sql.addRelation(t.baseRelation());
		}
		ch.addConstraintsToSQL(sql);
		return sql;
	}

	public void close() {
		if (this.resultSet != null) {
			this.resultSet.close();
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}