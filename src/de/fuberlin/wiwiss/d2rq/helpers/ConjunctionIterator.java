package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.Iterator;

public class ConjunctionIterator implements Iterator { // iterates over the
	// conjunctions of a
	// disjunctive normal
	// form
	protected final Object[][] disjunctiveTerms; // a conjunction over n

	protected static boolean createTypedArrays = false;

	// disjunctive Terms (conjunctive
	// normal form)
	protected Class termType;

	protected IndexArray nextIndexArray; // points to 0,0,0... after initialization

	protected final int n;

	protected Object[] resultStore; // optional
	protected boolean resultStoreIsInitialized;
	
	protected int estimatedNumberOfResults;

	public ConjunctionIterator(Object[][] disjunctiveTerms, Object[] resultStore) {
		this.disjunctiveTerms = disjunctiveTerms;
		Class aaT=disjunctiveTerms.getClass();
		Class aT=aaT.getComponentType();
		termType = aT.getComponentType();

		this.n = disjunctiveTerms.length;
		this.resultStore = resultStore;
		int[] ranges = new int[n];
		if (n>0)
			estimatedNumberOfResults=1;
		else
			estimatedNumberOfResults=0;
			
		for (int i = 0; i < n; i++) {
			int dim=disjunctiveTerms[i].length;
			ranges[i] = dim;
			estimatedNumberOfResults*=dim;
		}
		nextIndexArray = new IndexArray(ranges);
		resultStoreIsInitialized=false;
	}
	
	public boolean hasNext() {
		return nextIndexArray.lastIndexChanged() < n;
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
		if (upto >= n)
			return null;
		if (!resultStoreIsInitialized) {
			resultStoreIsInitialized=true;
			upto=n-1;
		} else if (upto < 0 ) 
			upto = n - 1;
		if (resultStore == null) {
			if (createTypedArrays)
				ret = (Object[]) java.lang.reflect.Array.newInstance(termType,
						n);
			else
				ret = new Object[n];
			upto = n - 1;
		} else {
			ret = resultStore;
		}
		for (int i = 0; i <= upto; i++)
			ret[i] = disjunctiveTerms[i][nextIndexArray.counters[i]];
		nextIndexArray.next();
		return ret;
    }	
	


	public static boolean isCreateTypedArrays() {
		return createTypedArrays;
	}
	public static void setCreateTypedArrays(boolean createTypedArrays) {
		ConjunctionIterator.createTypedArrays = createTypedArrays;
	}
	public IndexArray getNextIndexArray() {
		return nextIndexArray;
	}
	public void setNextIndexArray(IndexArray nextIndexArray) {
		this.nextIndexArray = nextIndexArray;
	}
	public Object[] getResultStore() {
		return resultStore;
	}
	public void setResultStore(Object[] resultStore) {
		this.resultStore = resultStore;
		resultStoreIsInitialized=false;
	}
	public Class getTermType() {
		return termType;
	}
	public void setTermType(Class termType) {
		this.termType = termType;
	}
	public int getEstimatedNumberOfResults() {
		return estimatedNumberOfResults;
	}
	
	
	public static Object[][] allConjunctions(Object[][] disjunctiveTerms, Object[][] resultStore) {
		ConjunctionIterator it=new ConjunctionIterator(disjunctiveTerms,null);
		int dim1=it.getEstimatedNumberOfResults();
		int dim2=disjunctiveTerms.length;
		if (resultStore==null) {
			if (createTypedArrays) {
				int[] dims=new int[]{dim1,dim2};
				resultStore = (Object[][]) java.lang.reflect.Array.newInstance(it.termType,
						dims);
			} else {
				resultStore = new Object[dim1][dim2];
			}
		} else {
			int d=resultStore.length;
			if (d<dim1)
				resultStore=null;
			for (int i=0; i<dim1 && resultStore!=null; i++) {
				if (resultStore[i]==null || resultStore[i].length<dim2) 
					resultStore=null;
			}
			if (resultStore==null) {
				throw new RuntimeException("Insufficient result Store");
			}
		}
		for (int i=0; i<dim1;i++) {
			it.setResultStore(resultStore[i]);
			if (!it.hasNext()) {
				throw new RuntimeException("Internal Error");
			}
			it.next();
		}
		return resultStore;
	}
}