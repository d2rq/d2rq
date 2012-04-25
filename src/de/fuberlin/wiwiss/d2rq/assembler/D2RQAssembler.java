package de.fuberlin.wiwiss.d2rq.assembler;

import com.hp.hpl.jena.assembler.Assembler;
import com.hp.hpl.jena.assembler.Mode;
import com.hp.hpl.jena.assembler.assemblers.AssemblerBase;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

/**
 * A Jena assembler that builds ModelD2RQs.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class D2RQAssembler extends AssemblerBase {

	public Object open(Assembler ignore, Resource description, Mode ignore2) {
		if (!description.hasProperty(D2RQ.mappingFile)) {
			throw new D2RQException("Error in assembler specification " + description + ": missing property d2rq:mappingFile");
		}
		if (!description.getProperty(D2RQ.mappingFile).getObject().isURIResource()) {
			throw new D2RQException("Error in assembler specification " + description + ": value of d2rq:mappingFile must be a URI");
		}
		String mappingFileURI = ((Resource) description.getProperty(D2RQ.mappingFile).getObject()).getURI();
		String resourceBaseURI = null;
		Statement stmt = description.getProperty(D2RQ.resourceBaseURI);
		if (stmt != null) {
			if (!stmt.getObject().isURIResource()) {
				throw new D2RQException("Error in assembler specification " + description + ": value of d2rq:resourceBaseURI must be a URI");
			}
			resourceBaseURI = ((Resource) stmt.getObject()).getURI();
		}
		return new ModelD2RQ(mappingFileURI, null, resourceBaseURI);
	}
}
