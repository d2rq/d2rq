package de.fuberlin.wiwiss.d2rq.utils;

import java.util.regex.Pattern;

/**
 * Taken from SQLStatement maker.  A collection of generic string 
 * manipulation functions.
 * 
 * @author Matthew Gheen - mgheen@bbn.com
 */

public class StringUtils {
    
    private final static Pattern escapePattern = Pattern.compile("([\\\\'])");
	private final static String escapeReplacement = "\\\\$1";
    
	/**
	 * Escape special characters in database literals to avoid
	 * SQL injection
	 */
	public static String escape(String s) {
		return escapePattern.matcher(s).replaceAll(escapeReplacement);
	}
}
