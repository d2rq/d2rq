package org.d2rq.r2rml;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.d2rq.HSQLDatabase;
import org.d2rq.db.SQLConnection;
import org.d2rq.pp.PrettyPrinter;
import org.d2rq.r2rml.Mapping;
import org.d2rq.r2rml.MappingValidator;
import org.d2rq.r2rml.R2RMLReader;
import org.d2rq.validation.Message;
import org.d2rq.validation.Report;
import org.d2rq.validation.ValidatingRDFParser;
import org.d2rq.validation.Message.Level;
import org.d2rq.validation.Message.Problem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;


/**
 * TODO: This only checks the first :predicate or :object if multiple are specified in the manifest
 */
@RunWith(Parameterized.class)
public class ValidatorTest {
	private final static String MANIFEST_FILE = "test/r2rml/validator-test-manifest.ttl";
	private final static String SCHEMA = "test/r2rml/schemas/validator-test.sql";
	private final static String BASE = "http://example.com/";
		
	private final static String PREFIXES = 
		"PREFIX : <http://d2rq.org/terms/test.ttl#>\n" +
		"PREFIX e: <http://d2rq.org/terms/r2rml-errors.ttl#>\n";
	private final static String TEST_CASE_LIST = PREFIXES +
		"SELECT ?r2rml {\n" +
		"  ?r2rml a :R2RMLValidatorTestCase .\n" +
		"}";
	private final static String TEST_CASE_DETAILS = PREFIXES +
		"SELECT ?problem ?detailcode ?level ?details ?subject (MIN(?predicates) AS ?predicate) (MIN(?objects) AS ?object) {\n" +
		"  ?r2rml :expect ?expectation .\n" +
		"  ?expectation :problem ?problem .\n" +
		"  OPTIONAL { ?expectation :level ?level }\n" +
		"  OPTIONAL { ?expectation :detailcode ?detailcode }\n" +
		"  OPTIONAL { ?expectation :details ?details }\n" +
		"  OPTIONAL { ?expectation :subject ?subject }\n" +
		"  OPTIONAL { ?expectation :predicate ?predicates }\n" +
		"  OPTIONAL { ?expectation :object ?objects }\n" +
		"} GROUP BY ?expectation ?problem ?level ?detailcode ?details ?subject";

