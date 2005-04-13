package de.fuberlin.wiwiss.d2rq;

import java.util.HashMap;

import de.fuberlin.wiwiss.d2rq.map.Database;

import junit.framework.TestCase;

/**
 * Tests for {@link D2RQResultIterator}.
 *
 * @version $Id: ResultIteratorTest.java,v 1.2 2005/04/13 16:56:08 garbers Exp $
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ResultIteratorTest extends TestCase {

    public void testCloseWithoutUse() {
        Database db = new Database("odbc", "jdbc", "jdbcDriver",
                "user", "pass", new HashMap());
        TripleResultSet trs = new TripleResultSet(
                "SELECT * FROM foo", new HashMap(), db);
        D2RQResultIterator it = new D2RQResultIterator();
        it.addTripleResultSet(trs);
        trs.close();
    }
}
