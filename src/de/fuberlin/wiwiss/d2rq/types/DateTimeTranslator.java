package de.fuberlin.wiwiss.d2rq.types;

import de.fuberlin.wiwiss.d2rq.map.Translator;

/**
 * Translates from MySQL DATETIME values to xsd:dateTime values.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: DateTimeTranslator.java,v 1.2 2006/09/03 00:08:12 cyganiak Exp $
 */
public class DateTimeTranslator implements Translator {

	public String toRDFValue(String dbValue) {
		return dbValue.replace(' ', 'T');
	}

	public String toDBValue(String rdfValue) {
		return rdfValue.replace('T', ' ');
	}
}
