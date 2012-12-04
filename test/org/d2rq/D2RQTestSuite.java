package org.d2rq;

import java.util.Collection;

import junit.framework.JUnit4TestAdapter;

import org.d2rq.D2RQException;
import org.d2rq.Log4jHelper;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.lang.D2RQReader;
import org.d2rq.lang.Mapping;
import org.d2rq.vocab.D2RQ;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.vocabulary.RDF;


/**
 * Test suite for D2RQ, including various helper methods.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	HSQLDatabaseTest.class,
//	de.fuberlin.wiwiss.d2rq.algebra.AllTests.class,
//	de.fuberlin.wiwiss.d2rq.dbschema.AllTests.class,
//	de.fuberlin.wiwiss.d2rq.engine.AllTests.class,
//	de.fuberlin.wiwiss.d2rq.functional_tests.AllTests.class,
//	de.fuberlin.wiwiss.d2rq.optimizer.AllTests.class,
	org.d2rq.csv.AllTests.class,
	org.d2rq.db.AllTests.class,
	org.d2rq.db.expr.AllTests.class,
	org.d2rq.db.op.AllTests.class,
	org.d2rq.db.op.util.AllTests.class,
	org.d2rq.db.renamer.AllTests.class,
	org.d2rq.db.schema.AllTests.class,
	org.d2rq.db.types.AllTests.class,
	org.d2rq.db.vendor.AllTests.class,
	org.d2rq.download.AllTests.class,
	org.d2rq.find.AllTests.class,
	org.d2rq.functional_tests.AllTests.class,
	org.d2rq.jena.AllTests.class,
	org.d2rq.lang.AllTests.class,
	org.d2rq.mapgen.AllTests.class,
	org.d2rq.nodes.AllTests.class,
	org.d2rq.pp.AllTests.class,
	org.d2rq.r2rml.AllTests.class,
	org.d2rq.values.AllTests.class,
	org.d2rq.vocab.AllTests.class,
})

public class D2RQTestSuite {
	public static final String DIRECTORY = "test/org/d2rq/";
	public static final String DIRECTORY_URL = "file:" + DIRECTORY;
	public static final String ISWC_MAP = "file:doc/example/mapping-iswc.ttl"; 
	public static final String DummyDatabase = "http://d2rq.org/terms/test#DummyDatabase";
	public static final PrefixMapping STANDARD_PREFIXES = new PrefixMappingImpl() {{
		setNsPrefixes(PrefixMapping.Standard);
		setNsPrefix("d2rq", "http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#");
		setNsPrefix("jdbc", "http://d2rq.org/terms/jdbc/");
		setNsPrefix("test", "http://d2rq.org/terms/test#");
		setNsPrefix("ex", "http://example.org/");
		setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
	}};

	public static void main(String[] args) {
		// Be quiet and leave error reporting to JUnit
		Log4jHelper.turnLoggingOff();
		junit.textui.TestRunner.run(D2RQTestSuite.suite());
	}

	public static JUnit4TestAdapter suite() {
		return new JUnit4TestAdapter(D2RQTestSuite.class);
	}

	public static Model loadTurtle(String fileName) {
		Model m = ModelFactory.createDefaultModel();
		m.read(D2RQTestSuite.DIRECTORY_URL + fileName, "TURTLE");
		return m;
	}
	
	public static Collection<TripleRelation> loadPropertyBridges(String mappingFileName) {
		Model m = loadTurtle(mappingFileName);
		Resource dummyDB = m.getResource(DummyDatabase);
		dummyDB.addProperty(RDF.type, D2RQ.Database);
		Mapping mapping = new D2RQReader(m, "http://example.org/").getMapping();
		return mapping.compile().getTripleRelations();
	}

	/**
	 * Parses a D2RQ mapping from a file located relative to
	 * the {@link D2RQTestSuite} directory.
	 * 
	 * @param testFileName Filename, relative to {@link D2RQTestSuite}'s location
	 * @return A mapping
	 * @throws D2RQException On error during parse
	 */
	public static Mapping loadMapping(String testFileName) {
		return new D2RQReader(
				loadTurtle(testFileName), 
				"http://example.org/").getMapping();
	}
}