/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/

package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.rdf.model.* ;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.datatypes.*;
import java.util.*;
import java.io.*;

/**  A D2RQ virtual read-only graph backed by a non-RDF database.
    * 
    * D2RQ is a declarative mapping language for describing mappings between ontologies and relational data models.
    * More information about D2RQ is found at: http://www.wiwiss.fu-berlin.de/suhl/bizer/d2rq/
    * 
    * <BR>History: 06-06-2004   : Initial version of this class.
    * @author Chris Bizer chris@bizer.de
    * @version V0.1
    *
    * @see de.fuberlin.wiwiss.d2rq.D2RQCapabilities
 */
public class GraphD2RQ extends GraphBase implements Graph {

    protected final ReificationStyle style;
    protected boolean closed = false;

    /** Collection of all PropertyBridges definded in the mapping file */
    protected HashSet propertyBridges;

    /** Collection of all ClassMaps definded in the mapping file */
    protected HashMap classMaps;

    /** Flag for turning the debug mode on */
    protected boolean debug = false;

    public GraphD2RQ(String mapfilename) throws D2RQException  {
        this.style = ReificationStyle.Minimal;
        this.readD2RQMap(mapfilename);
    }

    public GraphD2RQ(String mapfilename, String parameter) throws D2RQException  {
        this.style = ReificationStyle.Minimal;
        if (parameter.equals("DEBUG")) { debug = true; }
        this.readD2RQMap(mapfilename);
        if (debug) {
            System.out.println("-----------------------------------------------------------------------");
            System.out.println(" New D2RQ graph created using the map " + mapfilename);
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("");
        }
    }


