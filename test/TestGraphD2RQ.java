/*
  (c) Copyright 2004, Chris Bizer, Freie Universitaet Berlin
*/
import de.fuberlin.wiwiss.d2rq.*;
import java.util.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.datatypes.*;
import com.hp.hpl.jena.rdf.model.AnonId;

/**
 * Test Class for GraphD2RQ
 *
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
public class TestGraphD2RQ {
    private final static String D2RQMap = "C:/D2RQ/maps/ISWC-d2r.n3";
    static GraphD2RQ d2rqGraph;

    public static void main(String[] args) {

            System.out.println("--------------------------------------------");
            System.out.println("D2RQ Graph Test Started");
            System.out.println("--------------------------------------------");
            try{
               d2rqGraph = new GraphD2RQ(D2RQMap);
               // d2rqGraph = new GraphD2RQ(D2RQMap, "DEBUG");
            } catch  (D2RQException ex) {
               System.out.println(ex.toString());
            }
            System.out.println("D2RQ graph created using map " + D2RQMap);
            System.out.println("--------------------------------------------");

            // Test Selector
            //int test = 19;
            boolean outputResults = true;

            // Loop over all tests
            for (int test=1; test < 20; test++) {

            System.out.println("");
            System.out.println("");
            System.out.println("--------------------------------------------");
            System.out.println("Test Number " + String.valueOf(test));
            System.out.println("--------------------------------------------");

            Node subject = null;
            Node predicate = null;
            Node object = null;
            /////////////////////////////////////////////////////
            // Test: ANY, rdf:type, ANY
            /////////////////////////////////////////////////////
            if (test == 1) {
            	subject = Node.ANY;
            	predicate = RDF.Nodes.type;
            	object = Node.ANY;
            }

            /////////////////////////////////////////////////////
            // Test: ANY, rdf:type, iswc:Topic
            /////////////////////////////////////////////////////
            if (test == 2) {
            	subject = Node.ANY;
            	predicate = RDF.Nodes.type;
            	object = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#Topic");
            }

            /////////////////////////////////////////////////////
            // Test: ANY, iswc:name, ANY
            /////////////////////////////////////////////////////
            if (test == 3) {
            	subject = Node.ANY;
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#name");
            	object = Node.ANY;
            }

            /////////////////////////////////////////////////////
            // Test: ANY, iswc:author, ANY
            /////////////////////////////////////////////////////
            if (test == 4) {
            	subject = Node.ANY;
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#author_of");
            	object = Node.ANY;
            }

            /////////////////////////////////////////////////////
            // Test: ANY, iswc:name, "E-Business":dt#string
            /////////////////////////////////////////////////////
            if (test == 5) {
            	subject = Node.ANY;
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#name");
            	RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#string");
                object = Node.createLiteral("E-Business", null, dt);
            }

            /////////////////////////////////////////////////////
            // Test: ANY, iswc:name, "E-Business"  => result = null
            /////////////////////////////////////////////////////
            if (test == 6) {
            	subject = Node.ANY;
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#name");
                object = Node.createLiteral("E-Business", null, null);
            }

            /////////////////////////////////////////////////////
            // Test: ANY, iswc:title, "Trusting Information Sources One Citizen at a Time":en
            /////////////////////////////////////////////////////
            if (test == 7) {
            	subject = Node.ANY;
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#title");
                object = Node.createLiteral("Titel of the Paper: Trusting Information Sources One Citizen at a Time", "en", null);
            }

            /////////////////////////////////////////////////////
            // Test: Reverse Pattern
            /////////////////////////////////////////////////////
            if (test == 50) {
            	String pattern = "http://www.example.org/dbserver01/db01#Paper@@Papers.PaperID@@-@@Persons.PersonID@@-@@Conferences.ConfID@@.rdf";
                String value = "http://www.example.org/dbserver01/db01#Paper1111-2222222-333.rdf";
                HashMap result = D2RQUtil.ReverseValueWithPattern(value, pattern);
                if (result.isEmpty()) { System.out.println("Empty results set!"); }
                else {
				 	Iterator it = result.keySet().iterator();
                    while (it.hasNext()) {
                       String key = (String) it.next();
                       String resultvalue = (String) result.get(key);
                       System.out.println("Key:" + key + " Value: " + resultvalue);
                    }
                }
                outputResults = false;
            }

            /////////////////////////////////////////////////////
            // Test: ANY, iswc:year, 2003:gYear
            /////////////////////////////////////////////////////
            if (test == 8) {
            	subject = Node.ANY;
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#year");
                RDFDatatype dtYear = TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#gYear");
                object = Node.createLiteral("2003", null, dtYear);
            }

            /////////////////////////////////////////////////////
            // Test: http://www.conference.org/conf02004/paper#Paper2, iswc:year, ANY
            /////////////////////////////////////////////////////
            if (test == 9) {
            	subject = Node.createURI("http://www.conference.org/conf02004/paper#Paper2");
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#year");
                object = Node.ANY;
            }

            /////////////////////////////////////////////////////
            // Test: http://www.conference.org/conf02004/paper#Paper2, iswc:year, 2003:gYear
            /////////////////////////////////////////////////////
            if (test == 10) {
            	subject = Node.createURI("http://www.conference.org/conf02004/paper#Paper2");
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#year");
                RDFDatatype dtYear = TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#gYear");
                object = Node.createLiteral("2002", null, dtYear);
            }

            /////////////////////////////////////////////////////
            // Test: ANY, iswc:author, http://www.cs.vu.nl/~borys/papers/abstracts/ISWC2002.html#Bomelayenko
            /////////////////////////////////////////////////////
            if (test == 11) {
            	subject = subject = Node.ANY;
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#author");
                object = Node.createURI("http://www.cs.vu.nl/~borys#Bomelayenko");
            }

            /////////////////////////////////////////////////////
            // Test: ANY, iswc:email, mailto:andy.seaborne@hpl.hp.com
            /////////////////////////////////////////////////////
            if (test == 12) {
            	subject = subject = Node.ANY;
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#eMail");
                object = Node.createURI("mailto:andy.seaborne@hpl.hp.com");
            }

            /////////////////////////////////////////////////////
            // Test: bNode(http://www.example.org/dbserver01/db01#Topic@@Topics.TopicID@@3), iswc:name, ANY
            /////////////////////////////////////////////////////
            if (test == 13) {
            	subject = Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@Topics.TopicID@@3"));
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#name");
            	object = Node.ANY;
            }

            /////////////////////////////////////////////////////
            // Test: ANY, iswc:topic, bNode(http://www.example.org/dbserver01/db01#Topic@@Topics.TopicID@@3)
            /////////////////////////////////////////////////////
            if (test == 13) {
            	subject = Node.ANY;
            	predicate = Node.createURI("http://annotation.semanticweb.org/iswc/iswc.daml#topic");
            	object = Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@Topics.TopicID@@3"));
            }

            /////////////////////////////////////////////////////
            // Test: ANY, ANY, ANY (Database Dump)
            /////////////////////////////////////////////////////
            if (test == 14) {
            	subject = Node.ANY;
            	predicate = Node.ANY;
            	object = Node.ANY;
            }

            /////////////////////////////////////////////////////
            // Test: "http://www.conference.org/conf02004/paper#Paper2, ANY, ANY (Database Dump)
            /////////////////////////////////////////////////////
            if (test == 15) {
                //subject = Node.createURI("http://www.conference.org/conf02004/paper#Paper2");
            	subject = Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@Topics.TopicID@@3"));
            	predicate = Node.ANY;
            	object = Node.ANY;
            }

            /////////////////////////////////////////////////////
            // Test: "http://www.conference.org/conf02004/paper#Paper2, ANY, ANY (Database Dump)
            /////////////////////////////////////////////////////
            if (test == 16) {
                subject = Node.createURI("http://www.conference.org/conf02004/paper#Paper2");
            	//subject = Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@Topics.TopicID@@3"));
            	predicate = Node.ANY;
                RDFDatatype dtYear = TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#gYear");
            	object = Node.createLiteral("2002", null, dtYear);
            }

            /////////////////////////////////////////////////////
            // Test: ANY, ANY, bNode(http://www.example.org/dbserver01/db01#Topic@@Topics.TopicID@@3)
            /////////////////////////////////////////////////////
            if (test == 17) {
            	subject = Node.ANY;
            	predicate = Node.ANY;
            	object = Node.createAnon(new AnonId("http://www.example.org/dbserver01/db01#Topic@@Topics.TopicID@@3"));
            }

            /////////////////////////////////////////////////////
            // Test: ANY, ANY, 2002:dtYear)
            /////////////////////////////////////////////////////
            if (test == 18) {
            	subject = Node.ANY;
            	predicate = Node.ANY;
            	RDFDatatype dtYear = TypeMapper.getInstance().getSafeTypeByName("http://www.w3.org/2001/XMLSchema#gYear");
            	object = Node.createLiteral("2002", null, dtYear);
            }

            /////////////////////////////////////////////////////
            // Test: ANY, ANY, bNode(http://www.example.org/dbserver01/db01#Topic@@Topics.TopicID@@3)
            /////////////////////////////////////////////////////
            if (test == 19) {
            	subject = Node.ANY;
            	predicate = Node.ANY;
                //object = Node.createLiteral("Titel of the Paper: Trusting Information Sources One Citizen at a Time", "en", null);
                //object = Node.createURI("mailto:andy.seaborne@hpl.hp.com");
                object = Node.createURI("http://www.conference.org/conf02004/paper#Paper1");


            }

           if (outputResults) {
				Triple pattern = new Triple(subject, predicate, object);
				ExtendedIterator resultiterator = d2rqGraph.find(pattern);
                int count = 1;
				while (resultiterator.hasNext()) {
					System.out.println("Result Triple " + String.valueOf(count) + ": " +((Triple) resultiterator.next()).toString());
                    count++;
				}
           }

            } // loop over all tests
    }
}
