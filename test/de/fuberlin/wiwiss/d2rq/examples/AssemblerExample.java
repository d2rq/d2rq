package de.fuberlin.wiwiss.d2rq.examples;

import com.hp.hpl.jena.assembler.Assembler;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

public class AssemblerExample {

	public static void main(String[] args) {
		// Load assembler specification from file
		Model assemblerSpec = FileManager.get().loadModel("doc/example/assembler.ttl");
		
		// Get the model resource
		Resource modelSpec = assemblerSpec.createResource(assemblerSpec.expandPrefix(":myModel"));
		
		// Assemble a model
		Model m = Assembler.general.openModel(modelSpec);
		
		// Write it to System.out
		m.write(System.out);

		m.close();
	}
}
