package org.d2rq.lang;

import java.util.Collection;

import org.d2rq.ProcessorTestBase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.hp.hpl.jena.rdf.model.Model;

@RunWith(Parameterized.class)
public class ProcessorTest extends ProcessorTestBase {

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> getTestList() {
		return getTestListFromManifest("test/d2rq-lang/processor-test-manifest.ttl");
	}

	public ProcessorTest(String id, String mappingFile, String schemaFile, 
			Model expectedTriples) {
		super(id, mappingFile, schemaFile, expectedTriples);
	}
}
