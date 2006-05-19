/*
 * $Id: RDQLDB2Test.java,v 1.4 2006/05/19 18:42:37 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq.functional_tests;

import de.fuberlin.wiwiss.d2rq.RDQLTestFramework;


public class RDQLDB2Test extends RDQLTestFramework {
	static String DB2MappingFile="file:doc/manual/ISWC-2DB-d2rq.n3";
	static String zHasCitingPaperX=
	    "(?z, <http://annotation.semanticweb.org/iswc/iswc.daml#citingPaper>, ?x)";
	static String paperXCitesPaperY=
	    zHasCitingPaperX +
		", " +
		"(?z, <http://annotation.semanticweb.org/iswc/iswc.daml#citedPaper>, ?y)";
	static String paperXCitesPaperY2=
		"(?x, <http://annotation.semanticweb.org/iswc/iswc.daml#cites>, ?y)";

	static String paperYCitesPaperX=
		"(?w, <http://annotation.semanticweb.org/iswc/iswc.daml#citingPaper>, ?y), " +
		"(?w, <http://annotation.semanticweb.org/iswc/iswc.daml#citedPaper>, ?x)";
	static String paperXauthorA=
	    "(?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?a)";
	static String paperYauthorB=
	    "(?y, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?b)";

	public RDQLDB2Test(String arg0) {
		super(arg0);
		D2RQMap=DB2MappingFile;
	}
	
	protected void setUp() throws Exception {
	    super.setUp();
	    // Logger.instance().setDebug(true);
	    // D2RQResultIterator.logger=new Logger();
	    // D2RQResultIterator.logger.setDebug(true);
	    //PQCResultIterator4.logger=new Logger();
	    //PQCResultIterator4.logger.setDebug(true);
	    //D2RQPatternStage4.logger=new Logger();
	    //D2RQPatternStage4.logger.setDebug(true);
	}

	/**
	 * Makes sure all string constants work.
	 * uses db1:author
	 */
	public void testDB1() {
		rdql("SELECT ?x, ?a WHERE " + paperXauthorA);
		dump();
	}
	/**
	 * Make sure all string constants work.
	 */
	public void testDB1y() {
		rdql("SELECT ?y, ?b WHERE " + paperYauthorB);
		dump();
	}
	/**
	 * uses db2:citingPaper and db2:citedPaper
	 */
	public void testDB2paperXCitesPaperY() {
		rdql("SELECT ?x, ?y WHERE " + paperXCitesPaperY);
		dump();
	}
	/**
	 * uses db2:cites
	 */
	public void testDB2paperXCitesPaperY2() {
		rdql("SELECT ?x, ?y WHERE " + paperXCitesPaperY2);
		dump();
	}
	/**
	 * Papers that bidirectionally cite each other
	 */
	public void testDB2CiteAgain() {
		rdql("SELECT ?x, ?y WHERE " + paperXCitesPaperY + ", " + paperYCitesPaperX);
		dump();
	}
	
	// TODO make it work
	public void testPersonACitesPersonB() {
		rdql("SELECT ?a, ?b WHERE " +
		        paperXauthorA +
				", " + 
				paperXCitesPaperY +
				", " +
				paperYauthorB
				);
		dump();
	}

	/**
	 * simpler version of testPersonACitesPaperB()
	 */
	public void testAuthorsWhoCite() {
		rdql("SELECT ?a, ?z WHERE " +
		        paperXauthorA +
				", " + 
				zHasCitingPaperX 
				);
		dump();
	}

}
