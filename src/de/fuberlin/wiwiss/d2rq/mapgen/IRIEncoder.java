package de.fuberlin.wiwiss.d2rq.mapgen;

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
			int cCode = (int) c;

			if (c == '-' || c == '_' || c == '~' || c == '.'
					|| isDigit(cCode) || isLetter(cCode)
					|| cCode >= 0x00A0) {
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
	private static char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	private static boolean isDigit(int c) {
		return (c >= 48 && c <=57);
	}
	
	private static boolean isLetter(int c) {
		return (c >= 65 && c <= 90) || (c >= 97 && c <= 122); 
	}
}
