package de.fuberlin.wiwiss.d2rq.download;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.d2rq.helpers.HSQLDatabase;
import de.fuberlin.wiwiss.d2rq.helpers.MappingHelper;
import de.fuberlin.wiwiss.d2rq.map.DownloadMap;
import de.fuberlin.wiwiss.d2rq.map.Mapping;

public class DownloadContentQueryTest extends TestCase  {
	private HSQLDatabase db;
	private DownloadMap downloadCLOB;
	private DownloadMap downloadBLOB;
	private DownloadContentQuery q;
	
	public void setUp() {
		db = new HSQLDatabase("test");
		db.executeSQL("CREATE TABLE People (ID INT NOT NULL PRIMARY KEY, PIC_CLOB CLOB NULL, PIC_BLOB BLOB NULL)");
		db.executeSQL("INSERT INTO People VALUES (1, 'Hello World!', NULL)");
		db.executeSQL("INSERT INTO People VALUES (2, NULL, HEXTORAW('404040'))");
		Mapping m = MappingHelper.readFromTestFile("download/download-map.ttl");
		downloadCLOB = m.downloadMap(ResourceFactory.createResource("http://example.org/downloadCLOB"));
		downloadBLOB = m.downloadMap(ResourceFactory.createResource("http://example.org/downloadBLOB"));
	}

	public void tearDown() {
		db.close(true);
		if (q != null) q.close();
	}
	
	public void testFixture() {
		assertNotNull(downloadCLOB);
		assertNotNull(downloadBLOB);
	}
	
	public void testNullForNonDownloadURI() {
		q = new DownloadContentQuery(
				downloadCLOB, "http://not-in-the-mapping");
		assertFalse(q.hasContent());
		assertNull(q.getContentStream());
	}
	
	public void testNullForNonExistingRecord() {
		// There is no People.ID=42 in the table
		q = new DownloadContentQuery(
				downloadCLOB, "http://example.org/downloads/clob/42");
		assertFalse(q.hasContent());
		assertNull(q.getContentStream());
	}
	
	public void testReturnCLOBContentForExistingRecord() throws IOException {
		q = new DownloadContentQuery(
				downloadCLOB, "http://example.org/downloads/clob/1");
		assertTrue(q.hasContent());
		assertEquals("Hello World!", inputStreamToString(q.getContentStream()));
	}

	public void testNULLContent() {
		q = new DownloadContentQuery(
				downloadCLOB, "http://example.org/downloads/clob/2");
		assertFalse(q.hasContent());
		assertNull(q.getContentStream());
	}
	
	public void testReturnBLOBContentForExistingRecord() throws IOException {
		q = new DownloadContentQuery(downloadBLOB, "http://example.org/downloads/blob/2");
		assertTrue(q.hasContent());
		assertEquals("@@@", inputStreamToString(q.getContentStream()));
	}
	
	private String inputStreamToString(InputStream is) throws IOException {
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(is, "UTF-8");
		int read;
		do {
			read = in.read(buffer, 0, buffer.length);
			if (read>0) {
				out.append(buffer, 0, read);
			}
		} while (read>=0);
		return out.toString();		
	}
}
