package org.d2rq.mapgen;

import java.io.UnsupportedEncodingException;

public class IRIEncoder {

	/**
	 * %-encodes every character that is not in the
	 * iunreserved production of RFC 3987.
	 * 
	 * Behaviour for Unicode surrogates and Unicode
	 * non-characters is undefined. 
	 */
	public static String encode(String s) {
		StringBuffer sbuffer = new StringBuffer(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (isIUnreservedChar(c)) {
				sbuffer.append(c);
			} else {
				try {
					for (byte b: s.substring(i, i + 1).getBytes("utf-8")) {
						sbuffer.append('%');
						sbuffer.append(hexDigits[(b >> 4) & 0x0F]);
						sbuffer.append(hexDigits[b & 0x0F]);
					}
				} catch (UnsupportedEncodingException ex) {
					throw new RuntimeException("Can't happen");
				}
			}
		}
		return sbuffer.toString();
	}
	
	/**
	 * Checks if a string is a safe separator for IRI templates. This is
	 * the case if it contains any single character that is legal in an IRI, 
	 * but percent-encoded in the IRI-safe version of a data value.
	 * This includes in particular the eleven sub-delim characters
	 * defined in [RFC3987]: !$&'()*+,;=
	 * 
	 * @see <a href="http://www.w3.org/TR/r2rml/#dfn-safe-separator">R2RML: Safe separator</a>
	 */
	public static boolean isSafeSeparator(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (reserved.indexOf(s.charAt(i)) >= 0) return true;
		}
		return false;
	}
	
	/**
	 * Returns false if c cannot occur in an IRI (in unescaped form)
	 */
	public static boolean isAllowedInIRI(char c) {
		if (isIUnreservedChar(c)) return true;
		if (c == '%') return true;
		if (reserved.indexOf(c) >= 0) return true;
		return false;
	}
	
	private static char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	private static String reserved = ":/?#[]@!$&'()*+,;=";

	private static boolean isIUnreservedChar(char c) {
		return c == '-' || c == '_' || c == '~' || c == '.'
			|| isDigit(c) || isLetter(c)
			|| c >= '\u00A0';
	}
	
	private static boolean isDigit(char c) {
		return (c >= '0' && c <= '9');
	}
	
	private static boolean isLetter(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'); 
	}
}