	@Parameters(name="{index}: {0}")
	public static Collection<Object[]> getTestList() {
		Model m = FileManager.get().loadModel(MANIFEST_FILE);
		IRI baseIRI = IRIFactory.iriImplementation().construct(m.getNsPrefixURI("base"));
		ResultSet rs = QueryExecutionFactory.create(TEST_CASE_LIST, m).execSelect();
		List<Object[]> result = new ArrayList<Object[]>();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			Resource file = qs.getResource("r2rml");
//			if (!file.getLocalName().equals("duplicate-subject-map.ttl")) continue;
			List<MessageExpectation> expectations = new ArrayList<MessageExpectation>();
			QueryExecution qe = QueryExecutionFactory.create(TEST_CASE_DETAILS, m);
			qe.setInitialBinding(qs);
			ResultSet rs2 = qe.execSelect();
			while (rs2.hasNext()) {
				QuerySolution solution = rs2.next();

				// Work around a SPARQL/ARQ weirdness where MAX() on an empty
				// solution sequence creates a new all-empty solution
				if (!solution.varNames().hasNext()) continue;
				
				MessageExpectation expectation = new MessageExpectation( 
						Problem.valueOf(solution.getResource("problem").getLocalName()));
				if (solution.contains("level")) {
					expectation.expectLevel(Level.valueOf(solution.getResource("level").getLocalName()));
				}
				if (solution.contains("details") && 
						solution.get("details").isLiteral() && 
						solution.getLiteral("details").getBoolean()) {
					expectation.expectDetails();
				}
				if (solution.contains("detailcode")) {
					expectation.expectDetailCode(solution.getResource("detailcode").getLocalName());
				}
				if (solution.contains("subject")) {
					expectation.expectSubject(solution.getResource("subject"));
				}
				if (solution.contains("predicate")) {
					expectation.expectPredicate(solution.getResource("predicate").as(Property.class));
				}
				if (solution.contains("object")) {
					expectation.expectObject(solution.get("object"));
				}
				expectations.add(expectation);
			}
			result.add(new Object[]{baseIRI.relativize(file.getURI()).toString(), file.getURI(), SCHEMA, expectations});
		}
		return result;
	}
	
	private final String id;
	private final String r2rmlFile;
	private final String sqlFile;
	private final List<MessageExpectation> expectations = new ArrayList<MessageExpectation>();
	private HSQLDatabase db;
	
	public ValidatorTest(String id, String r2rmlFile, String sqlFile, 
			List<MessageExpectation> expectations) {
		this.id = id;
		this.r2rmlFile = r2rmlFile;
		this.sqlFile = sqlFile;
		this.expectations.addAll(expectations);
	}
	
	@Before
	public void setUpDatabase() {
		if (sqlFile != null) {
			db = new HSQLDatabase("test");
			db.executeScript(sqlFile);
		}
	}
	
	@Test 
	public void run() {
		Report report = new Report();
		report.setIgnoreInfo(true);
		Model m = new ValidatingRDFParser(r2rmlFile, report).parse();
		if (m != null) {
			R2RMLReader reader = new R2RMLReader(m, BASE);
			reader.setReport(report);
			Mapping mapping = reader.getMapping();
			if (mapping != null) {
				MappingValidator validator = 
					db == null
						? new MappingValidator(mapping)
						: new MappingValidator(mapping, new SQLConnection(
								db.getJdbcURL(), HSQLDatabase.DRIVER_CLASS, 
								db.getUser(), db.getPassword()));
				validator.setReport(report);
				validator.run();
			}
		}
		for (Message actual: report.getMessages()) {
			boolean match = false;
			for (MessageExpectation expectation: expectations) {
				if (expectation.matches(actual)) {
					match = true;
					expectations.remove(expectation);
					break;
				}
			}
			if (!match) {
				fail(id + ": Unexpected message: " + actual);
				return;
			}
		}
		if (!expectations.isEmpty()) {
			fail(id + ": Expected message missing: " + expectations.get(0));
			return;
		}
	}
	
	@After
	public void dropDatabase() {
		if (db != null) db.close(true);
	}
	
	private static class MessageExpectation {
		private final Problem problem;
		private Level level = Level.Error;
		private boolean expectDetails = false;
		private String detailCode = null;
		private Resource subject = null;
		private Property predicate = null;
		private RDFNode object = null;
		private MessageExpectation(Problem problem) {
			this.problem = problem;
		}
		private void expectLevel(Level level) {
			this.level = level;
		}
		private void expectDetails() {
			expectDetails = true;
		}
		private void expectDetailCode(String detailCode) {
			this.detailCode = detailCode;
		}
		private void expectSubject(Resource subject) {
			this.subject = subject;
		}
		private void expectPredicate(Property property) {
			this.predicate = property;
		}
		private void expectObject(RDFNode object) {
			this.object = object;
		}
		private boolean matches(Message actual) {
			if (!actual.getProblem().equals(problem)) return false;
			if (!actual.getLevel().equals(level)) return false;
			if (expectDetails && actual.getDetails() == null) return false;
			if (detailCode != null && !detailCode.equals(actual.getDetailCode())) return false;
			if (subject != null && !subject.equals(actual.getSubject())) return false;
			if (predicate != null && !predicate.equals(actual.getPredicate())) return false;
			if (object != null && !object.equals(actual.getObject())) return false;
			return true;
		}
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(problem.name());
			if (detailCode != null) {
				s.append('/');
				s.append(detailCode);
			}
			s.append('(');
			s.append(problem.getLevel());
			s.append(')');
			if (subject != null) {
				s.append("; resource: ");
				s.append(PrettyPrinter.toString(subject));
			}
			if (predicate != null) {
				s.append("; predicate: ");
				s.append(PrettyPrinter.toString(predicate));
			}
			if (object != null) {
				s.append("; object: ");
				s.append(PrettyPrinter.toString(object));
			}
			if (expectDetails) {
				s.append(" (details)");
			}
			return s.toString();
		}
	}
	
	public String toString() {
		return getClass().getName() + "<" + id + ">";
	}
}
