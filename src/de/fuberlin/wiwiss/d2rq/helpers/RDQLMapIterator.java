package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdql.Query;
import com.hp.hpl.jena.rdql.QueryEngine;
import com.hp.hpl.jena.rdql.QueryResults;
import com.hp.hpl.jena.rdql.ResultBinding;
import com.hp.hpl.jena.rdql.ResultBindingIterator;

/** 
 * RDQLMapIterator shields the user from knowing the internal classes of Jena.
 * Give it a model and a query (String). 
 * It then behaves as an Iterator, that returns maps.
 * Each map maps variable names to string values.
 * @author jgarbers
 *
 */

public class RDQLMapIterator implements Iterator {
    Query query;
    QueryResults qr;
    boolean swiPrologStyle=false;
    
    public RDQLMapIterator(Model m, String queryString) {
        query=new Query(queryString);
        query.setSource(m);
        qr=new QueryEngine(query).exec();
    }
    
    public void setSwiPrologStyle(boolean yn) {
        swiPrologStyle=yn;
    }
    
    public boolean hasNext() {
        return qr.hasNext();
    }
    public Map nextMap() {
		ResultBinding binding = (ResultBinding) qr.next();
		return resultBindingToMap(binding);
    }
    public Object next() {
		ResultBinding binding = (ResultBinding) qr.next();
		return resultBindingToMap(binding);
    }
    
    protected Object mapValueForObject(Object obj) {
        if (!swiPrologStyle ||  (obj instanceof Resource))
           return obj.toString();
        if (obj instanceof Literal) {
            Literal lit=(Literal)obj;
            String lex=lit.getLexicalForm();
            String datatype=lit.getDatatypeURI();
            if (datatype!=null) 
               return new Object[]{"literal",new Object[] {"type",datatype,lex}}; 
            String lang=lit.getLanguage();
            if (lang!=null)
                return new Object[]{"literal",new Object[] {"lang",lang,lex}}; 
            return new Object[]{"literal", lex};
        } else {
            throw new RuntimeException("mapValueForObject: unexpected Object type " + obj.getClass().toString());
        }
    }
    
	protected Map resultBindingToMap(ResultBinding b) {
		Map m=new HashMap();
		ResultBindingIterator it=b.iterator();
		while (it.hasNext()) {
			it.next();
		    String var=JenaCompatibility.resultBindingIteratorVarName(it);
		    Object val=JenaCompatibility.resultBindingIteratorValue(it,b);
			m.put(var,mapValueForObject(val));
		}
		return m;
	}
	
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
}
