package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.Iterator;

public class ConjunctionIterator implements Iterator { // iterates over the
                                                       // conjunctions of a
                                                       // disjunctive normal
                                                       // form
    public final Object[][] disjunctiveTerms; // a conjunction over n
                                              // disjunctive Terms (conjunctive
                                              // normal form)

    public IndexArray nextIndexArray; // points to 0,0,0... after initialization

    public final int n;

    public Object[] resultStore; // optional

    public ConjunctionIterator(Object[][] disjunctiveTerms, Object[] resultStore) {
        this.disjunctiveTerms = disjunctiveTerms;
        this.n = disjunctiveTerms.length;
        this.resultStore = resultStore;
        int[] ranges = new int[n];
        for (int i = 0; i < n; i++)
            ranges[i] = disjunctiveTerms[i].length;
        nextIndexArray = new IndexArray(ranges);
    }

    public boolean hasNext() {
        return nextIndexArray.lastIndexChanged()<n;
    }

    public Object next() {
        return nextArray();
    }

    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Object[] nextArray() {
        Object[] ret = null;
        int upto = nextIndexArray.lastIndexChanged(); 
        if (upto>=n)
            return null;
        if (upto<0)
            upto=n-1;
        if (resultStore == null) {
           ret = new Object[n];
           upto = n-1;
        } else {
           ret = resultStore;
        }
        for (int i = 0; i <= upto; i++) 
           ret[i] = disjunctiveTerms[i][nextIndexArray.counters[i]];
        nextIndexArray.next();
        return ret;
    }
    
	public static void test(){
	    ConjunctionIterator a=new ConjunctionIterator(new String[][]{new String[]{"a","b"},new String[]{"c","d","e"},new String[]{"f","g"}},null);
	    while (a.hasNext()) {
	        Object[] arr=(Object[])(a.next());
	        for (int j=0; j<arr.length; j++) {
	            System.out.print(arr[j]);
	            System.out.print(" ");
	        }	            
	        System.out.println(";");
	    }
	}
	public static void main(String[]args){
	    test();
	}
}