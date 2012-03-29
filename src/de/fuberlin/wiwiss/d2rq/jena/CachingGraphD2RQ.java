package de.fuberlin.wiwiss.d2rq.jena;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.WrappedIterator;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.map.Mapping;


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
	private Map<TripleMatch,List<Triple>> queryCache = 
		new LinkedHashMap<TripleMatch,List<Triple>>(100, 0.75f, true) {
		private static final int MAX_ENTRIES = 10000;
		@Override
	    protected boolean removeEldestEntry(Map.Entry<TripleMatch,List<Triple>> eldest) {
	        return size() > MAX_ENTRIES;
	    }
	};
	
	public CachingGraphD2RQ(Mapping mapping) throws D2RQException {
		super(mapping);
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
	@Override
	public ExtendedIterator<Triple> graphBaseFind(TripleMatch m) {
		List<Triple> cached = queryCache.get(m);
		if (cached != null) {
            return WrappedIterator.create(cached.iterator());
		}
		ExtendedIterator<Triple> it = super.graphBaseFind(m);
		final List<Triple> list = it.toList();
		queryCache.put(m, list);
		return WrappedIterator.create(list.iterator());
	}
}
