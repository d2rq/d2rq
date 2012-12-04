package org.d2rq.values;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.lang.TranslationTable;
import org.d2rq.nodes.NodeSetFilter;


/**
 * Describes a set of strings that are obtained in some way
 * from one or more database columns.
 * <p>
 * Typical implementations are {@link ColumnValueMaker} (describing the set
 * of strings contained in one database column), {@link TemplateValueMaker}
 * (describing the set of strings that is obtained by sticking
 * the values of several database fields into a string pattern),
 * and {@link BlankNodeIDValueMaker} (similar).
 * <p>
 * There are several other implementations that modify the behaviour
 * of another underlying ValueMaker, implementing the Decorator
 * pattern. This includes {@link TranslationTable}, TranslatingValueMaker
 * (translates values using a translation table or translation class)
 * and the various value restrictions (@link RegexRestriction et. al.).
 * <p>
 * ValueMakers are used by <tt>NodeMaker</tt>s. A node maker
 * wraps the strings into Jena nodes, thus creating a description
 * of a set of RDF nodes.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public interface ValueMaker {
    
	/**
	 * A value maker that never produces a value.
	 */
	public static final ValueMaker NULL = new ValueMaker() {
		public boolean matches(String value) { return false; }
		public Expression valueExpression(String value, DatabaseOp tabular, Vendor vendor) {
			return Expression.FALSE;
		} 
		public Set<ProjectionSpec> projectionSpecs() {
			return Collections.emptySet();
		}
		public String makeValue(ResultRow row) {return null;}
		public void describeSelf(NodeSetFilter c) {c.limitToEmptySet();}
		public ValueMaker rename(Renamer renamer) {return this;}
		public List<OrderSpec> orderSpecs(boolean ascending) {
			return Collections.emptyList();
		}
	};
	
	/**
	 * Indicates whether the node maker is capable of producing a given value,
	 * as far as can be told without having access to the actual database.
	 * 
	 * @return <code>true</code> iff this node maker is capable of producing the value
	 */
	boolean matches(String value);
	
	/** 
	 * A SQL expression that selects only rows where this value maker
	 * produces the specified value. {@link Expression#FALSE} if this
	 * value maker is incapable of producing the value.
	 * 
	 * @param value A value 
	 * @param tabular The table to which the expression will be applicable
	 * @param vendor A vendor instance for datatyping etc.
	 * @return An expression that selects rows that produce this value
	 */
	Expression valueExpression(String value, DatabaseOp tabular, Vendor vendor);

	/**
	 * Returns a set of all {@link ProjectionSpec}s containing data necessary
	 * for this ValueSource.
	 * @return a set of {@link ProjectionSpec}s
	 */
	Set<ProjectionSpec> projectionSpecs();

	/**
	 * Retrieves a value from a database row according to some rule or pattern.
	 * @param row the database row
	 * @return a value created from the row
	 */
	String makeValue(ResultRow row);
	
	void describeSelf(NodeSetFilter c);

	ValueMaker rename(Renamer renamer);
	
	List<OrderSpec> orderSpecs(boolean ascending);
}
