package de.fuberlin.wiwiss.d2rq.find;

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.map.TripleMaker;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;

/**
 * Tests for {@link D2RQResultIterator}.
 *
 * @version $Id: ApplyTripleMakersIteratorTest.java,v 1.3 2006/09/09 20:51:49 cyganiak Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ApplyTripleMakersIteratorTest extends TestCase {

    public void testApplyTripleMakersIteratorIsLazy() {
        ApplyTripleMakersIterator it = new ApplyTripleMakersIterator(
        		new FakeQueryExecutionIterator(),
        		new TripleMaker() {
					public Collection makeTriples(String[] row) {
						return Collections.EMPTY_LIST;
					}
        		});
        it.close();
    }
    
    private class FakeQueryExecutionIterator extends QueryExecutionIterator {
    	FakeQueryExecutionIterator() {
    		super(null, null);
    	}
    	public boolean hasNext() {
    		fail("QueryExecutionIterator should be used lazily");
    		return false;
    	}
    }
}
