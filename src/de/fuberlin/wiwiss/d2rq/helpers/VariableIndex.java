/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.helpers;

/** 
 * Helper class for storing the index of a node within a query.
 * 
 * @author jgarbers
 *
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