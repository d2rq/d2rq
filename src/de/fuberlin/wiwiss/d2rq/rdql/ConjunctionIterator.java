package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Iterator;

/** 
 * Iterates over the elements of a cross product of a set of sets.
 * In logical terms: given a conjunction of <code>disjunctiveTerms</code> in disjunctive
 * normal form (a_1_1 \/ ... \/ a_1_M1) /\ .... /\ (a_n_1 \/ ... \/ a_n_Mn)
 * we want to multiplicate it out into conjunctive normal form
 * (a_1_1 /\ ... /\ a_n_1) \/ .... \/ (a_1_M1 /\ ... /\ a_n_Mn).
 * We can expect to get M1 * ... * Mn factors {@link #estimatedNumberOfResults}.
 * 
 * @author jgarbers
 * @version $Id: ConjunctionIterator.java,v 1.1 2006/09/02 23:10:43 cyganiak Exp $
 */
public class ConjunctionIterator implements Iterator { 
    
    /**
     * the number of disjunctions.
     */
	protected final int n;

    /** 
     * the disjunctions. Type: Object [n][].
     */
	protected final Object[][] disjunctiveTerms; 

	/**
	 * Flag to allow/disallow use of reflexion API.
	 * <code>true</code> is more convenient but may in some cases violate the
	 * security constraints of the code.
	 */
	protected static boolean createTypedArrays = false;

	/**
	 * Java type of a single Term a_i_j.
	 */
	protected Class termType;

	/**
	 * the indices point to the terms in <code>disjunctiveTerms</code> that 
	 * build the next conjunction in conjunctive normal form.
	 */
	protected IndexArray nextIndexArray; // points to 0,0,0... after initialization

	/** 
	 * optionally a resultStore can be given.
	 * This has two advantages: 
	 * 1) it avoids the allocation of a new array in each iteration
	 * 2) it can have the correct array type used for later processing
	 *    and this way avoids casting.
	 * Disadvantage: the array is overridden each time next() is called.
	 * So be very careful with 'storing' iterator results! 
	 */
	protected Object[] resultStore; 
	
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
	
	/** 
	 * Get all conjunctions.
	 * @param disjunctiveTerms see above
	 * @param resultStore optional, of size [estimatedNumberOfResults][n]
	 * @return array of size [estimatedNumberOfResults][n]
	 */
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