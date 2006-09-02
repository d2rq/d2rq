package de.fuberlin.wiwiss.d2rq.sesame;

import org.openrdf.model.ValueFactory;
import org.openrdf.sesame.sail.StatementIterator;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/** 
 * Wraps a Jena ExtendedIterator with Jena Statements and maps the Jena Statements to Sesame Statments
 *
 * @author Oliver Maresch (oliver-maresch@gmx.de)
 * @version $Id: D2RQStatementIterator.java,v 1.5 2006/09/02 20:59:00 cyganiak Exp $
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

    public boolean hasNext(){
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
