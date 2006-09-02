package de.fuberlin.wiwiss.d2rq.rdql;

/** 
 * Helper class for storing the index of a node within a query.
 * 
 * @author jgarbers
 * @version $Id: VariableIndex.java,v 1.1 2006/09/02 23:10:43 cyganiak Exp $
 */
public class VariableIndex implements Comparable {
	public int tripleNr; // >=0

	public int nodeNr; // should be 0..2

	public VariableIndex(int triple, int node) {
		tripleNr = triple;
		nodeNr = node;
	}

	public int hashCode() {
		return tripleNr;
	}

	public boolean equals(Object obj) {
		VariableIndex v = (VariableIndex) obj;
		return tripleNr == v.tripleNr && nodeNr == v.nodeNr;
	}

	public int compareTo(Object obj) {
		VariableIndex v = (VariableIndex) obj;
		int diff = tripleNr - v.tripleNr;
		if (diff == 0)
			diff = nodeNr - v.nodeNr;
		return diff;
	}

	public String toString() {
		return "VariableIndex@<" + tripleNr + "," + nodeNr + ">";
	}
	
}