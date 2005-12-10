/*
 * $Id: RDQLDB2Test.java,v 1.1 2005/12/10 01:05:58 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.functional_tests;

import de.fuberlin.wiwiss.d2rq.RDQLTestFramework;


public class RDQLDB2Test extends RDQLTestFramework {
	static String DB2MappingFile="file:doc/manual/ISWC-2DB-d2rq.n3";
	static String paperXCitesPaperY=
		"(?c, <http://annotation.semanticweb.org/iswc/iswc.daml#citingPaper>, ?x), " +
		"(?c, <http://annotation.semanticweb.org/iswc/iswc.daml#citedPaper>, ?y)";
		
	{
		setD2RQMap(DB2MappingFile);
	}
	
	public RDQLDB2Test(String arg0) {
		super(arg0);
	}

	public void testDB2() {
		rdql("SELECT ?x, ?y WHERE " + paperXCitesPaperY);
		dump();
	}
	
	// TODO make it work
	public void testPersonACitesB() {
		rdql("SELECT ?a, ?b WHERE " +
				"(?x, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?a), " + 
				paperXCitesPaperY +
				", (?y, <http://annotation.semanticweb.org/iswc/iswc.daml#author>, ?b)");
		dump();
	}

}
