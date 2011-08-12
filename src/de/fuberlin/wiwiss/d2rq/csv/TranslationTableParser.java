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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.n3.IRIResolver;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.map.TranslationTable.Translation;

/**
 * Parses the contents of a CSV file into a collection of
 * <tt>Translation</tt>s. The CVS file must contain exactly
 * two columns. DB values come from the first, RDF values
 * from the second.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: TranslationTableParser.java,v 1.4 2007/10/23 15:17:53 cyganiak Exp $
 */
public class TranslationTableParser {
	private Logger log = LoggerFactory.getLogger(TranslationTableParser.class);
	private BufferedReader reader;
	private CSV csvLineParser = new CSV();
	private String url;

	public TranslationTableParser(Reader reader) {
		this.reader = new BufferedReader(reader);
	}
	
	public TranslationTableParser(String url) {
		try {
			this.url = new IRIResolver().resolve(url);;
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
					log.warn("Skipping line with {} instead of 2 columns in CSV file {}", Integer.valueOf(fields.length), this.url);
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
