package de.fuberlin.wiwiss.d2rq;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.WrappedIterator;


/**
 * A GraphD2RQ that caches the results of the most recently performed
 * queries on an LRU basis.
 * 
 * @author Holger Knublauch (holger@topquadrant.com)
 */
public class CachingGraphD2RQ extends GraphD2RQ {

	/**
	 * Cache of recently queried triple matches
	 * (TripleMatch -> List<Triple>) 
	 */
	private LinkedHashMap queryCache = new LinkedHashMap(100, 0.75f, true) {
		
		private static final int MAX_ENTRIES = 10000;

	    protected boolean removeEldestEntry(Map.Entry eldest) {
	        return size() > MAX_ENTRIES;
	    }
	};
	
	public CachingGraphD2RQ(Model mapModel, String baseURIForData) throws D2RQException {
		super(mapModel, baseURIForData);
	}
	
	/**
	 * Clears the current cache.  This can be used in case the
	 * database has been changed.
	 */
	public void clearCache() {
		queryCache.clear();
	}

	
	/**
	 * Overloaded to reuse and update the cache.
	 */
	public ExtendedIterator graphBaseFind(TripleMatch m) {

		List cached = (List) queryCache.get(m);
		if(cached != null) {
            return WrappedIterator.create(cached.iterator());
		}
		
		ExtendedIterator it = super.graphBaseFind(m);
		
		final List list = it.toList();
		queryCache.put(m, list);
		return WrappedIterator.create(list.iterator());
	}
}
