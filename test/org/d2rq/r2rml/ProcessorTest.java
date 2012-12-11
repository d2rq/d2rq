package org.d2rq.r2rml;

import java.util.Collection;

import org.d2rq.ProcessorTestBase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Runs R2RML tests, driven by a manifest file.
 * 
 * TODO: See manifest file for more R2RML test cases that should be written
 */
@RunWith(Parameterized.class)
public class ProcessorTest extends ProcessorTestBase {

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> getTestList() {
		return getTestListFromManifest("test/r2rml/processor-test-manifest.ttl");
	}

	public ProcessorTest(String id, String mappingFile, String schemaFile, 
			Model expectedTriples) {
		super(id, mappingFile, schemaFile, expectedTriples);
	}
}