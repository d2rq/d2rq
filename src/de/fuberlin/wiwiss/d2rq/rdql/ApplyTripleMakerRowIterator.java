package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Map;
import java.util.NoSuchElementException;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.map.TripleMaker;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;

/**
 * Contains the result set from one SQL query and transforms it into triples.
 * A triple is produced for TripleMaker in TripleMaker and each row in the result set.
 *
 * @author jgarbers
 * @version $Id: ApplyTripleMakerRowIterator.java,v 1.3 2006/09/09 15:40:03 cyganiak Exp $
 */
public class ApplyTripleMakerRowIterator implements ClosableIterator {
	private QueryExecutionIterator sqlIterator;
	private TripleMaker[] tripleMakers = null;
	private Triple[] prefetchedRow;
	private boolean explicitlyClosed = false;

	public ApplyTripleMakerRowIterator(QueryExecutionIterator sqlIterator, RDFRelation[] queries,
			Map columnNameNumberMap) {
		this.sqlIterator = sqlIterator;
		this.tripleMakers = new TripleMaker[queries.length];
		for (int i = 0; i < queries.length; i++) {
			this.tripleMakers[i] = new TripleMaker(queries[i], columnNameNumberMap);
		}
	}
	
	public boolean hasNext() {
		if (this.explicitlyClosed) {
			return false;
		}
		if (this.prefetchedRow == null) {
			this.prefetchedRow = tryFetchNextRow();
		}
		return this.prefetchedRow != null;
	}

	public Triple[] nextRow() {
		if (this.prefetchedRow != null) {
			Triple[] result = this.prefetchedRow;
			this.prefetchedRow = null;
			return result;
		}
		throw new NoSuchElementException();
	}
	
	public Object next() {
		return nextRow();
	}
	
	public void close() {
		this.explicitlyClosed = true;
		this.sqlIterator.close();
	}
	
	public void remove() {
		throw new RuntimeException("Operation not supported");
	}
	
	private Triple[] tryFetchNextRow() {
		while (this.sqlIterator.hasNext()) {
			Triple[] nextRow = makeTripleRow(this.sqlIterator.nextRow());
			if (nextRow != null) {
				return nextRow;
			}
		}
		this.sqlIterator.close();
		return null;
    }
	
	private Triple[] makeTripleRow(String[] row) {
		Triple[] result = new Triple[tripleMakers.length];
		for (int i = 0; i < tripleMakers.length; i++) {
			Triple triple = tripleMakers[i].makeTriple(row);
			if (triple == null) {
				return null;
			}
			result[i] = triple;
		}
		return result;
	}
}
