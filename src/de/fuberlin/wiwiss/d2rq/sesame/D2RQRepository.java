/*
 * D2RQRepository.java
 *
 * Created on 13. September 2005, 17:36
 *
 */

package de.fuberlin.wiwiss.d2rq.sesame;

import org.openrdf.sesame.repository.SesameRepository;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.repository.local.LocalService;
import org.openrdf.sesame.sail.RdfSource;

/**
 *
 * @author Oliver Maresch (oliver-maresch@gmx.de)
 */
public class D2RQRepository extends LocalRepository implements SesameRepository{
    
    /** Creates a new instance of D2RQRepository */
    public D2RQRepository(String id, RdfSource rdfSource, LocalService service) {
        super(id, rdfSource, service);
    }

    public void addData(SesameRepository sesameRepository, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void addData(java.io.File file, String str, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.FileNotFoundException, java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void addData(java.io.InputStream inputStream, String str, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void addData(java.io.Reader reader, String str, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void addData(String str, String str1, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void addData(java.net.URL uRL, String str, org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void addGraph(org.openrdf.model.Graph graph) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void addGraph(org.openrdf.model.Graph graph, boolean param) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void addGraph(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void addGraph(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str, boolean param) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void clear(org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public java.io.InputStream extractRDF(org.openrdf.sesame.constants.RDFFormat rDFFormat, boolean param, boolean param2, boolean param3, boolean param4) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
        return super.extractRDF(rDFFormat, param, param2, param3, param4);
    }

    public String getRepositoryId() {
        return super.getRepositoryId();
    }

    public org.openrdf.model.Graph performGraphQuery(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str) throws java.io.IOException, org.openrdf.sesame.query.MalformedQueryException, org.openrdf.sesame.query.QueryEvaluationException, org.openrdf.sesame.config.AccessDeniedException {
        return super.performGraphQuery(queryLanguage, str);
    }

    public void performGraphQuery(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str, org.openrdf.sesame.query.GraphQueryResultListener graphQueryResultListener) throws java.io.IOException, org.openrdf.sesame.query.MalformedQueryException, org.openrdf.sesame.query.QueryEvaluationException, org.openrdf.sesame.config.AccessDeniedException {
        super.performGraphQuery(queryLanguage, str, graphQueryResultListener);
    }

    public org.openrdf.sesame.query.QueryResultsTable performTableQuery(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str) throws java.io.IOException, org.openrdf.sesame.query.MalformedQueryException, org.openrdf.sesame.query.QueryEvaluationException, org.openrdf.sesame.config.AccessDeniedException {
        return super.performTableQuery(queryLanguage, str);
    }

    public void performTableQuery(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str, org.openrdf.sesame.query.TableQueryResultListener tableQueryResultListener) throws java.io.IOException, org.openrdf.sesame.query.MalformedQueryException, org.openrdf.sesame.query.QueryEvaluationException, org.openrdf.sesame.config.AccessDeniedException {
        super.performTableQuery(queryLanguage, str, tableQueryResultListener);
    }

    public void removeGraph(org.openrdf.model.Graph graph) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void removeGraph(org.openrdf.sesame.constants.QueryLanguage queryLanguage, String str) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public void removeStatements(org.openrdf.model.Resource resource, org.openrdf.model.URI uRI, org.openrdf.model.Value value, org.openrdf.sesame.admin.AdminListener adminListener) throws java.io.IOException, org.openrdf.sesame.config.AccessDeniedException {
    }

    public boolean hasReadAccess() {
        return true;
    }
    
    public boolean hasWriteAccess() {
        return false;
    }
    
}
