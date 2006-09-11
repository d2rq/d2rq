package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.sql.ApplyTripleMakerIterator;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;
import de.fuberlin.wiwiss.d2rq.sql.TripleMaker;

/**
 * Tests for {@link D2RQResultIterator}.
 *
 * @version $Id: ApplyTripleMakerIteratorTest.java,v 1.1 2006/09/11 23:22:26 cyganiak Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ApplyTripleMakerIteratorTest extends TestCase {

    public void testApplyTripleMakerIteratorIsLazy() {
        ApplyTripleMakerIterator it = new ApplyTripleMakerIterator(
        		new FakeQueryExecutionIterator(),
        		new TripleMaker() {
					public Collection makeTriples(ResultRow row) {
						return Collections.EMPTY_LIST;
					}
        		});
        it.close();
    }
    
    private class FakeQueryExecutionIterator extends QueryExecutionIterator {
    	FakeQueryExecutionIterator() {
    		super(null, null, null);
    	}
    	public boolean hasNext() {
    		fail("QueryExecutionIterator should be used lazily");
    		return false;
    	}
    }
}
