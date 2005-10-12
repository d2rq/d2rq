/*
 * D2RQStatementIterator.java
 *
 * Created on 15. September 2005, 17:52
 *
 */

package de.fuberlin.wiwiss.d2rq.sesame;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.sesame.sail.StatementIterator;

/** Wraps a Jena ExtendedIterator with Jena Statements and maps the Jena Statements to Sesame Statments
 *
 * @author Oliver Maresch (oliver-maresch@gmx.de)
 */
public class D2RQStatementIterator implements StatementIterator {
    
    private ExtendedIterator iterator = null;
    
    private ValueFactory valueFactory = null;
    
    /** Creates a new instance of D2RQStatementIterator */
    public D2RQStatementIterator(ExtendedIterator d2rq, ValueFactory valueFactory) {
        this.iterator = d2rq;
        this.valueFactory = valueFactory;
    }

    public void close() {
        this.iterator.close();
    }

    public boolean hasNext() {
        return this.iterator.hasNext();
    }
    
    public org.openrdf.model.Statement next() {
        if(iterator.hasNext()){
            Triple t = (Triple) iterator.next();
            return SesameJenaUtilities.makeSesameStatement(t, valueFactory);            
        }
        return null;
    }
    
}
