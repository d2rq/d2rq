package de.fuberlin.wiwiss.d2rq.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.query.util.RelURI;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable.Translation;

/**
 * Parses the contents of a CSV file into a collection of
 * {@link Translation}s. The CVS file must contain exactly
 * two columns. DB values come from the first, RDF values
 * from the second.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TranslationTableParser.java,v 1.2 2006/09/16 13:22:29 cyganiak Exp $
 */
public class TranslationTableParser {
	private Log log = LogFactory.getLog(TranslationTableParser.class);
	private BufferedReader reader;
	private CSV csvLineParser = new CSV();
	private String url;

	public TranslationTableParser(Reader reader) {
		this.reader = new BufferedReader(reader);
	}
	
	public TranslationTableParser(String url) {
		try {
			this.url = RelURI.resolve(url);;
			this.reader = new BufferedReader(new FileReader(new File(new URI(this.url))));
		} catch (FileNotFoundException fnfex) {
			throw new D2RQException("File not found at URL: " + this.url);
		} catch (URISyntaxException usynex) {
			throw new D2RQException("Malformed URI: " + this.url);
		}
	}
	
	public Collection parseTranslations() {
		try {
			List result = new ArrayList();
			while (true) {
				String line = this.reader.readLine();
				if (line == null) {
					break;
				}
				String[] fields = this.csvLineParser.parse(line);
				if (fields.length != 2) {
					this.log.warn("Skipping line with " +
							fields.length + " instead of 2 columns in CSV file " + this.url);
					continue;
				}
				result.add(new Translation(fields[0], fields[1]));
			}
			return result;
		} catch (IOException iex) {
			throw new D2RQException(iex);
		}
	}
}
