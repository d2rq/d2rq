package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents an SQL join between two tables, spanning one or more columns.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Join.java,v 1.9 2009/08/02 19:22:10 fatorange Exp $
 */
public class Join {
	private List attributes1 = new ArrayList();
	private List attributes2 = new ArrayList();
	private RelationName table1 = null;
	private RelationName table2 = null;
	private Map otherSide = new HashMap(4);
	
	private int joinDirection;
	
	public static final int DIRECTION_UNDIRECTED = 0;
	public static final int DIRECTION_LEFT = 1;
	public static final int DIRECTION_RIGHT = 2;
	
	public static final String[] joinOperators = {"=", "<=", "=>"};	
	
	public Join(Attribute oneSide, Attribute otherSide, int joinDirection) {
		this(Collections.singletonList(oneSide), Collections.singletonList(otherSide), joinDirection);
	}
	
	public Join(List oneSideAttributes, List otherSideAttributes, int joinDirection) {
		RelationName oneRelation = ((Attribute) oneSideAttributes.get(0)).relationName();
		RelationName otherRelation = ((Attribute) otherSideAttributes.get(0)).relationName();
		this.attributes1 = oneSideAttributes;
		this.attributes2 = otherSideAttributes;
		this.table1 = oneRelation;
		this.table2 = otherRelation;
		this.joinDirection = joinDirection;
		for (int i = 0; i < this.attributes1.size(); i++) {
			Attribute a1 = (Attribute) this.attributes1.get(i);
			Attribute a2 = (Attribute) this.attributes2.get(i);
			this.otherSide.put(a1, a2);
			this.otherSide.put(a2, a1);
		}
	}
	
	public boolean isSameTable() {
		return this.table1.equals(this.table2);
	}
	
	public boolean containsColumn(Attribute column) {
		return this.attributes1.contains(column) ||
				this.attributes2.contains(column);
	}

	public RelationName table1() {
		return this.table1;
	}
	
	public RelationName table2() {
		return this.table2;
	}

	public List attributes1() {
		return this.attributes1;
	}

	public List attributes2() {
		return this.attributes2;
	}
	
	public int joinDirection() {
		return this.joinDirection;
	}

	public Attribute equalAttribute(Attribute column) {
		return (Attribute) this.otherSide.get(column);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("Join(");
		Iterator it = this.attributes1.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			result.append(attribute.qualifiedName());
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(joinDirection == DIRECTION_UNDIRECTED ? " <=> " : (joinDirection == DIRECTION_RIGHT ? " => " : " <= "));
		it = this.attributes2.iterator();
		while (it.hasNext()) {
			Attribute attribute = (Attribute) it.next();
			result.append(attribute.qualifiedName());
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}

	public int hashCode() {
		return this.attributes1.hashCode() ^ this.attributes2.hashCode();
	}
	
	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof Join)) {
			return false;
		}
		Join otherJoin = (Join) otherObject;
		return 
				(this.attributes1.equals(otherJoin.attributes1) 
						&& this.attributes2.equals(otherJoin.attributes2))
				||
				(this.attributes1.equals(otherJoin.attributes2) 
						&& this.attributes2.equals(otherJoin.attributes1));
	}
	
	public Join renameColumns(ColumnRenamer columnRenamer) {
		List oneSide = new ArrayList();
		List otherSide = new ArrayList();
		Iterator it = attributes1().iterator();
		while (it.hasNext()) {
			Attribute column = (Attribute) it.next();
			oneSide.add(columnRenamer.applyTo(column));
			otherSide.add(columnRenamer.applyTo(equalAttribute(column)));
		}
		return new Join(oneSide, otherSide, joinDirection);
	}
}