    private void readD2RQMap(String mapfilename) throws D2RQException {

     propertyBridges = new HashSet();
     classMaps = new HashMap();

     //////////////////////////////////////
     // Read D2RQ Map from file to model
     /////////////////////////////////////
     Model d2rqMapModel = ModelFactory.createDefaultModel();
     InputStream in = null ;
     BufferedReader reader = null ;
			try {
				in = new FileInputStream(mapfilename) ;
               	reader = new BufferedReader(new InputStreamReader(in, "UTF-8")) ;
			} catch (FileNotFoundException noEx) {
                 throw new IllegalArgumentException("File: " + mapfilename + " not found.");
     		} catch (java.io.UnsupportedEncodingException ex) {
                 throw new IllegalArgumentException("File: " + mapfilename + " has strange encoding (Not UTF-8).");
            }
	String baseName;
	File f = new File(mapfilename) ;
	baseName = "file:///"+f.getAbsolutePath() ;
	baseName = baseName.replace('\\', '/') ;
	d2rqMapModel.read(reader, baseName, "N3") ;

    // Check map for logical consistency
    // Todo for version 0.2 :-)

    ////////////////////////////////////////////
    // Generate Databases, ClassMaps and PropertyBridges
    ////////////////////////////////////////////
    Graph d2rqMap = d2rqMapModel.getGraph();

    // Find all databases
    ExtendedIterator databases = d2rqMap.find(Node.ANY, RDF.Nodes.type, D2RQ.Database);
    if (!databases.hasNext()) {
        throw new D2RQException("No database connection defined in the mapping file.");
    }
	while (databases.hasNext()) {
		 Triple database = (Triple) databases.next();
		 ExtendedIterator itOdbc = d2rqMap.find(database.getSubject(), D2RQ.odbcDSN, Node.ANY);
         String odbcDSN = null;
         if (itOdbc.hasNext()) {
             odbcDSN = ((Triple) itOdbc.next()).getObject().toString(); }
		 ExtendedIterator itJdbc = d2rqMap.find(database.getSubject(), D2RQ.jdbcDSN, Node.ANY);
         String jdbcDSN = null;
         String jdbcDriver = null;
         if (itJdbc.hasNext()) {
             jdbcDSN = ((Triple) itJdbc.next()).getObject().toString();
             ExtendedIterator itJdbcDr = d2rqMap.find(database.getSubject(), D2RQ.jdbcDriver, Node.ANY);
             if (itJdbcDr.hasNext()) {
                jdbcDriver = ((Triple) itJdbcDr.next()).getObject().toString();
             } else {
                throw new D2RQException("You used d2rq:jdbcDSN without specifing d2rq:jdbcDriver.");
             }
         }
		 ExtendedIterator itUser = d2rqMap.find(database.getSubject(), D2RQ.username, Node.ANY);
         String username = null;
         if (itUser.hasNext()) {
             username = ((Triple) itUser.next()).getObject().toString(); }
         ExtendedIterator itPass = d2rqMap.find(database.getSubject(), D2RQ.password, Node.ANY);
         String password = null;
         if (itPass.hasNext()) {
             password = ((Triple) itPass.next()).getObject().toString(); }
         HashMap columnTypes = new HashMap();
         ExtendedIterator itColText = d2rqMap.find(database.getSubject(), D2RQ.textColumn, Node.ANY);
         while (itColText.hasNext()) {
             Triple trip = (Triple) itColText.next();
             columnTypes.put(trip.getObject().toString(), D2RQ.textColumn);
         }
         ExtendedIterator itColNum = d2rqMap.find(database.getSubject(), D2RQ.numericColumn, Node.ANY);
         while (itColNum.hasNext()) {
             Triple trip = (Triple) itColNum.next();
             columnTypes.put(trip.getObject().toString(), D2RQ.numericColumn);
         }
         ExtendedIterator itColDate = d2rqMap.find(database.getSubject(), D2RQ.dateColumn, Node.ANY);
         while (itColDate.hasNext()) {
             Triple trip = (Triple) itColDate.next();
             columnTypes.put(trip.getObject().toString(), D2RQ.dateColumn);
         }
         Database db = new Database(odbcDSN, jdbcDSN, jdbcDriver, username, password, columnTypes);

         // Create all ClassMaps for this database
         ExtendedIterator itClassMaps = d2rqMap.find(Node.ANY, D2RQ.dataStorage, database.getSubject());
         while (itClassMaps.hasNext()) {
		     Node classMap = ((Triple) itClassMaps.next()).getSubject();
             // Get classMap properties
             ExtendedIterator it = d2rqMap.find(Node.ANY, D2RQ.classMap, classMap);
             Node rdfsClass = null;
             if (it.hasNext()) {
                rdfsClass = ((Triple) it.next()).getSubject(); }
             // Get URI pattern
             it = d2rqMap.find(classMap, D2RQ.uriPattern, Node.ANY);
             String uriPattern = null;
             if (it.hasNext()) {
                uriPattern = ((Triple) it.next()).getObject().toString(); }
             // Get URI column
             it = d2rqMap.find(classMap, D2RQ.uriColumn, Node.ANY);
             String uriColumn = null;
             if (it.hasNext()) {
                uriColumn = ((Triple) it.next()).getObject().toString(); }
             // Get bNode id columns
             it = d2rqMap.find(classMap, D2RQ.bNodeIdColumns, Node.ANY);
             String bNodeIdColumnsInput = null;
             ArrayList bNodeIdColumns = null;
             if (it.hasNext()) {
                bNodeIdColumnsInput = ((Triple) it.next()).getObject().toString();
                StringTokenizer tokenizer = new StringTokenizer(bNodeIdColumnsInput, ",");
                bNodeIdColumns = new ArrayList(3);
                while (tokenizer.hasMoreTokens()) {
                   bNodeIdColumns.add(tokenizer.nextToken());
                }
             }
             // Create ClassMap
             ClassMap classMapObj = new ClassMap(classMap, rdfsClass, uriPattern, uriColumn, bNodeIdColumns, db);
             classMaps.put(classMap, classMapObj);
             // Create RdfTypePropertyBridge
             if (rdfsClass != null) {
				 RDFTypePropertyBridge typeBridge = new RDFTypePropertyBridge(Node.createAnon(), rdfsClass, classMapObj, this);
				 propertyBridges.add(typeBridge);
				 classMapObj.addPropertyBridge(typeBridge);
             }
             // Create all other PropertyBridges for this classMap
			 ExtendedIterator itBridges = d2rqMap.find(Node.ANY, D2RQ.belongsToClassMap, classMap);
			 while (itBridges.hasNext()) {
				 Node bridge = ((Triple) itBridges.next()).getSubject();
                 it = d2rqMap.find(Node.ANY, D2RQ.propertyBridge, bridge);
				 Node property = null;
				 if (it.hasNext()) {
					property = ((Triple) it.next()).getSubject(); }
                 else {
                    throw new D2RQException("There is no d2rq:propertyBridge defined that links any RDF property to the PropertyBridge " + bridge.toString() + "."); }
				 it = d2rqMap.find(bridge, D2RQ.column, Node.ANY);
				 String column = null;
				 if (it.hasNext()) {
					column = ((Triple) it.next()).getObject().toString(); }
				 it = d2rqMap.find(bridge, D2RQ.pattern, Node.ANY);
				 String pattern = null;
				 if (it.hasNext()) {
					pattern = ((Triple) it.next()).getObject().toString(); }
                 it = d2rqMap.find(bridge, D2RQ.refersToClassMap, Node.ANY);
				 ArrayList joins = new ArrayList(5);
                 ExtendedIterator itjoin = d2rqMap.find(bridge, D2RQ.join, Node.ANY);
                 while (itjoin.hasNext()) {
						Node join =((Triple) itjoin.next()).getObject();
						joins.add(join.toString()); }
				 Node refersTo = null;
				 if (it.hasNext()) {
					refersTo = ((Triple) it.next()).getObject();
                    // A bridge which refers to another classMap has to have joins
					if (joins.isEmpty()) {
						 throw new D2RQException("PropertyBridge " + bridge.toString() + " uses d2rq:refersTo but defines no d2rq:join clauses.");
                    }
				 }
                 // check bridge type
                 it = d2rqMap.find(bridge, RDF.Nodes.type, Node.ANY);
				 Node type = null;
				 if (it.hasNext()) {
					type = ((Triple) it.next()).getObject();
                    if (!(type.equals(D2RQ.DatatypePropertyBridge) || type.equals(D2RQ.ObjectPropertyBridge))) {
                        throw new D2RQException("The PropertyBridge " + bridge.toString() + " has the wrong rdf:type."); }
                 } else {
                    throw new D2RQException("There is no RDF type defined for PropertyBridge " + bridge.toString() + "."); }
                 PropertyBridge bridgeObj = null;
                 if (type.equals(D2RQ.DatatypePropertyBridge)) {
                    // DataTypePropertyBridge
					 it = d2rqMap.find(bridge, D2RQ.datatype, Node.ANY);
					 RDFDatatype rdfDatatype = null;
					 if (it.hasNext()) {
                        rdfDatatype = TypeMapper.getInstance().getSafeTypeByName(((Triple) it.next()).getObject().toString());
					 }
					 it = d2rqMap.find(bridge, D2RQ.lang, Node.ANY);
					 String lang = null;
					 if (it.hasNext()) {
					 	lang = ((Triple) it.next()).getObject().toString();
					 }
					 bridgeObj = new DatatypePropertyBridge(bridge, property, column, pattern, rdfDatatype, lang, classMapObj, refersTo, joins, this);
                 } else {
                     // ObjectPropertyBridge
					 bridgeObj = new ObjectPropertyBridge(bridge, property, column, pattern, classMapObj, refersTo, joins, this);
                 }
			     propertyBridges.add(bridgeObj);
                 classMapObj.addPropertyBridge(bridgeObj);
			 } // Bridges
         } // ClassMaps
		} // Databases
    }
	/**
     * Todo: Implement D2RQ query handler
		@see com.hp.hpl.jena.graph.Graph#queryHandler
	*/
	public QueryHandler queryHandler() 
        { return new SimpleQueryHandler(this); }

/**
     * @see com.hp.hpl.jena.graph.Graph#close()
     */
    public void close() 
        { closed = true; }

