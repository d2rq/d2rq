package de.fuberlin.wiwiss.d2rq.download;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(DownloadContentQueryTest.class);
		//$JUnit-END$
		return suite;
	}

}
