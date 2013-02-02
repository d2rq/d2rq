package org.d2rq.db.renamer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.d2rq.db.expr.ColumnListEquality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.util.OpRenamer;
import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.IdentifierList;
import org.d2rq.db.schema.TableName;
import org.d2rq.nodes.FixedNodeMaker;
import org.d2rq.nodes.NodeMaker;
import org.d2rq.nodes.NodeMakerVisitor;
import org.d2rq.nodes.TypedNodeMaker;


/**
 * Something that can rename tables and columns in various objects.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class Renamer {
	
	/**
	 * An optimized ColumnRenamer that leaves every column unchanged
	 */
	public final static Renamer IDENTITY = new Renamer() {
		public ColumnName applyTo(ColumnName original) { return original; }
		public TableName applyTo(TableName original) { return original; }
		public String toString() { return "Renamer.NULL"; }
	};
	
	/**
	 * @param original A column name
	 * @return The renamed version of that column name, or the same column name
	 * 		if the renamer does not apply to this argument
	 */
	public abstract ColumnName applyTo(ColumnName original);
	
	/**
	 * @param original A table name
	 * @return The renamed version of that table name, or the original if the
	 * 		renamer does not apply to this argument
	 */
	public abstract TableName applyTo(TableName original);
	
	/**
	 * @param original An expression
	 * @return An expression with all columns renamed according to this Renamer
	 */
	public Expression applyTo(Expression original) {
		return original.rename(this);
	}

	public Identifier applyTo(TableName table, Identifier identifier) {
		return applyTo(table.qualifyIdentifier(identifier)).getColumn();
	}
	
	/**
	 * Renames tables/columns in a {@link ForeignKey}.
	 * 
	 * @param table The table on which the foreign key is defined
	 * @param foreignKey The foreign key to be renamed
	 * @return A foreign key with all columns renamed according to this Renamer
	 */
	public ForeignKey applyTo(TableName table, ForeignKey foreignKey) {
		return new ForeignKey(
				applyTo(table, foreignKey.getLocalColumns()), 
				applyTo(table, foreignKey.getReferencedColumns()),
				applyTo(foreignKey.getReferencedTable()));
	}
	
	public Set<ColumnListEquality> applyToJoinConditions(Set<ColumnListEquality> joins) {
		Set<ColumnListEquality> result = new HashSet<ColumnListEquality>();
		for (ColumnListEquality join: joins) {
			result.add((ColumnListEquality) applyTo(join));
		}
		return result;
	}

	public IdentifierList applyTo(TableName table, IdentifierList key) {
		List<Identifier> result = new ArrayList<Identifier>();
		for (Identifier column: key) {
			result.add(applyTo(table, column));
		}
		return IdentifierList.createFromIdentifiers(result);
	}
	
	public List<OrderSpec> applyTo(List<OrderSpec> orderSpecs) {
		List<OrderSpec> result = new ArrayList<OrderSpec>(orderSpecs.size());
		for (OrderSpec spec: orderSpecs) {
			result.add(new OrderSpec(applyTo(spec.getExpression()), spec.isAscending()));
		}
		return result;
	}
	
	public Set<DatabaseOp> applyToTabulars(Set<DatabaseOp> originals) {
		Set<DatabaseOp> result = new HashSet<DatabaseOp>();
		for (DatabaseOp original: originals) {
			result.add(new OpRenamer(original, this).getResult());
		}
		return result;
	}
	
	public NodeMaker applyTo(final NodeMaker nodeMaker) {
		return new NodeMakerVisitor() {
			private NodeMaker result = nodeMaker;
			public NodeMaker getResult() { nodeMaker.accept(this); return result; }
			public void visit(TypedNodeMaker nodeMaker) {
				result = new TypedNodeMaker(nodeMaker.getNodeType(), 
							nodeMaker.getValueMaker().rename(Renamer.this));
			}
			public void visit(FixedNodeMaker nodeMaker) {}
			public void visitEmpty() {}
		}.getResult();
	}
	
	public List<ColumnName> applyToColumns(List<ColumnName> columns) {
		List<ColumnName> result = new ArrayList<ColumnName>(columns.size());
		for (ColumnName column: columns) {
			result.add(applyTo(column));
		}
		return result;
	}
	
	public ColumnList applyTo(ColumnList columns) {
		List<ColumnName> renamed = new ArrayList<ColumnName>();
		for (ColumnName column: columns) {
			renamed.add(applyTo(column));
		}
		return ColumnList.create(renamed);
	}
}
