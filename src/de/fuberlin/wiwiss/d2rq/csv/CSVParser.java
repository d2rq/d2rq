package de.fuberlin.wiwiss.d2rq.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.fuberlin.wiwiss.d2rq.D2RQException;

/**
 * Parses the contents of a CSV file into a Map. The CVS
 * file must contain exactly two columns. Keys come from the
 * first, values from the second.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: CSVParser.java,v 1.1 2006/09/02 22:49:17 cyganiak Exp $
 */
public class CSVParser {
	private Log log = LogFactory.getLog(CSVParser.class);
	private BufferedReader reader;
	private CSV csvLineParser = new CSV();
	private String url;

	public CSVParser(Reader reader) {
		this.reader = new BufferedReader(reader);
	}
	
	public CSVParser(String url) {
		try {
			this.reader = new BufferedReader(new FileReader(new File(new URI(url))));
			this.url = url;
		} catch (FileNotFoundException fnfex) {
			throw new D2RQException("File not found: " + url);
		} catch (URISyntaxException usynex) {
			throw new D2RQException("Malformed URI: " + url);
		}
	}
	
	public Map parse() {
		try {
			Map result = new HashMap();
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
				result.put(fields[0], fields[1]);
			}
			return result;
		} catch (IOException iex) {
			throw new D2RQException(iex);
		}
	}
}
