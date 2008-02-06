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
 * TODO: Remodel as a foreign key constraint? Values of side1 must be in side2?
 *       At the moment, A=B is considered .equals(B=A) 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Join.java,v 1.8 2008/02/06 17:26:13 cyganiak Exp $
 */
public class Join {
	private List attributes1 = new ArrayList();
	private List attributes2 = new ArrayList();
	private RelationName table1 = null;
	private RelationName table2 = null;
	private Map otherSide = new HashMap(4); 

	public Join(Attribute oneSide, Attribute otherSide) {
		this(Collections.singletonList(oneSide), Collections.singletonList(otherSide));
	}
	
	public Join(List oneSideAttributes, List otherSideAttributes) {
		RelationName oneRelation = ((Attribute) oneSideAttributes.get(0)).relationName();
		RelationName otherRelation = ((Attribute) otherSideAttributes.get(0)).relationName();
		this.attributes1 = oneSideAttributes;
		this.attributes2 = otherSideAttributes;
		this.table1 = oneRelation;
		this.table2 = otherRelation;
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
		result.append(" <=> ");
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
		return new Join(oneSide, otherSide);
	}
}
