package de.fuberlin.wiwiss.d2rq;

import java.util.HashMap;

import junit.framework.TestCase;

/**
 * Tests for {@link D2RQResultIterator}.
 *
 * @version $Id: ResultIteratorTest.java,v 1.1 2005/01/24 23:18:31 cyganiak Exp $
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
