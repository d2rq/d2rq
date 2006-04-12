/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * @author jg
 *
 */
public class TestHelpers extends TestCase {
	
	String[][] cnf; // conjunctive normal form
	String[] resultStore;
	String[][] expectedResults;
	
	public void setUp() {
		cnf=new String[][] {{ "a", "b" }, { "c", "d", "e" }, { "f", "g" } };
		resultStore=new String[cnf.length];
		expectedResults=new String[][]{
			{"a","c","f"},{"b","c","f"},{"a","d","f"},{"b","d","f"},{"a","e","f"},{"b","e","f"},
			{"a","c","g"},{"b","c","g"},{"a","d","g"},{"b","d","g"},{"a","e","g"},{"b","e","g"}
		};
		// new String[][] {
		//		new String[] { "a", "b" }, 
		//		new String[] { "c", "d", "e" },
		//		new String[] { "f", "g" } }
	}

	public void xTestPrintConjunctionIteratorResults() {
		ConjunctionIterator a = new ConjunctionIterator(cnf, null);
		while (a.hasNext()) {
			Object[] arr = (Object[]) (a.next());
			for (int j = 0; j < arr.length; j++) {
				System.out.print(arr[j]);
				System.out.print(" ");
			}
			System.out.println(";");
		}
	}

	public void testConjunctionIterator() {
		ConjunctionIterator a = new ConjunctionIterator(cnf, null);
		Object[] firstResult=null;
		if (a.hasNext())
			firstResult=(Object[])a.next();
		assertTrue(Arrays.equals(firstResult,expectedResults[0]));
	}	

	public void testAllConjunctions() {
		ConjunctionIterator.setCreateTypedArrays(true);
		String[][] results=(String[][])ConjunctionIterator.allConjunctions(cnf,null);
		assertEquals(results.getClass(), expectedResults.getClass());
		boolean sameSize=(results.length == expectedResults.length);
		assertTrue(sameSize);
		if (sameSize)
			for (int i=0; i<results.length; i++)
				assertTrue(Arrays.equals(results[i],expectedResults[i]));
	}
}
