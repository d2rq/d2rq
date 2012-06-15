package de.fuberlin.wiwiss.d2rq.mapgen;

import junit.framework.TestCase;

public class IRIEncoderTest extends TestCase {
	
	public void testDontEncodeAlphanumeric() {
		assertEquals("azAZ09", IRIEncoder.encode("azAZ09"));
	}
	
	public void testDontEncodeSafePunctuation() {
		assertEquals("-_.~", IRIEncoder.encode("-_.~"));
	}
	
	public void testDontEncodeUnicodeChars() {
		// This is 'LATIN SMALL LETTER A WITH DIAERESIS' (U+00E4)
		assertEquals("\u00E4", IRIEncoder.encode("\u00E4"));
		// First char to be not encoded
		assertEquals("\u00A0", IRIEncoder.encode("\u00A0"));
	    assertEquals("\uD7FF", IRIEncoder.encode("\uD7FF"));
	    assertEquals("\uFFEF", IRIEncoder.encode("\uFFEF"));
	}

	public void testEncodeGenDelims() {
		assertEquals("%3A%2F%3F%23%5B%5D%40", IRIEncoder.encode(":/?#[]@"));
	}
	
	public void testEncodeSubDelims() {
		assertEquals("%21%24%26%27%28%29%2A%2B%2C%3B%3D", IRIEncoder.encode("!$&'()*+,;="));
	}
	
	public void testEncodePercentSign() {
		assertEquals("%25", IRIEncoder.encode("%"));		
	}
	
	public void testEncodeOtherASCIIChars() {
		assertEquals("%20%22%3C%3E%5C%5E%60%7B%7C%7D", IRIEncoder.encode(" \"<>\\^`{|}"));
	}
	
	public void testEncodeASCIIControlChars() {
		assertEquals("%00%01%02%03%04%05%06%07%08%09%0A%0B%0C%0D%0E%0F",
				IRIEncoder.encode("\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\n\u000B\u000C\r\u000E\u000F"));
		assertEquals("%10%11%12%13%14%15%16%17%18%19%1A%1B%1C%1D%1E%1F",
				IRIEncoder.encode("\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F"));
		assertEquals("%7F", IRIEncoder.encode("\u007F"));
	}
	
	public void testEncodeUnicodeControlChars() {
		assertEquals("%C2%80", IRIEncoder.encode("\u0080"));
		assertEquals("%C2%9F", IRIEncoder.encode("\u009F"));
	}
}
