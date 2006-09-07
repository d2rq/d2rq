package de.fuberlin.wiwiss.d2rq.sesame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.openrdf.sesame.admin.AdminListener;
import org.openrdf.sesame.config.AccessDeniedException;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.constants.RDFFormat;
import org.openrdf.sesame.query.GraphQueryResultListener;
import org.openrdf.sesame.query.MalformedQueryException;
import org.openrdf.sesame.query.QueryEvaluationException;
import org.openrdf.sesame.query.QueryResultsTable;
import org.openrdf.sesame.query.TableQueryResultListener;
import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.repository.local.LocalService;

/**
 * This implementation for the SesameRepository interface provides a read-only access
 * to a D2RQ Source.
 *
 * @author Oliver Maresch (oliver-maresch@gmx.de)
 * @version $Id: D2RQRepository.java,v 1.7 2006/09/07 22:04:32 cyganiak Exp $
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
    public void addData(SesameRepository sesameRepository, AdminListener adminListener) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(File file, String str, RDFFormat rDFFormat, boolean param, AdminListener adminListener) throws FileNotFoundException, IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(InputStream inputStream, String str, RDFFormat rDFFormat, boolean param, AdminListener adminListener) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(Reader reader, String str, RDFFormat rDFFormat, boolean param, AdminListener adminListener) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(String str, String str1, RDFFormat rDFFormat, boolean param, AdminListener adminListener) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addData(java.net.URL uRL, String str, RDFFormat rDFFormat, boolean param, AdminListener adminListener) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addGraph(org.openrdf.model.Graph graph) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addGraph(org.openrdf.model.Graph graph, boolean param) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addGraph(QueryLanguage queryLanguage, String str) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void addGraph(QueryLanguage queryLanguage, String str, boolean param) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void clear(AdminListener adminListener) throws IOException, AccessDeniedException {
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
    public InputStream extractRDF(RDFFormat rdfDocWriter, boolean ontology, boolean instances, boolean explicitOnly, boolean niceOutput) throws IOException, AccessDeniedException {
        return super.extractRDF(rdfDocWriter, ontology, instances, explicitOnly, niceOutput);
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
    public org.openrdf.model.Graph performGraphQuery(QueryLanguage queryLanguage, String str) throws IOException, MalformedQueryException, QueryEvaluationException, AccessDeniedException {
        return super.performGraphQuery(queryLanguage, str);
    }

    /**
     * Executes an Query and returns the result as a graph.
     * @param queryLanguage indentifier for the used query language (SeRQL, RQL or RDQL)
     * @param str the query string
     * @param graphQueryResultListener the result listener
     */
    public void performGraphQuery(QueryLanguage queryLanguage, String str, GraphQueryResultListener graphQueryResultListener) throws IOException, MalformedQueryException, QueryEvaluationException, AccessDeniedException {
        super.performGraphQuery(queryLanguage, str, graphQueryResultListener);
    }

    /**
     * Executes an Query and reports the result in a table to the supplied listener.
     * @param queryLanguage indentifier for the used query language (SeRQL, RQL or RDQL)
     * @param str the query string
     * @return the query result table
     */
    public QueryResultsTable performTableQuery(QueryLanguage queryLanguage, String str) throws IOException, MalformedQueryException, QueryEvaluationException, AccessDeniedException {
        return super.performTableQuery(queryLanguage, str);
    }

    /**
     * Executes an Query and reports the resulting statements to the specified listener.
     * @param queryLanguage indentifier for the used query language (SeRQL, RQL or RDQL)
     * @param str the query string
     * @param tableQueryResultListener the result listener
     */
    public void performTableQuery(QueryLanguage queryLanguage, String str, TableQueryResultListener tableQueryResultListener) throws IOException, MalformedQueryException, QueryEvaluationException, AccessDeniedException {
        super.performTableQuery(queryLanguage, str, tableQueryResultListener);
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void removeGraph(org.openrdf.model.Graph graph) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void removeGraph(QueryLanguage queryLanguage, String str) throws IOException, AccessDeniedException {
    }

    /**
     * Performs no action, because D2RQRepositories are always write protected.
     */
    public void removeStatements(org.openrdf.model.Resource resource, org.openrdf.model.URI uRI, org.openrdf.model.Value value, AdminListener adminListener) throws IOException, AccessDeniedException {
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
