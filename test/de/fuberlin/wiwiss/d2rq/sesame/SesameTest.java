package de.fuberlin.wiwiss.d2rq.sesame;
import de.fuberlin.wiwiss.d2rq.helpers.TestFramework;
import de.fuberlin.wiwiss.d2rq.sesame.D2RQRepository;
import de.fuberlin.wiwiss.d2rq.sesame.D2RQSource;

import org.openrdf.model.Value;
import org.openrdf.sesame.Sesame;
import org.openrdf.sesame.constants.QueryLanguage;
import org.openrdf.sesame.query.QueryResultsTable;
import org.openrdf.sesame.repository.SesameRepository;

/**
 * @author jgarbers
 * @version $Id: SesameTest.java,v 1.3 2006/09/03 13:03:43 cyganiak Exp $
 */
public class SesameTest extends TestFramework {

    public void testSimpleSelect() {
        // jg: code taken from doc/manual/index.html
       try{
            // Initialize repository
            D2RQSource source = new D2RQSource(D2RQMap, "N3"); // jg: adopted to TestFramework
            SesameRepository repos = new D2RQRepository("urn:youRepository", source, Sesame.getService());

            // Query the repository
            String query = "SELECT ?x, ?y WHERE (<http://www.conference.org/conf02004/paper#Paper1>, ?x, ?y)";        
            QueryResultsTable result = repos.performTableQuery(QueryLanguage.RDQL, query);

            // print the result
            int rows = result.getRowCount();
            int cols = result.getColumnCount();
            for(int i = 0; i < rows; i++){
                for(int j = 0; j < cols; j++){
                    Value v = result.getValue(i,j);
                    System.out.print(v.toString() + "    ");                  
                }
                System.out.println();
            }
        } catch(Exception e){
            // catches D2RQException from D2RQSource construcor
            // catches java.io.IOException,
            //         org.openrdf.sesame.query.MalformedQueryException,
            //         org.openrdf.sesame.query.QueryEvaluationException, 
            //         org.openrdf.sesame.config.AccessDeniedException
            //         from performTableQuery
            e.printStackTrace();
        }

    }
    
}
