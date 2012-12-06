package org.d2rq;

import static org.junit.Assert.fail;

import org.d2rq.pp.PrettyPrinter;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.PrefixMapping;

public class ModelAssert {

	public static void assertIsomorphic(Model expected, Model actual) {
		if (!actual.isIsomorphicWith(expected)) {
			Model missingStatements = expected.difference(actual);
			Model unexpectedStatements = actual.difference(expected);
			if (missingStatements.isEmpty() && unexpectedStatements.isEmpty()) {
				fail("Models not isomorphic due to duplicate triples; expected: " + 
						asNTriples(expected, expected) +
						" actual: " + asNTriples(actual, expected));
			} else if (missingStatements.isEmpty()) {
				fail("Unexpected statement(s): " + 
						asNTriples(unexpectedStatements, expected));
			} else if (unexpectedStatements.isEmpty()) {
				fail("Missing statement(s): " + 
						asNTriples(missingStatements, expected));
			} else {
				fail("Missing statement(s): " + 
						asNTriples(missingStatements, expected) + 
						" Unexpected statement(s): " + 
						asNTriples(unexpectedStatements, expected));
			}
		}
	}
	
	private static String asNTriples(Model model, PrefixMapping prefixes) {
		return PrettyPrinter.toString(model, prefixes);
	}
}
