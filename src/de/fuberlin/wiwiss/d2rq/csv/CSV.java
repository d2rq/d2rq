package de.fuberlin.wiwiss.d2rq.csv;
import java.util.ArrayList;

/**
 * Parse comma-separated values (CSV), a common Windows file format.
 * Sample input: "LU",86.25,"11/4/1998","2:19PM",+4.0625
 * <p>
 * Inner logic adapted from a C++ original that was
 * Copyright (C) 1999 Lucent Technologies
 * Excerpted from 'The Practice of Programming'
 * by Brian W. Kernighan and Rob Pike.
 * <p>
 * Included by permission of the http://tpop.awl.com/ web site, 
 * which says:
 * "You may use this code for any purpose, as long as you leave 
 * the copyright notice and book citation attached." I have done so.
 * 
 * @author Brian W. Kernighan and Rob Pike (C++ original)
 * @author Ian F. Darwin (translation into Java and removal of I/O)
 * @author Ben Ballard (rewrote advQuoted to handle '""' and for readability)
 * @author Don Brown (wanted to return a String array instead of Iterator)
 * @author Richard Cyganiak (richard@cyganiak.de) (adapted to local coding style)
 */
class CSV {      

	public static final char DEFAULT_SEP = ',';

	/** Construct a CSV parser, with the default separator (`,'). */
	public CSV() {
		this(DEFAULT_SEP);
	}

	/** Construct a CSV parser with a given separator. Must be
	 * exactly the string that is the separator, not a list of
	 * separator characters!
	 */
	public CSV(char sep) {
		this.fieldSep = sep;
	}

	/** The fields in the current String */
	private ArrayList<String> list = new ArrayList<String>();

	/** the separator char for this parser */
	private char fieldSep;

	/** parse: break the input String into fields
	 * @return String array containing each field 
	 * from the original as a String, in order.
	 */
	public String[] parse(String line) {
		StringBuffer sb = new StringBuffer();
		this.list.clear();                   // discard previous, if any
		int i = 0;

		if (line.length() == 0) {
			this.list.add(line);
			return (String[])this.list.toArray(new String[]{});
		}

		do {
			sb.setLength(0);
			if (i < line.length() && line.charAt(i) == '"')
				i = advQuoted(line, sb, ++i);   // skip quote
			else
				i = advPlain(line, sb, i);
			this.list.add(sb.toString());
			i++;
		} while (i < line.length());

		return (String[])this.list.toArray(new String[]{});
	}

	/** advQuoted: quoted field; return index of next separator */
	private int advQuoted(String s, StringBuffer sb, int i)
	{
		int j;
		int len= s.length();
		for (j=i; j<len; j++) {
			if (s.charAt(j) == '"' && j+1 < len) {
				if (s.charAt(j+1) == '"') {
					j++; // skip escape char
				} else if (s.charAt(j+1) == this.fieldSep) { //next delimeter
					j++; // skip end quotes
					break;
				}
			} else if (s.charAt(j) == '"' && j+1 == len) { // end quotes at end of line
				break; //done
			}
			sb.append(s.charAt(j)); // regular character.
		}
		return j;
	}

	/** advPlain: unquoted field; return index of next separator */
	private  int advPlain(String s, StringBuffer sb, int i) {
		int j;

		j = s.indexOf(this.fieldSep, i); // look for separator
		//Debug.println("csv", "i = " + i + ", j = " + j);
		if (j == -1) {                  // none found
			sb.append(s.substring(i));
			return s.length();
		}
		sb.append(s.substring(i, j));
		return j;
	}
}