    protected Capabilities capabilities = null;
    
    public Capabilities getCapabilities()
        { 
        if (capabilities == null) capabilities = new D2RQCapabilities();
        return capabilities;
        }

    /** Returns an iterator over Triple. */
    public ExtendedIterator find( TripleMatch m ) {
        checkOpen();
        Triple tm = m.asTriple();
        Node s = tm.getSubject();
        Node p = tm.getPredicate();
        Node o = tm.getObject();

        if (debug) {
            System.out.println("--------------------------------------------");
            System.out.println("        Find(SPO) Query Pattern");
            System.out.println("--------------------------------------------");
			System.out.println("Subject: " + s.toString());
			System.out.println("Predicate: " + p.toString());
			System.out.println("Object: " + o.toString());
            System.out.println("");
        }

        D2RQResultIterator resultiterator = null;

        /////////////////////////////////////////////////////////////////////
        // Switch to the different algorithms depending on the find pattern.
        /////////////////////////////////////////////////////////////////////

        // Pattern X p X
        // Pattern s p X
        // Pattern X p o
        // Pattern s p o
        if (p.isConcrete()) {
            resultiterator = findPatternXPX(s,p,o);

        // Pattern s X X
        } else if (s.isConcrete()) {
            resultiterator = findPatternSXX(s,p,o);

        // Pattern X X o
        } else if (o.isConcrete()) {
            resultiterator = findPatternXPX(s,p,o);

       // Pattern X X X
        } else {
            resultiterator = findPatternSXX(s,p,o);
        }
        return resultiterator;
    }

