package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.TripleMaker;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: UnionOverSameBase.java,v 1.3 2006/09/10 22:18:44 cyganiak Exp $
 */
public class UnionOverSameBase implements RDFRelation {

	/**
	 * Checks if two {@link RDFRelation}s can be combined into
	 * a single SQL statement. Relations can be combined iff
	 * they access the same database and they contain exactly the same
	 * joins and WHERE clauses. If they both contain no joins, they
	 * must contain only columns from the same table.
	 * 
	 * TODO This should be done via Relation.equals?
	 * 
	 * @return <tt>true</tt> if both arguments are combinable
	 */
	public static boolean isSameBase(RDFRelation first, RDFRelation second) {
		if (!first.baseRelation().database().equals(second.baseRelation().database())) {
			return false;
		}
// TODO:  
//		if (first.mightContainDuplicates() || second.mightContainDuplicates()) {
//			return false;
//		}
		if (!first.baseRelation().joinConditions().equals(second.baseRelation().joinConditions())) {
			return false;
		}
		if (!first.baseRelation().condition().equals(second.baseRelation().condition())) {
			return false;
		}
		if (!first.baseRelation().attributeConditions().equals(second.baseRelation().attributeConditions())) {
			return false;
		}
		if (!first.baseRelation().aliases().equals(second.baseRelation().aliases())) {
			return false;
		}
		if (!tables(first).equals(tables(second))) {
			return false;
		}
		return true;
	}

	/**
	 * @return All table names used in the argument
	 */
	private static Set tables(RDFRelation query) {
		Set results = new HashSet();
		Iterator it = query.baseRelation().attributeConditions().keySet().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			results.add(column.getTableName());
		}
		it = query.projectionColumns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			results.add(column.getTableName());
		}
		it = query.baseRelation().condition().columns().iterator();
		while (it.hasNext()) {
			Column column = (Column) it.next();
			results.add(column.getTableName());
		}
		it = query.baseRelation().joinConditions().iterator();
		while (it.hasNext()) {
			Join join = (Join) it.next();
			results.add(join.getFirstTable());
			results.add(join.getSecondTable());
		}
		return results;
	}

	private Relation baseRelation;
	private List tripleMakers;
	private Set projectionColumns = new HashSet();
	
	public UnionOverSameBase(List baseRelations) {
		this.baseRelation = ((RDFRelation) baseRelations.get(0)).baseRelation();
		this.tripleMakers = baseRelations;
		Iterator it = baseRelations.iterator();
		while (it.hasNext()) {
			RDFRelation relation = (RDFRelation) it.next();
			this.projectionColumns.addAll(relation.projectionColumns());
		}
	}

	public Relation baseRelation() {
		return this.baseRelation;
	}
	
	public Set projectionColumns() {
		return this.projectionColumns;
	}
	
	public boolean isUnique() {
		return false;	// TODO Determine uniqueness
	}
	
	public Collection makeTriples(ResultRow row) {
		List result = new ArrayList();
		Iterator it = this.tripleMakers.iterator();
		while (it.hasNext()) {
			TripleMaker relation = (TripleMaker) it.next();
			result.addAll(relation.makeTriples(row));
		}
		return result;
	}
	
	public NodeMaker nodeMaker(int index) {
		throw new UnsupportedOperationException();
	}

	public RDFRelation selectTriple(Triple triplePattern) {
		throw new UnsupportedOperationException();
	}
	
	public RDFRelation renameColumns(ColumnRenamer renamer) {
		throw new UnsupportedOperationException();
	}
}
