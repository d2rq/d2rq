package de.fuberlin.wiwiss.d2rq.types;

import de.fuberlin.wiwiss.d2rq.map.Translator;

/**
 * TODO Describe this type
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DateTimeTranslator.java,v 1.1 2006/07/12 11:08:09 cyganiak Exp $
 */
public class DateTimeTranslator implements Translator {

	public String toRDFValue(String dbValue) {
		return dbValue.replace(' ', 'T');
	}

	public String toDBValue(String rdfValue) {
		return rdfValue.replace('T', ' ');
	}
}