  /**
   * D2RQ Algorithm for the following find patterns:
   * 
   *  X p X
   *  s p X
   *  X p o
   *  s p o
   *  X X o
   */
    private D2RQResultIterator findPatternXPX(Node s,Node p, Node o) {

        D2RQResultIterator resultIt = new D2RQResultIterator();

        // Iterate over all propertyBridges
        Iterator it = propertyBridges.iterator();
        while (it.hasNext()) {
            PropertyBridge bridge = (PropertyBridge) it.next();

            // Check if current bridge could fit the query
            if (bridge.nodeCouldFitPredicate(p) &&
                bridge.nodeCouldFitSubject(s) &&
                bridge.nodeCouldFitObject(o)) {

                if (debug) {
                    System.out.println("--------------------------------------------");
                	System.out.println("Using property bridge: " + bridge.getId().toString());
                }

                SQLStatementMaker sqlMaker = new SQLStatementMaker();

                NodeMaker subjectMaker = bridge.getSubjectMaker(s, sqlMaker);
                NodeMaker objectMaker = bridge.getObjectMaker(o, sqlMaker);
  			    NodeMaker predicateMaker = bridge.getPredicateMaker();

				TripleResultSet trs = new TripleResultSet(sqlMaker.getSQLStatement(), sqlMaker.getColumnNameNumberMap(), bridge.getClassMap().getDatabase(), debug);
                trs.addTripleMaker(subjectMaker, predicateMaker, objectMaker);

				resultIt.addTripleResultSet(trs);
            }
        }
        return resultIt;
    }

  /**
   * D2RQ Algorithm for the find patterns:
   * 
   *  s X X
   *  s X o
   *  X X X
   */
    private D2RQResultIterator findPatternSXX(Node s,Node p, Node o) {

        D2RQResultIterator resultIt = new D2RQResultIterator();

        // Loop over all classMaps
        Iterator classMapsIt = classMaps.values().iterator();
        while (classMapsIt.hasNext()) {
            ClassMap classMap = (ClassMap) classMapsIt.next();

            // Check if s could be an instance of this classMap
            if (classMap.nodeCouldBeInstanceId(s)) {

               if (debug) {
                    System.out.println("--------------------------------------------");
                	System.out.println("Using class map: " + classMap.getId().toString());
                }

				SQLStatementMaker sqlMaker = new SQLStatementMaker();
	
				// Loop over all propertyBridges for this classMap
				Iterator propertyBridgesIt = classMap.getPropertyBridgesIterator();
				ArrayList tripleMakers = new ArrayList();
				while (propertyBridgesIt.hasNext()) {
					PropertyBridge bridge = (PropertyBridge) propertyBridgesIt.next();
	
					NodeMaker subjectMaker = bridge.getSubjectMaker(s, sqlMaker);
					NodeMaker predicateMaker = bridge.getPredicateMaker();
                    if (o.equals(Node.ANY)) {
					       NodeMaker objectMaker = bridge.getObjectMaker(Node.ANY, sqlMaker);
					       TripleMaker tripMaker = new TripleMaker(subjectMaker, predicateMaker, objectMaker);
					       tripleMakers.add(tripMaker);
                    } else {
                        // Object value set
                        if (bridge.nodeCouldFitObject(o)) {
                           NodeMaker objectMaker = bridge.getObjectMaker(o, sqlMaker);
					       TripleMaker tripMaker = new TripleMaker(subjectMaker, predicateMaker, objectMaker);
					       tripleMakers.add(tripMaker);
                        }
                    }
				}
	
				// Create TripleResultSet
				TripleResultSet trs = new TripleResultSet(sqlMaker.getSQLStatement(), sqlMaker.getColumnNameNumberMap(), classMap.getDatabase(), debug);
				Iterator tripleMakersIt = tripleMakers.iterator();
				while (tripleMakersIt.hasNext()) {
					trs.addTripleMaker((TripleMaker) tripleMakersIt.next());
				}
	
				// Add TripleResultSet to D2RQResultIterator
                if (trs.hasTripleMakers()) {
					resultIt.addTripleResultSet(trs);
                }
            }
        }
        return  resultIt;

    }

}
