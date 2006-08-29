package de.fuberlin.wiwiss.d2rq.find;

import java.util.Collections;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;

/**
 * Tests for {@link D2RQResultIterator}.
 *
 * @version $Id: ApplyTripleMakersIteratorTest.java,v 1.1 2006/08/29 15:13:13 cyganiak Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ApplyTripleMakersIteratorTest extends TestCase {

    public void testApplyTripleMakersIteratorIsLazy() {
        ApplyTripleMakersIterator it = new ApplyTripleMakersIterator(
        		new FakeQueryExecutionIterator(),
        		Collections.singleton("A TripleQuery instance should go here"),
        		Collections.EMPTY_MAP);
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
