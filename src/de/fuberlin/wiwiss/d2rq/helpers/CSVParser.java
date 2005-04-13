/*
 * $Id: CSVParser.java,v 1.1 2005/04/13 16:55:28 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.helpers;

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


/**
 * Parses the contents of a CSV file into a Map. The CVS
 * file must contain exactly two columns. Keys come from the
 * first, values from the second.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class CSVParser {
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
			Logger.instance().error("File not found: " + url);
		} catch (URISyntaxException usynex) {
			Logger.instance().error("Malformed URI: " + url);
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
					Logger.instance().warning("Skipping line with " +
							fields.length + " columns in " + this.url);
					continue;
				}
				result.put(fields[0], fields[1]);
			}
			return result;
		} catch (IOException iex) {
			Logger.instance().error(iex.getMessage());
			return null;
		}
	}
}
