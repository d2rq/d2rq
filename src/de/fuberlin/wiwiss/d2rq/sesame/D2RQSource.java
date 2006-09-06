/*
 * D2RQSource.java
 *
 * Created on 13. September 2005, 17:44
 */

package de.fuberlin.wiwiss.d2rq.sesame;

import org.openrdf.sesame.sail.RdfSource;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.GraphD2RQ;



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
    public D2RQSource(String d2rqMapUrl, String language, String baseURI) 
    throws D2RQException{
        Model d2rqMap = ModelFactory.createDefaultModel();
        d2rqMap.read(d2rqMapUrl, language);
        this.d2rqGraph = new GraphD2RQ(d2rqMap, baseURI);
        this.rdfSource = new org.openrdf.sesame.sailimpl.memory.RdfSource();
    }

    /**
     * Returns all know namespaces within the source.
     * @return interator over all known namespaces
     */
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

    /**
     * Rerturns the ValueFactory of the RdfSource.
     * @return the ValueFactory
     */
    public org.openrdf.model.ValueFactory getValueFactory() {
        return rdfSource.getValueFactory();
    }

    /**
     * Returns true, if the source contains the specified statement.
     * @param resource the RDF resource of the subject
     * @param uRI the URI of the statment property
     * @param value the RDF resource of the object
     * @return true, if the sourec contains the specified statement, false otherwise
     */
    public boolean hasStatement(org.openrdf.model.Resource resource, org.openrdf.model.URI uRI, org.openrdf.model.Value value) {
        if(getStatements(resource, uRI,value).hasNext()){
            return true;
        }
        return false;
    }

    /**
     * Initialize the repository.
     * @param configParams
     */
    public void initialize(java.util.Map configParams) throws org.openrdf.sesame.sail.SailInitializationException {
        this.rdfSource.initialize(configParams);
    }

    /**
     * Optimize queries (no opitmization in this implementation)
     * @param query the unoptimized query
     * @return the optimized query
     */
    public org.openrdf.sesame.sail.query.Query optimizeQuery(org.openrdf.sesame.sail.query.Query query) {
        return query;
    }

    /**
     * Shuts down the repository.
     */
    public void shutDown() {
        this.rdfSource.shutDown();
    }
    
}
