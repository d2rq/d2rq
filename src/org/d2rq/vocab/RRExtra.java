package org.d2rq.vocab;

import com.hp.hpl.jena.rdf.model.Resource;


/**
 * An extra term that should be in the R2RML vocabulary but isn't.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class RRExtra {

	public static Resource Mapping = 
		RR.SubjectMap.getModel().createResource(RR.getURI() + "Mapping");
	public static Resource TermMap = 
		RR.SubjectMap.getModel().createResource(RR.getURI() + "TermMap");
	public static Resource ConstantValuedTermMap = 
		RR.SubjectMap.getModel().createResource(RR.getURI() + "ConstantValuedTermMap");
	public static Resource ColumnValuedTermMap = 
		RR.SubjectMap.getModel().createResource(RR.getURI() + "ColumnValuedTermMap");
	public static Resource TemplateValuedTermMap = 
		RR.SubjectMap.getModel().createResource(RR.getURI() + "TemplateValuedTermMap");
}
