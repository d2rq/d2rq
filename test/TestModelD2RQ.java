/*
  (c) Copyright 2004, Chris Bizer, Freie Universitaet Berlin
*/
import de.fuberlin.wiwiss.d2rq.*;

import java.io.*;
import java.util.*;
import com.hp.hpl.jena.rdf.model.* ;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.datatypes.*;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdql.*;

/**
 * Test Class for Model D2RQ
 *
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
public class TestModelD2RQ {

    private final static String D2RQMap = "C:/D2RQ/maps/ISWC-d2rq.n3";
    static ModelD2RQ d2rqModel;

    public static void main(String[] args) {

            System.out.println("--------------------------------------------");
            System.out.println("D2RQ Model Test Started");
            System.out.println("--------------------------------------------");
            try{
               d2rqModel = new ModelD2RQ(D2RQMap);
               // d2rqModel = new ModelD2RQ(D2RQMap, "DEBUG");
            } catch  (D2RQException ex) {
               System.out.println(ex.toString());
            }
            System.out.println("D2RQ model created using map " + D2RQMap);
            System.out.println("--------------------------------------------");

            // Test Selector
            // int test = 7;
            boolean outputResults = true;

            // Loop over all tests
            for (int test=1; test < 8; test++) {

            System.out.println("");
            System.out.println("");
            System.out.println("--------------------------------------------");
            System.out.println("Test Number " + String.valueOf(test));
            System.out.println("--------------------------------------------");

            ////////////////////////////////////
            // Test Number 1
            ////////////////////////////////////
            if (test == 1) {
            System.out.println("Iterate over the model using 'd2rqModel.listStatements();'");
            System.out.println("--------------------------------------------");

				 // list the statements in the Model
				StmtIterator iter = d2rqModel.listStatements();
				
				// print out the predicate, subject and object of each statement
				while (iter.hasNext()) {
					Statement stmt      = iter.nextStatement();  // get next statement
					Resource  subject   = stmt.getSubject();     // get the subject
					Property  predicate = stmt.getPredicate();   // get the predicate
					RDFNode   object    = stmt.getObject();      // get the object
				
					System.out.print(subject.toString());
					System.out.print(" " + predicate.toString() + " ");
					if (object instanceof Resource) {
					   System.out.print(object.toString());
					} else {
						// object is a literal
						System.out.print(" \"" + object.toString() + "\"");
					}
				
					System.out.println(" .");
				 }
            }

            ////////////////////////////////////
            // Test Number 2
            ////////////////////////////////////
            if (test == 2) {
            System.out.println("Check if a paper has the property author using 'paperRessource.hasProperty(author);'");
            System.out.println("--------------------------------------------");

             String paperURI = "http://www.conference.org/conf02004/paper#Paper1";
             Resource paperRessource = d2rqModel.getResource(paperURI);
             Property author = d2rqModel.createProperty("http://annotation.semanticweb.org/iswc/iswc.daml#author");
             if (paperRessource.hasProperty(author)) System.out.println("The paper has an author.");

            }

            ////////////////////////////////////
            // Test Number 3
            ////////////////////////////////////
            if (test == 3) {
            String rdql = "SELECT ?x, ?y WHERE (<http://www.conference.org/conf02004/paper#Paper1>, ?x, ?y)";
            System.out.println("RDQL query: " + rdql);
            System.out.println("--------------------------------------------");
            Query query = new Query(rdql);
			query.setSource(d2rqModel);
			QueryExecution qe = new QueryEngine(query) ;
			QueryResults results = qe.exec() ;
			QueryResultsFormatter fmt = new QueryResultsFormatter(results) ;
			PrintWriter pw = new PrintWriter(System.out) ;
			fmt.printAll(pw, " | ") ;
			pw.flush() ;
			fmt.close() ;	
			results.close() ;

            }

            ////////////////////////////////////
            // Test Number 4
            ////////////////////////////////////
            if (test == 4) {
            String rdql = "SELECT ?x, ?y WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?z), (?z, <http://annotation.semanticweb.org/iswc/iswc.daml#eMail> , ?y)";
            System.out.println("RDQL query: " + rdql);
            System.out.println("--------------------------------------------");
            Query query = new Query(rdql);
			query.setSource(d2rqModel);
			QueryExecution qe = new QueryEngine(query) ;
			QueryResults results = qe.exec() ;
			QueryResultsFormatter fmt = new QueryResultsFormatter(results) ;
			PrintWriter pw = new PrintWriter(System.out) ;
			fmt.printAll(pw, " | ") ;
			pw.flush() ;
			fmt.close() ;	
			results.close() ;

            }

            ////////////////////////////////////
            // Test Number 5
            ////////////////////////////////////
            if (test == 5) {
            String rdql = "SELECT ?x, ?z, ?y WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#topic>, ?z), (?z, <http://annotation.semanticweb.org/iswc/iswc.daml#name>, ?y)";
            System.out.println("RDQL query: " + rdql);
            System.out.println("--------------------------------------------");
            Query query = new Query(rdql) ;
			query.setSource(d2rqModel);
			QueryExecution qe = new QueryEngine(query) ;
			QueryResults results = qe.exec() ;
			QueryResultsFormatter fmt = new QueryResultsFormatter(results) ;
			PrintWriter pw = new PrintWriter(System.out) ;
			fmt.printAll(pw, " | ") ;
			pw.flush() ;
			fmt.close() ;	
			results.close() ;
            }

            ////////////////////////////////////
            // Test Number 6
            ////////////////////////////////////
            if (test == 6) {
            String rdql = "SELECT ?x, ?y WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?y), (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#title>, 'Titel of the Paper: Three Implementations of SquishQL, a Simple RDF Query Language'@en)";
            System.out.println("RDQL query: " + rdql);
            System.out.println("--------------------------------------------");
            Query query = new Query(rdql) ;
			query.setSource(d2rqModel);
			QueryExecution qe = new QueryEngine(query) ;
			QueryResults results = qe.exec() ;
			QueryResultsFormatter fmt = new QueryResultsFormatter(results) ;
			PrintWriter pw = new PrintWriter(System.out) ;
			fmt.printAll(pw, " | ") ;
			pw.flush() ;
			fmt.close() ;	
			results.close() ;

            }

            ////////////////////////////////////
            // Test Number 7
            ////////////////////////////////////
            if (test == 7) {
            String rdql = "SELECT ?x, ?y, ?a WHERE (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?y), (?x, <http://annotation.semanticweb.org/iswc/iswc.daml#title>, 'Titel of the Paper: Three Implementations of SquishQL, a Simple RDF Query Language'@en), (?y, <http://annotation.semanticweb.org/iswc/iswc.daml#eMail> , ?a)";
            System.out.println("RDQL query: " + rdql);
            System.out.println("--------------------------------------------");
            Query query = new Query(rdql) ;
			query.setSource(d2rqModel);
			QueryExecution qe = new QueryEngine(query) ;
			QueryResults results = qe.exec() ;
			QueryResultsFormatter fmt = new QueryResultsFormatter(results) ;
			PrintWriter pw = new PrintWriter(System.out) ;
			fmt.printAll(pw, " | ") ;
			pw.flush() ;
			fmt.close() ;	
			results.close() ;

            }
            }
    }

}
