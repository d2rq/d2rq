package de.fuberlin.wiwiss.d2rq.sesame;

import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.repository.local.LocalService;

/**
 * This implementation for the SesameRepository interface provides a read-only access
 * to a D2RQ Source.
 *
 * @author Oliver Maresch (oliver-maresch@gmx.de)
 * @version $Id: D2RQRepository.java,v 1.6 2006/09/02 20:59:00 cyganiak Exp $
 */
public class D2RQRepository extends LocalRepository implements SesameRepository{
    
 	/**
	 * Creates a read-only D2RQRepository for the supplied Sail.
	 **/
    public D2RQRepository(String id, D2RQSource d2rqSource, LocalService service) {
        super(id, d2rqSource, service);
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(SesameRepository sesameRepository, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(java.io.File file, String str, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.FileNotFoundException, java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(java.io.InputStream inputStream, String str, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(java.io.Reader reader, String str, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(String str, String str1, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(java.net.URL uRL, String str, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addGraph(org.openrdf.model.Graph graph) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addGraph(org.openrdf.model.Graph graph, boolean param) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addGraph(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addGraph(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str, boolean param) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void clear(org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    //TODO wrong documentation
	/**
	 * Extracts data from the repository and reports the triples to the supplied
	 * <tt>RdfDocumentWriter</tt>.
	 *
	 * @param rdfDocWriter The <tt>RdfDocumentWriter</tt> to report the triples to.
	 * @param ontology If <tt>true</tt> the ontological statements will be extracted.
	 * @param instances If <tt>true</tt> the instance (non-schema) statements will be extracted.
	 * @param explicitOnly If <tt>true</tt>, only the explicitly added statements will be extracted.
	 * @param niceOutput If <tt>true</tt>, the extracted statements will be sorted by their subject.
	 **/
    public java.io.InputStream extractRDF(org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, boolean param2, boolean param3, boolean param4) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
        return super.extractRDF(rDFFormat, param, param2, param3, param4);
    }

    /**
     * Returns the ID of the repository
     */
    public String getRepositoryId() {
        return super.getRepositoryId();
    }

    /**
     * Executes an Query and returns the result as a graph.
     * @param queryLanguage indentifier for the used query language (SeRQL, RQL or RDQL)
     * @param str the query string
     * @return the result graph
     */
    public org.openrdf.model.Graph performGraphQuery(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str) throws java.io.IOException, org.openrdf.sesame.query.MalformedQueryException, org.openrdf.sesame.query.QueryEvaluationException, org.openrdf.sesame.config.AccessDeniedException {
        return super.performGraphQuery(queryLanguage, str);
    }

    /**
     * Executes an Query and returns the result as a graph.
     * @param queryLanguage indentifier for the used query language (SeRQL, RQL or RDQL)
     * @param str the query string
     * @param listener the result listener
     */
    public void performGraphQuery(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str, org.openrdf.sesame.query.GraphQueryResultListener graphQueryResultListener) throws java.io.IOException, org.openrdf.sesame.query.MalformedQueryException, org.openrdf.sesame.query.QueryEvaluationException, org.openrdf.sesame.config.AccessDeniedException {
        super.performGraphQuery(queryLanguage, str, graphQueryResultListener);
    }

    /**
     * Executes an Query and reports the result in a table to the supplied listener.
     * @param queryLanguage indentifier for the used query language (SeRQL, RQL or RDQL)
     * @param str the query string
     * @return the query result table
     */
    public org.openrdf.sesame.query.QueryResultsTable performTableQuery(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str) throws java.io.IOException, org.openrdf.sesame.query.MalformedQueryException, org.openrdf.sesame.query.QueryEvaluationException, org.openrdf.sesame.config.AccessDeniedException {
        return super.performTableQuery(queryLanguage, str);
    }

    /**
     * Executes an Query and reports the resulting statements to the specified listener.
     * @param queryLanguage indentifier for the used query language (SeRQL, RQL or RDQL)
     * @param str the query string
     * @param listener the result listener
     */
    public void performTableQuery(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str, org.openrdf.sesame.query.TableQueryResultListener tableQueryResultListener) throws java.io.IOException, org.openrdf.sesame.query.MalformedQueryException, org.openrdf.sesame.query.QueryEvaluationException, org.openrdf.sesame.config.AccessDeniedException {
        super.performTableQuery(queryLanguage, str, tableQueryResultListener);
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void removeGraph(org.openrdf.model.Graph graph) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void removeGraph(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void removeStatements(org.openrdf.model.Resource resource, org.openrdf.model.URI uRI, org.openrdf.model.Value value, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

	/**
	 * Returns true, all D2RQ Repositories have read access.
	 * @return always <tt>true</tt> 
	 **/
    public boolean hasReadAccess() {
        return true;
        }
    
	/**
	 * Returns false, no D2RQ Repositories have write access.
	 * @return always <tt>false</tt> 
	 **/
    public boolean hasWriteAccess() {
        return false;
    }
    
}
