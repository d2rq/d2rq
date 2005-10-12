/*
 * D2RQSource.java
 *
 * Created on 13. September 2005, 17:44
 */

package de.fuberlin.wiwiss.d2rq.sesame;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.GraphD2RQ;

import org.openrdf.model.GraphException;
import org.openrdf.model.ValueFactory;
import org.openrdf.sesame.sail.RdfSource;



/** Implementation of the Sesame RdfSource Interface for D2RQ
 * This class wraps the D2RQGraph into an implementation of the Sesame RdfSource
 * interface. 
 *
 * @author Oliver Maresch (oliver-maresch@gmx.de)
 */
public class D2RQSource implements RdfSource {
    
    private GraphD2RQ d2rqGraph = null;
    
    private org.openrdf.sesame.sailimpl.memory.RdfSource rdfSource= null;
  
    /** Creates a new instance of D2RQSource 
     *  @param d2rqMapUrl URL of the mapping file for D2RQ
     *  @param language Identifies the format of the rdf data in the mapping file. Should be one of the values "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE" and "N3". The default value, represented by <code>null</code>, is "RDF/XML".
     */
    public D2RQSource(String d2rqMapUrl, String language) 
    throws GraphException {
        try{
            Model d2rqMap = ModelFactory.createDefaultModel();
            d2rqMap.read(d2rqMapUrl, language);
            this.d2rqGraph = new GraphD2RQ(d2rqMap);
        } catch(D2RQException e){
            throw new GraphException("Couldn't load D2RQ mapping from Model.", e);
        }
        this.rdfSource = new org.openrdf.sesame.sailimpl.memory.RdfSource();
    }

    public org.openrdf.sesame.sail.NamespaceIterator getNamespaces() {
        return rdfSource.getNamespaces();
    }
    
    
    /** 
     * Maps the call of the Sesame RdfSource.getStatement interface to the 
     * Jena Graph.find(spo) interface and returns an implemenation of the
     * Sesame StatementIterator, which wraps the results of the Jena Graph 
     * interface. 
     */
    public org.openrdf.sesame.sail.StatementIterator getStatements(org.openrdf.model.Resource resource, org.openrdf.model.URI uRI, org.openrdf.model.Value value) {

        // Map the Sesame resources to Jena
        Node jenaSubject = SesameJenaUtilities.makeJenaSubject(resource);
        Node jenaPredicate = SesameJenaUtilities.makeJenaPredicate(uRI);
        Node jenaObject = SesameJenaUtilities.makeJenaObject(value);
        Triple pattern = new Triple(jenaSubject, jenaPredicate, jenaObject);

        // Query the graph
        ExtendedIterator resultIterator = d2rqGraph.find(pattern);
        
        return new D2RQStatementIterator(resultIterator, rdfSource.getValueFactory());
    }

    public org.openrdf.model.ValueFactory getValueFactory() {
        return rdfSource.getValueFactory();
    }

    public boolean hasStatement(org.openrdf.model.Resource resource, org.openrdf.model.URI uRI, org.openrdf.model.Value value) {
        if(getStatements(resource, uRI,value).hasNext()){
            return true;
        }
        return false;
    }

    public void initialize(java.util.Map configParams) throws org.openrdf.sesame.sail.SailInitializationException {
        this.rdfSource.initialize(configParams);
    }

    public org.openrdf.sesame.sail.query.Query optimizeQuery(org.openrdf.sesame.sail.query.Query query) {
        return query;
    }

    public void shutDown() {
        this.rdfSource.shutDown();
    }
    
}
